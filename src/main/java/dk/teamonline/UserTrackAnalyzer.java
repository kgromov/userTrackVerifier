package dk.teamonline;

import com.thoughtworks.qdox.JavaProjectBuilder;
import dk.teamonline.annotation.ExternalWebService;
import dk.teamonline.annotation.InternalWebService;
import dk.teamonline.domain.ModuleSummary;
import dk.teamonline.domain.WebController;
import dk.teamonline.enums.ModuleSourceImpl;
import dk.teamonline.resolvers.MethodParametersResolver;
import dk.teamonline.resolvers.ThoughtworksMethodParametersResolver;
import dk.teamonline.utils.ExcelTransformer;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Main class to analyze UserTrack of certain module for potential and real errors.
 * Aggregates all required parameters in {@link ModuleSummary}:
 * {@link org.springframework.web.bind.annotation.RequestParam}
 * {@link org.springframework.web.bind.annotation.PathVariable}
 * {@link org.springframework.web.bind.annotation.ModelAttribute}
 * {@link RequestBody}
 * so they could be used later on for verification with e.g. Postman
 * <p>
 * Usage:
 * 1) add dependency for module with UserTracking - userTrackVerifier.domain.gradle
 * 2) add new item to {@link ModuleSourceImpl} enum
 */
public class UserTrackAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserTrackAnalyzer.class);

    public static void main(String[] args) {
        for (ModuleSource moduleSource : ModuleSourceImpl.values()) {
            String moduleName = moduleSource.getModuleName();

            List<String> packagesToScan = moduleSource.getPackages();
            JavaProjectBuilder builder = new JavaProjectBuilder();
            moduleSource.getSourcePaths().forEach(path -> builder.addSourceTree(path.toFile()));

            ModuleSummary moduleSummary = new ModuleSummary(moduleName);
            ExcelTransformer excelTransformer = new ExcelTransformer();

            packagesToScan.forEach(packageToScan -> {
                Reflections scanner = new Reflections(packageToScan,
                    new TypeAnnotationsScanner(),
                    new SubTypesScanner(false),
                    new MethodAnnotationsScanner(),
                    new MethodParameterScanner(),
                    new MethodParameterNamesScanner()
                );

                try {
                    String decoration = StringUtils.repeat('*', 20 + packageToScan.length());
                    LOGGER.info("\n{}", StringUtils.repeat(decoration, 3));
                    LOGGER.info("{} Scan {} {}", decoration, packageToScan, decoration);
                    LOGGER.info(StringUtils.repeat(decoration, 3));

                    MethodParametersResolver namesResolver = new ThoughtworksMethodParametersResolver(builder);

                    Set<Class<?>> controllerClasses = scanner.getTypesAnnotatedWith(Controller.class);
                    Set<Class<?>> restControllerClasses = scanner.getTypesAnnotatedWith(RestController.class);
                    Set<Class<?>> extWebServicesClasses = scanner.getTypesAnnotatedWith(ExternalWebService.class);
                    Set<Class<?>> intWebServicesClasses = scanner.getTypesAnnotatedWith(InternalWebService.class);

                    Stream.of(controllerClasses.stream(), restControllerClasses.stream(),
                        extWebServicesClasses.stream(), intWebServicesClasses.stream())
                        .flatMap(Function.identity())
                        .sorted(Comparator.comparing(Class::getSimpleName))
                        .forEach(clazz -> {
                            WebController controller = new WebController(clazz);
                            controller.printHead();
                            LOGGER.trace("class level mapping url = {}", controller.getRelativeUrl());
                            controller.setupEndpoints(namesResolver);

                            controller.getEndpoints().forEach(endpoint ->
                            {
                                controller.addMethodInfo(moduleSummary.getEndpointsSheet(), endpoint);
                                UserTrackVerifier verifier = new UserTrackVerifier(endpoint);
                                verifier.printEndpointInfo();
                                verifier.verify();
                                verifier.printEndpointSummary();

                                moduleSummary.mergeEntityClasses(endpoint);
                                moduleSummary.mergeParameters(endpoint);
                                moduleSummary.collectWarnings(verifier.getEndpointMethod(), verifier.getErrorCollector());
                                moduleSummary.collectErrors(verifier.getEndpointMethod(), verifier.getErrorCollector());
                            });
                        });
                } catch (Exception e) {
                    LOGGER.error("ERROR\tCan't scan specified package {}. " +
                        "Make sure module was added to dependencies and specified correctly in ModuleSourceImpl:\n{}", packageToScan, e);
                }
            });
            moduleSummary.printModuleSummary();
            // export results to excel
            moduleSummary.exportEntitiesDataToSheet();
            moduleSummary.exportParametersDataToSheet();
            excelTransformer.exportToFile(moduleSummary);
        }
    }
}
