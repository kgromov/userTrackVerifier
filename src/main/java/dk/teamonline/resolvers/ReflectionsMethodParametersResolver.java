package dk.teamonline.resolvers;

import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.List;

public class ReflectionsMethodParametersResolver implements MethodParametersResolver {
    private final Reflections scanner;

    public ReflectionsMethodParametersResolver(Reflections scanner) {
        this.scanner = scanner;
    }

    @Override
    public List<String> getParameterNames(Method method) {
        return scanner.getMethodParamNames(method);
    }
}
