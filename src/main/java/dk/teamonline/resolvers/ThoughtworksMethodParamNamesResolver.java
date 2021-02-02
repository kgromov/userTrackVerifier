package dk.teamonline.resolvers;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class ThoughtworksMethodParamNamesResolver implements MethodParamNamesResolver {
    private final JavaProjectBuilder builder;

    public ThoughtworksMethodParamNamesResolver(JavaProjectBuilder builder) {
        this.builder = builder;
    }

    @Override
    public List<String> getParameterNames(Method method) {
        return getParameterNames(method.getDeclaringClass(), method);
    }

    public List<String> getParameterNames(Class<?> clazz, Method method) {
        Optional<JavaMethod> javaMethod = builder.getClassByName(clazz.getName()).getMethods()
            .stream()
            .filter(m -> m.getName().equals(method.getName()))
            .filter(m -> isTheSameSignature(method, m))
            .findFirst();
        return javaMethod.map(m -> m.getParameters().stream().map(JavaParameter::getName).collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
    }

    private boolean isTheSameSignature(Method method, JavaMethod javaMethod) {
        List<String> originalParamTypes = Arrays.stream(method.getParameters())
            .map(Parameter::getType)
            .map(Class::getName)
            .collect(Collectors.toList());

        List<String> parsedParamTypes = javaMethod.getParameters().stream()
            .map(JavaParameter::getType)
            .map(JavaType::getFullyQualifiedName)
            .collect(Collectors.toList());
        return originalParamTypes.size() == parsedParamTypes.size()
            && CollectionUtils.isEqualCollection(originalParamTypes, parsedParamTypes);
    }
}
