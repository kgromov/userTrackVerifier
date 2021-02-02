package dk.teamonline.utils;


import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;

public class RequestMappingUtils {

    private RequestMappingUtils() {
        throw new UnsupportedOperationException("Utils class is not supposed to have instances");
    }

    public static String getClassRelativeUrl(RequestMapping requestMapping) {
        return requestMapping == null || requestMapping.value().length == 0 ? "" : requestMapping.value()[0];
    }

    public static String getMethodRelativeUrl(Method method) {
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        if (mapping != null) {
            return mapping.value().length == 0 ? "" : mapping.value()[0];
        } else if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            return getMapping.value().length == 0 ? "" : getMapping.value()[0];
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping getMapping = method.getAnnotation(PostMapping.class);
            return getMapping.value().length == 0 ? "" : getMapping.value()[0];
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping putMapping = method.getAnnotation(PutMapping.class);
            return putMapping.value().length == 0 ? "" : putMapping.value()[0];
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
            return deleteMapping.value().length == 0 ? "" : deleteMapping.value()[0];
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
            return patchMapping.value().length == 0 ? "" : patchMapping.value()[0];
        }
        return "";
    }

    public static Set<RequestMethod> getHttpMethods(Method method) {
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        if (requestMapping != null) {
            return Set.of(requestMapping.method());
        } else if (method.isAnnotationPresent(GetMapping.class)) {
            return Set.of(RequestMethod.GET);
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            return Set.of(RequestMethod.POST);
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            return Set.of(RequestMethod.PUT);
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            return Set.of(RequestMethod.DELETE);
        } else if (method.isAnnotationPresent(PatchMapping.class)) {
            return Set.of(RequestMethod.PATCH);
        }
        return emptySet();
    }

    public static Optional<? extends Class<? extends Annotation>> getRequestParameterAnnotationClass(Parameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
            .map(Annotation::annotationType)
            .filter(annotationType -> annotationType.equals(PathVariable.class)
                || annotationType.equals(RequestParam.class)
                || annotationType.equals(RequestBody.class)
                || annotationType.equals(ModelAttribute.class)
            )
            .findFirst();

    }

}
