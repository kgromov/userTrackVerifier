package dk.teamonline.resolvers;

import java.lang.reflect.Method;
import java.util.List;

public interface MethodParamNamesResolver {

    List<String> getParameterNames(Method method);
}
