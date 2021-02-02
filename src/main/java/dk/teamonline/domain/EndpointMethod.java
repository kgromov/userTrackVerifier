package dk.teamonline.domain;

import dk.eg.sensum.userTrack.domain.UserTracking;
import dk.teamonline.resolvers.MethodParamNamesResolver;
import dk.teamonline.utils.RequestMappingUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Entity;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dk.eg.sensum.userTrack.domain.UserTrackAction.IGNORE;

public class EndpointMethod {
    public static final Predicate<Parameter> IS_PARAMETER_ANNOTATED = p ->
        p.isAnnotationPresent(PathVariable.class)
            || p.isAnnotationPresent(RequestParam.class)
            || p.isAnnotationPresent(ModelAttribute.class)
            || p.isAnnotationPresent(RequestBody.class);

    private final String methodName;
    private final String returnType;
    private final String relativeUrl;
    private final Set<RequestMethod> httpMethods;
    private final UserTrackMethod userTrackMethod;
    private final Map<String, Parameter> parametersToRealName = new LinkedHashMap<>();


    public EndpointMethod(Method method, String parentUrl, MethodParamNamesResolver namesResolver) {
        this.methodName = method.getName();
        this.returnType = method.getReturnType().getSimpleName();
        String methodUrl = RequestMappingUtils.getMethodRelativeUrl(method);
        this.relativeUrl = (parentUrl + '/' + methodUrl).replaceAll("/{2,}", "/")
            // correlation to Postman
            .replace("{", "{{")
            .replace("}", "}}");
        this.httpMethods = RequestMappingUtils.getHttpMethods(method);
        UserTracking userTracking = method.getAnnotation(UserTracking.class);
        this.userTrackMethod = userTracking != null ? new UserTrackMethod(userTracking) : null;
        List<String> methodParameterNames = namesResolver.getParameterNames(method);
        Parameter[] methodParameters = method.getParameters();
        for (int i = 0; i < methodParameters.length; i++) {
            parametersToRealName.put(methodParameterNames.get(i), methodParameters[i]);
        }

    }

    public String getMethodName() {
        return methodName;
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

    public boolean isIgnoring() {
        return userTrackMethod.getAction() == IGNORE;
    }


    public String getPrettyPrintSignature() {
        StringBuilder signature = new StringBuilder(returnType);
        signature.append(' ');
        signature.append(methodName);
        signature.append('(');
        int leftPadding = returnType.length() + methodName.length() + 8;
        String params = parametersToRealName.entrySet().stream()
            .map(e -> {
                Parameter parameter = e.getValue();
                Annotation[] annotations = parameter.getAnnotations();
                String annotation = annotations == null || annotations.length == 0
                    ? "" : Arrays.stream(annotations)
                    .map(a -> a.annotationType().getSimpleName())
                    .collect(Collectors.joining(" @", "@", " "));
                return annotation + parameter.getType().getSimpleName() + ' ' + e.getKey();
            })
            .collect(Collectors.joining(",\n" + StringUtils.leftPad(" ", leftPadding)));
        signature.append(params);
        signature.append(')');
        return signature.toString();
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
                RequestMappingUtils.getRequestParameterAnnotationClass(parameter).ifPresent(annotationType -> {
                    result.computeIfAbsent(annotationType, value -> new HashSet<>())
                        .add(parameter.getType().getSimpleName() + ' ' + parameterName);
                });
            });
        return result;
    }

    @Override
    public String toString() {
        return String.join("\n", getPrettyPrintSignature(),
            "method = " + httpMethods.toString(),
            "URL = " + relativeUrl);
    }
}
