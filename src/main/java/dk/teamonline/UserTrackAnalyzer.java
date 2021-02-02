package dk.teamonline;

import com.thoughtworks.qdox.JavaProjectBuilder;
import dk.teamonline.annotation.ExternalWebService;
import dk.teamonline.domain.ModuleSummary;
import dk.teamonline.domain.WebController;
import dk.teamonline.resolvers.MethodParamNamesResolver;
import dk.teamonline.resolvers.ThoughtworksMethodParamNamesResolver;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main class to analyze UserTrack of certain module for potential and real errors.
 *
 * Usage:
 * 1) add dependency for module with UserTracking - userTrackVerifier.domain.gradle
 * 2) configure analyzer with System properties:
 *      module.name - required parameter - name of module to check: e.g. goalIndicator, residentAway etc
 *      module name is used also to setup logging path (logger is configured as local file)
 *      module.types - if omitted default ones be used - {ui, ws, extws}
 *      package.pattern - specifies package name pattern, default - dk.teamonline.{moduleName}.{moduleType}
 *      userTrack.log.path - path to lo file
 *
 * 1 configuration is supposed to verify 1 module and produce results into 1 log file.
 */
public class UserTrackAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserTrackAnalyzer.class);
    private static final String SOURCE_PART = "src/main/java";
    private static final String DEFAULT_PACKAGE_PATTERN = "dk.teamonline.%s.%s";
    private static final String DEFAULT_MODULE_TYPES = "ui,ws,extws";

    public static void main(String[] args) {
        String moduleName = System.getProperty("module.name");
        String[] moduleTypes = System.getProperty("module.types", DEFAULT_MODULE_TYPES).split(",\\s*");
        String packagePattern = System.getProperty("package.pattern", DEFAULT_PACKAGE_PATTERN);
        List<String> packagesToScan = Arrays.stream(moduleTypes)
            .map(type -> String.format(packagePattern, moduleName, type)).collect(Collectors.toList());
        ModuleSummary moduleSummary = new ModuleSummary(moduleName);
        for (String packageToScan : packagesToScan) {
            try {
                String decoration = StringUtils.repeat('*', 20 + packageToScan.length());
                LOGGER.info("\n{}", StringUtils.repeat(decoration, 3));
                LOGGER.info("{} Scan {} {}", decoration, packageToScan, decoration);
                LOGGER.info(StringUtils.repeat(decoration, 3));

                Reflections scanner = new Reflections(packageToScan,
                    new TypeAnnotationsScanner(),
                    new SubTypesScanner(),
                    new MethodAnnotationsScanner(),
                    new MethodParameterScanner(),
                    new MethodParameterNamesScanner()
                );
//            MethodParamNamesResolver namesResolver = new ReflectionsMethodParamNamesResolver(scanner);
                JavaProjectBuilder builder = new JavaProjectBuilder();
                String sourceDir = convertPackageToFolder(moduleName, packageToScan);
                builder.addSourceTree(new File(sourceDir));
                MethodParamNamesResolver namesResolver = new ThoughtworksMethodParamNamesResolver(builder);

                Set<Class<?>> controllerClasses = scanner.getTypesAnnotatedWith(Controller.class);
                Set<Class<?>> restControllerClasses = scanner.getTypesAnnotatedWith(RestController.class);
                Set<Class<?>> extWebServicesClasses = scanner.getTypesAnnotatedWith(ExternalWebService.class);

                Stream.of(controllerClasses.stream(), restControllerClasses.stream(), extWebServicesClasses.stream())
                    .flatMap(Function.identity())
                    .sorted(Comparator.comparing(Class::getSimpleName))
                    .forEach(clazz -> {
                        WebController controller = new WebController(clazz);
                        controller.printHead();
                        LOGGER.debug("class level mapping url = {}", controller.getRelativeUrl());

                        controller.setupEndpoints(namesResolver);
                        controller.getEndpoints().forEach(endpoint ->
                        {
                            UserTrackVerifier verifier = new UserTrackVerifier(endpoint);
                            verifier.printEndpointInfo();
                            verifier.verify();
                            verifier.printEndpointSummary();

                            moduleSummary.mergeEntityClasses(endpoint);
                            moduleSummary.mergeParameters(endpoint);
                        });
                    });
            } catch (Exception e) {
                LOGGER.error("FATAL\tCan't scan specified package {}. " +
                    "Make module was added to dependencies and specified correctly:\n{}", packagesToScan, e);
            }
        }
        moduleSummary.printModuleSummary();
    }

    // Very fragile, is used only because Reflections provides method param names with some local vars
    // and -parameters javac does not work
    private static String convertPackageToFolder(String module, String packageToScan) {
        String projectPath = Paths.get(".").normalize().toAbsolutePath().toString();
        String packageRelativePath = packageToScan.replace('.', '/');
        String moduleType = packageRelativePath.substring(packageRelativePath.lastIndexOf('/') + 1);
        return String.join("/", projectPath, module, moduleType, SOURCE_PART, packageRelativePath);
    }
}
