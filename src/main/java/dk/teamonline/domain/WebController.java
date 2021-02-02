package dk.teamonline.domain;

import dk.eg.sensum.userTrack.domain.UserTracking;
import dk.teamonline.utils.RequestMappingUtils;
import dk.teamonline.annotation.ExternalWebService;
import dk.teamonline.resolvers.MethodParamNamesResolver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

import static dk.teamonline.enums.UserTrackError.MISSED_USER_TRACKING;

public class WebController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebController.class);

    private static final Predicate<Method> IS_ENDPOINT_METHOD = m ->
        m.isAnnotationPresent(RequestMapping.class)
            || m.isAnnotationPresent(GetMapping.class)
            || m.isAnnotationPresent(PostMapping.class)
            || m.isAnnotationPresent(PutMapping.class)
            || m.isAnnotationPresent(DeleteMapping.class)
            || m.isAnnotationPresent(PatchMapping.class);

    public static final Predicate<Method> HAS_USERTRACKING = m -> m.isAnnotationPresent(UserTracking.class);

    private final Class<?> clazz;
    private final boolean isExternalWebService;
    private final String relativeUrl;
    private final List<EndpointMethod> endpoints = new ArrayList<>();

    public WebController(Class<?> clazz) {
        this.clazz = clazz;
        this.isExternalWebService = clazz.isAnnotationPresent(ExternalWebService.class);
        this.relativeUrl = resolveUrlFromModuleAndClass();
    }

    private String resolveUrlFromModuleAndClass() {
        String classLocation = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        String moduleName = classLocation.substring(classLocation.indexOf("atlas") + 6).split("/")[0];
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        String relativeUrl = RequestMappingUtils.getClassRelativeUrl(requestMapping);
        return (isExternalWebService ? "/wsapi" : moduleName) + '/' + relativeUrl;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getRelativeUrl() {
        return relativeUrl;
    }

    public boolean isExternalWebService() {
        return isExternalWebService;
    }

    public List<EndpointMethod> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    public void setupEndpoints(MethodParamNamesResolver namesResolver) {
        Arrays.stream(clazz.getDeclaredMethods())
            .filter(IS_ENDPOINT_METHOD)
            .peek(this::verifyForMissingUserTracking)
            .filter(HAS_USERTRACKING)
            .map(m -> new EndpointMethod(m, relativeUrl, namesResolver))
            .forEach(endpoints::add);
    }

    public void verifyForMissingUserTracking(Method method) {
        if (!HAS_USERTRACKING.test(method)) {
            LOGGER.error("{} - the following method is supposed to have @UserTracking:\n{}", MISSED_USER_TRACKING, method);
        }
    }

    public void printHead() {
        String decoration = StringUtils.repeat('#', 20);
        LOGGER.info("\n{} {} {}", decoration, clazz.getSimpleName(), decoration);
    }

    public void printFooter() {
        LOGGER.info(StringUtils.repeat('#', 50));
    }
}
