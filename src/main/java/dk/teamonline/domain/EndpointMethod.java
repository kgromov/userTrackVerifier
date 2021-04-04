package dk.teamonline.domain;

import dk.eg.sensum.userTrack.domain.UserTracking;
import dk.teamonline.resolvers.MethodParametersResolver;
import dk.teamonline.table.TableRow;
import dk.teamonline.utils.RequestMappingUtils;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Entity;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dk.eg.sensum.userTrack.domain.UserTrackAction.IGNORE;
import static dk.teamonline.resolvers.MethodParametersResolver.concatenateParams;
import static java.util.stream.Collectors.joining;

public class EndpointMethod implements TableRow {
    public static final Predicate<Parameter> IS_PARAMETER_ANNOTATED = p ->
        p.isAnnotationPresent(PathVariable.class)
            || p.isAnnotationPresent(RequestParam.class)
            || p.isAnnotationPresent(ModelAttribute.class)
            || p.isAnnotationPresent(RequestBody.class);

    private final Class<?> declaredClass;
    private final String methodName;
    private final String methodSignature;
    private final String relativeUrl;
    private final Set<RequestMethod> httpMethods;
    private final UserTrackMethod userTrackMethod;
    private final Map<String, Parameter> parametersToRealName;


    public EndpointMethod(WebController controller, Method method, MethodParametersResolver parametersResolver) {
        this.declaredClass = method.getDeclaringClass();
        this.methodName = method.getName();
        this.methodSignature = parametersResolver.getPrettyPrintSignature(method);
        this.parametersToRealName = parametersResolver.getParametersToRealName(method);
        String methodUrl = RequestMappingUtils.getMethodRelativeUrl(method);
        this.relativeUrl = (controller.getRelativeUrl() + '/' + methodUrl).replaceAll("/{2,}", "/")
            // correlation to Postman
            .replace("{", "{{")
            .replace("}", "}}");
        this.httpMethods = RequestMappingUtils.getHttpMethods(method);
        UserTracking userTracking = method.getAnnotation(UserTracking.class);
        this.userTrackMethod = userTracking != null ? new UserTrackMethod(userTracking) : null;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getRelativeUrl() {
        return relativeUrl;
    }

    public Set<RequestMethod> getHttpMethods() {
        return Collections.unmodifiableSet(httpMethods);
    }

    public UserTrackMethod getUserTrackMethod() {
        return userTrackMethod;
    }

    public Map<String, Parameter> getParametersToRealName() {
        return parametersToRealName;
    }

    public Class<?> getDeclaredClass() {
        return declaredClass;
    }

    public boolean isIgnoring() {
        return userTrackMethod.getAction() == IGNORE;
    }

    // helper methods for module summary data
    Set<Class<?>> getTrackedEntities() {
        return userTrackMethod.getUserTrackParameters().stream()
            .map(UserTrackValue::getType)
            .filter(p -> p.isAnnotationPresent(Entity.class))
            .collect(Collectors.toSet());
    }

    Map<Class<?>, Set<String>> getRequestedParameters() {
        Map<Class<?>, Set<String>> result = new HashMap<>();
        parametersToRealName.entrySet().stream()
            .filter(entry -> IS_PARAMETER_ANNOTATED.test(entry.getValue()))
            .forEach(entry -> {
                String parameterName = entry.getKey();
                Parameter parameter = entry.getValue();
                RequestMappingUtils.getRequestParameterAnnotationClass(parameter).ifPresent(annotationType ->
                    result.computeIfAbsent(annotationType, value -> new HashSet<>())
                        .add(parameter.getType().getSimpleName() + ' ' + parameterName));
            });
        return result;
    }


    @Override
    public String[] getCells() {
        String params = concatenateParams(parametersToRealName, "\n");
        return new String[]{
            this.methodName,
            this.relativeUrl,
            this.userTrackMethod.getAction().getTextValue(),
            this.httpMethods.stream().map(Enum::name).sorted().collect(joining(",")),
            this.userTrackMethod.getUserTrackParameters().stream().map(UserTrackValue::toString).collect(joining("\n")),
            params,
            ""
        };
    }

    @Override
    public String toString() {
        return String.join("\n", methodSignature,
            "method = " + httpMethods.toString(),
            "URL = " + relativeUrl);
    }
}
