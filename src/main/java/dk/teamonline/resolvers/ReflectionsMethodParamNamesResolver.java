package dk.teamonline.resolvers;

import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.List;

public class ReflectionsMethodParamNamesResolver implements MethodParamNamesResolver {
    private final Reflections scanner;

    public ReflectionsMethodParamNamesResolver(Reflections scanner) {
        this.scanner = scanner;
    }

    @Override
    public List<String> getParameterNames(Method method) {
        return scanner.getMethodParamNames(method);
    }
}
