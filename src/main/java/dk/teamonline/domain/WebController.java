package dk.teamonline.domain;

import dk.eg.sensum.userTrack.domain.UserTracking;
import dk.teamonline.annotation.ExternalWebService;
import dk.teamonline.annotation.InternalWebService;
import dk.teamonline.resolvers.MethodParametersResolver;
import dk.teamonline.table.CommandLineTable;
import dk.teamonline.table.ExcelSheet;
import dk.teamonline.utils.RequestMappingUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static dk.teamonline.enums.UserTrackError.MISSED_USER_TRACKING;

public class WebController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebController.class);
    private static final String[] METHOD_OVERVIEW_TABLE_HEADERS =
        {"Method_Name", "Relative_Path", "Action", "HTTP_method(s)", "UserTrackParams", "Method_params", "Inherited_From"};


    private static final Predicate<Method> IS_ENDPOINT_METHOD = m ->
        m.isAnnotationPresent(RequestMapping.class)
            || m.isAnnotationPresent(GetMapping.class)
            || m.isAnnotationPresent(PostMapping.class)
            || m.isAnnotationPresent(PutMapping.class)
            || m.isAnnotationPresent(DeleteMapping.class)
            || m.isAnnotationPresent(PatchMapping.class);

    public static final Predicate<Method> HAS_USERTRACKING = m -> m.isAnnotationPresent(UserTracking.class);

    private final Class<?> clazz;
    private final boolean isWebService;
    private final String relativeUrl;
    private final List<EndpointMethod> endpoints = new ArrayList<>();
    private final CommandLineTable table;

    public WebController(Class<?> clazz) {
        this.clazz = clazz;
        this.isWebService = clazz.isAnnotationPresent(ExternalWebService.class)
            || clazz.isAnnotationPresent(InternalWebService.class);
        this.relativeUrl = resolveUrlFromModuleAndClass();
        this.table = new CommandLineTable(clazz.getSimpleName());
        table.setHeaders(METHOD_OVERVIEW_TABLE_HEADERS);
    }

    private String resolveUrlFromModuleAndClass() {
        String classLocation = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        String moduleName = classLocation.substring(classLocation.indexOf("atlas") + 6).split("/")[0];
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        String relativeUrl = RequestMappingUtils.getClassRelativeUrl(requestMapping);
        return (isWebService ? "/wsapi" : moduleName) + '/' + relativeUrl;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getRelativeUrl() {
        return relativeUrl;
    }

    public boolean isWebService() {
        return isWebService;
    }

    public List<EndpointMethod> getEndpoints() {
        return Collections.unmodifiableList(endpoints);
    }

    public void setupEndpoints(MethodParametersResolver resolver) {
        Arrays.stream(clazz.getMethods())
            .filter(IS_ENDPOINT_METHOD)
            // reflection bug - creates volatile methods when controller overrides method
            .filter(m -> !Modifier.isVolatile(m.getModifiers()))
            .peek(method -> verifyForMissingUserTracking(method, resolver))
            .filter(HAS_USERTRACKING)
            .map(method -> new EndpointMethod(this, method, resolver))
            .forEach(endpoints::add);
    }

    public void verifyForMissingUserTracking(Method method, MethodParametersResolver resolver) {
        if (!HAS_USERTRACKING.test(method)) {
            LOGGER.error("{} - the following method is supposed to have @UserTracking:\n{}", MISSED_USER_TRACKING,
                resolver.getPrettyPrintSignature(method));
        }
    }

    public void printHead() {
        String decoration = StringUtils.repeat('#', 20);
        LOGGER.info("\n{} {} {}", decoration, clazz.getSimpleName(), decoration);
    }

    public void addMethodInfo(ExcelSheet excelSheet, EndpointMethod method) {
        excelSheet.addRow(this.clazz.getSimpleName());
        String[] cells = method.getCells();
        if (!clazz.equals(method.getDeclaredClass())) {
            cells[cells.length - 1] = method.getDeclaredClass().getName();
        }
        excelSheet.addRow(cells);
    }

    public void addMethodInfo(EndpointMethod method) {
        String[] cells = method.getCells();
        if (!clazz.equals(method.getDeclaredClass())) {
            cells[cells.length - 1] = method.getDeclaredClass().getName();
        }
        table.addRow(cells);
    }

    public void printMethodsSummary() {
        table.printTable();
    }
}
