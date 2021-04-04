package dk.teamonline.resolvers;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface MethodParametersResolver {
    Logger LOGGER = LoggerFactory.getLogger(MethodParametersResolver.class);

    List<String> getParameterNames(Method method);

    default Map<String, Parameter> getParametersToRealName(Method method) {
        Parameter[] methodParameters = method.getParameters();
        List<String> methodParameterNames = getParameterNames(method);
        Map<String, Parameter> parametersToRealName = new LinkedHashMap<>(methodParameters.length);
        if (methodParameterNames.size() != methodParameters.length) {
            LOGGER.error("ERROR\tCan't find method by signature:\n{}. " +
                "Make sure module was added to dependencies and specified correctly in ModuleSourceImpl", method);
            return parametersToRealName;
        }
        for (int i = 0; i < methodParameters.length; i++) {
            parametersToRealName.put(methodParameterNames.get(i), methodParameters[i]);
        }
        return parametersToRealName;
    }

    default String getPrettyPrintSignature(Method method) {
        StringBuilder signature = new StringBuilder();
        String returnType = method.getReturnType().getSimpleName();
        signature.append(returnType);
        signature.append(' ');
        String methodName = method.getName();
        signature.append(methodName);
        signature.append('(');
        int leftPadding = returnType.length() + methodName.length() + 8;
        Map<String, Parameter> parametersToRealName = getParametersToRealName(method);
        String params = concatenateParams(parametersToRealName, ",\n" + StringUtils.leftPad(" ", leftPadding));
        signature.append(params);
        signature.append(')');
        return signature.toString();
    }

    static String concatenateParams(Map<String, Parameter> parametersToRealName, String delimiter) {
        return parametersToRealName.entrySet().stream()
            .map(e -> {
                Parameter parameter = e.getValue();
                Annotation[] annotations = parameter.getAnnotations();
                String annotation = annotations == null || annotations.length == 0
                    ? "" : Arrays.stream(annotations)
                    .map(a -> a.annotationType().getSimpleName())
                    .collect(Collectors.joining(" @", "@", " "));
                return annotation + parameter.getType().getSimpleName() + ' ' + e.getKey();
            })
            .collect(Collectors.joining(delimiter));
    }
}
