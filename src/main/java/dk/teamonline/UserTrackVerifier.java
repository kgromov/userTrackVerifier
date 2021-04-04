package dk.teamonline;

import dk.eg.sensum.userTrack.domain.CitizenIdAware;
import dk.eg.sensum.userTrack.domain.UserTrackAction;
import dk.teamonline.domain.EndpointMethod;
import dk.teamonline.domain.ErrorCollector;
import dk.teamonline.domain.UserTrackMethod;
import dk.teamonline.domain.UserTrackValue;
import dk.teamonline.util.hibernateBase.Identifiable;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.persistence.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dk.eg.sensum.userTrack.domain.UserTrackAction.*;
import static dk.teamonline.domain.EndpointMethod.IS_PARAMETER_ANNOTATED;
import static dk.teamonline.enums.UserTrackError.*;
import static dk.teamonline.enums.UserTrackWarning.*;

/**
 * Class that perform all required validation over endpoint under @UserTracking
 * <p>
 * By real errors meant:
 * 1) invalid Spel expression in @UserTrackParameter expression;
 * 2) @UserTrackParameter type is not @Entity annotated
 * 3) duplicated parameter(s)
 * 4) missed @userTracking for method with @RequestMapping
 * <p>
 * By potential errors (warning):
 * 1) HTTP method and user track action mismatch;
 * 2) Redundancy - More than 2 @UserTrackParameters;
 * 3) Missed UserTrackParameter for input method params annotated with either:
 * {@link org.springframework.web.bind.annotation.RequestParam}
 * or {@link org.springframework.web.bind.annotation.PathVariable}
 * or {@link org.springframework.web.bind.annotation.RequestBody}
 * or {@link org.springframework.web.bind.annotation.ModelAttribute}
 */
public class UserTrackVerifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserTrackVerifier.class);

    private static final Map<UserTrackAction, RequestMethod> ACTION_TO_METHOD = Map.of(
        SHOW, RequestMethod.GET,
        EDIT, RequestMethod.PUT,
        DELETE, RequestMethod.DELETE,
        SAVE, RequestMethod.POST,
        SEARCH, RequestMethod.GET,
        INSERT, RequestMethod.POST,
        COPY, RequestMethod.POST,
        AUTOSAVE, RequestMethod.POST
    );

    private static final Predicate<Parameter> IS_PLURAL = p ->
    {
        Class<?> type = p.getType();
        return type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type);
    };

    private final ErrorCollector errorCollector;
    private final EndpointMethod endpointMethod;

    public UserTrackVerifier(EndpointMethod endpointMethod) {
        this.endpointMethod = endpointMethod;
        this.errorCollector = new ErrorCollector();
    }

    public void verify() {
        if (endpointMethod.isIgnoring()) {
            verifyIgnoringMethod();
            return;
        }
        // warnings
        verifyAction();
        verifyForRedundancy();
        // errors
        verifyParametersNecessity();
        verifyDuplicates();
        endpointMethod.getUserTrackMethod().getUserTrackParameters().forEach(userTrackParam -> {
            Optional<Map.Entry<String, Parameter>> parameterEntry = matchedParameterByUserTrackExpression(userTrackParam);
            parameterEntry
                .ifPresentOrElse(
                    parameter -> validateUserTrackParameter(userTrackParam, parameter.getValue(), parameter.getKey()),
                    () -> errorCollector.appendError(INCORRECT_EXPRESSION,
                        String.format("No parameter found for UserTrack expression = %s", userTrackParam.getExpression()))
                );
        });
    }

    private void verifyIgnoringMethod() {
        if (endpointMethod.getUserTrackMethod().hasUserTrackParameters()) {
            errorCollector.appendError(REDUNDANT_PARAMETERS, "UserTracking with 'IGNORE' action should not have @UserTrackParameters");
        }
    }

    private void verifyAction() {
        Set<RequestMethod> httpMethods = endpointMethod.getHttpMethods();
        UserTrackMethod userTrackMethod = endpointMethod.getUserTrackMethod();
        boolean isActionCorrect = httpMethods.isEmpty() || httpMethods.contains(ACTION_TO_METHOD.getOrDefault(userTrackMethod.getAction(), RequestMethod.GET));
        if (!isActionCorrect) {
            errorCollector.appendWarning(INCORRECT_ACTION,
                String.format("Probably UserTrack has incorrect action: %s, Http methods: %s", userTrackMethod.getAction(), httpMethods));
        }
    }

    private void verifyForRedundancy() {
        List<UserTrackValue> userTrackParameters = endpointMethod.getUserTrackMethod().getUserTrackParameters();
        if (userTrackParameters.size() > 2 /*|| hasNotOnlyCitizenIdAware()*/) {
            errorCollector.appendWarning(REDUNDANT_PARAMETER, "Seems not all of @UserTrackParameters are required");
        }
    }

    private boolean hasNotOnlyCitizenIdAware(List<UserTrackValue> userTrackParameters) {
        long amountOfCitizenAwareEntities = userTrackParameters.stream()
            .map(UserTrackValue::getType)
            .filter(CitizenIdAware.class::isAssignableFrom)
            .count();
        return amountOfCitizenAwareEntities > 0 && userTrackParameters.size() - amountOfCitizenAwareEntities > 0;
    }

    private void verifyParametersNecessity() {
        if (!endpointMethod.getUserTrackMethod().hasUserTrackParameters()) {
            List<String> paramsForTracking = endpointMethod.getParametersToRealName().entrySet().stream()
                .filter(e -> IS_PARAMETER_ANNOTATED.test(e.getValue()))
                .filter(e -> !IS_PLURAL.test(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            if (!paramsForTracking.isEmpty()) {
                errorCollector.appendWarning(MISSED_PARAMETERS,
                    String.format("At Least 1 @UserTrackParameter is required cause method has annotated parameters:%n%s", paramsForTracking));
            }
        }
    }

    private void verifyDuplicates() {
        UserTrackMethod userTrackMethod = endpointMethod.getUserTrackMethod();
        if (userTrackMethod.hasRedundantParameters()) {
            errorCollector.appendError(DUPLICATED_PARAMETER,
                String.format("The following @UserTrackParameters are duplicated:%n%s", userTrackMethod.getDuplicates()));
        }
    }

    public Optional<Map.Entry<String, Parameter>> matchedParameterByUserTrackExpression(UserTrackValue userTrackParam) {
        return endpointMethod.getParametersToRealName().entrySet().stream()
            .filter(p -> userTrackParam.getExpression().startsWith(p.getKey()))
            .findFirst();
    }

    /**
     * The following
     * 1) Model or ModelAndView => @ModelAttribute
     * 2) Command, [@RequestBody] + dto, searcher => by Spell (. is mandatory)
     * 3) String or primitives and boxing types => by param name
     */
    private void validateUserTrackParameter(UserTrackValue userTrackParameter, Parameter parameter, String parameterName) {
        if (!userTrackParameter.getType().isAnnotationPresent(Entity.class)
            && !Identifiable.class.isAssignableFrom(userTrackParameter.getType())) {
            errorCollector.appendError(INCORRECT_TYPE,
                String.format("Incorrect type for %s - should be @Entity or extends Identifiable", userTrackParameter));
        }

        // validate expression
        Class<?> parameterType = parameter.getType();
        String expression = userTrackParameter.getExpression();
        Class<?> fieldClazz = parameterType;
        if (parameterType.equals(String.class) || ClassUtils.isPrimitiveOrWrapper(parameterType)) {
            if (!parameterName.equals(expression)) {
                errorCollector.appendError(INCORRECT_EXPRESSION,
                    String.format("Incorrect expression: %s does not match method parameter name: %s",
                        userTrackParameter.getExpression(), parameterType.getSimpleName()));
            }
        } else {
            String[] fieldNames = expression.split("\\.");
            for (int i = 1; i < fieldNames.length; i++) {
                Field field = ReflectionUtils.findField(fieldClazz, fieldNames[i]);
                if (field == null) {
                    errorCollector.appendError(INCORRECT_EXPRESSION,
                        String.format("Incorrect expression %s: method parameter: %s has no field: %s",
                            userTrackParameter.getExpression(), parameterType.getSimpleName(), fieldNames[i]));
                    break;
                }
                fieldClazz = field.getType();
            }
        }
    }

    public void printEndpointInfo() {
        LOGGER.info("\n{}\n{}", endpointMethod.toString(), endpointMethod.getUserTrackMethod());
    }

    public void printEndpointSummary() {
        String decoration = StringUtils.repeat('=', 20);
        LOGGER.info("{} Method Summary {}", decoration, decoration);
        LOGGER.error("---ERRORS---\n{}", errorCollector.getErrorsAsString());
        LOGGER.error("\n---WARNINGS---\n{}", errorCollector.getWarningsAsString());
        LOGGER.info("{}", StringUtils.repeat(decoration, 3));
    }

    public ErrorCollector getErrorCollector() {
        return errorCollector;
    }

    public EndpointMethod getEndpointMethod() {
        return endpointMethod;
    }
}
