package dk.teamonline.domain;

import dk.teamonline.enums.UserTrackError;
import dk.teamonline.enums.UserTrackWarning;
import dk.teamonline.table.ExcelSheet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySortedSet;

public class ModuleSummary {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleSummary.class);

    private static final String[] ENDPOINTS_TABLE_HEADERS = {"Method_Name", "Relative_Path", "Action", "HTTP_method(s)", "UserTrackParams", "Method_params", "Inherited_From"};
    private static final String[] ENTITIES_TABLE_HEADERS = {"Entity_Class", "Table_Name"};
    private static final String[] PARAMETERS_TABLE_HEADERS = {"Parameter_Class", "PathVariable", "RequestParam", "RequestBody", "ModelAttribute"};
    private static final String[] WARNINGS_TABLE_HEADERS =
        Stream.concat(Stream.of("Class", "Method"), Arrays.stream(UserTrackWarning.values()).map(Enum::name)).toArray(String[]::new);
    private static final String[] ERRORS_TABLE_HEADERS =
        Stream.concat(Stream.of("Class", "Method"), Arrays.stream(UserTrackError.values()).map(Enum::name)).toArray(String[]::new);

    private final String moduleName;

    private final Map<Class<?>, String> entityClasses = new TreeMap<>(Comparator.comparing(Class::getName));
    private final Map<Class<?>, SortedSet<String>> moduleParameters = new HashMap<>();
    // not to duplicate inherited methods
    private final Set<String> uniqueMethodWarnings = new HashSet<>();
    private final Set<String> uniqueMethodErrors = new HashSet<>();

    private final ExcelSheet endpointsSheet = new ExcelSheet("Endpoints", ENDPOINTS_TABLE_HEADERS);
    private final ExcelSheet entitiesSheet = new ExcelSheet("Entities", ENTITIES_TABLE_HEADERS);
    private final ExcelSheet parametersSheet = new ExcelSheet("Parameters", PARAMETERS_TABLE_HEADERS);
    private final ExcelSheet warningsSheet = new ExcelSheet("Warnings", WARNINGS_TABLE_HEADERS);
    private final ExcelSheet errorsSheet = new ExcelSheet("Errors", ERRORS_TABLE_HEADERS);

    public ModuleSummary(String moduleName) {
        this.moduleName = moduleName;
    }

    public void mergeParameters(EndpointMethod endpointMethod) {
        Map<Class<?>, Set<String>> endpointParameters = endpointMethod.getRequestedParameters();
        endpointParameters.forEach((annotationClazz, params) ->
            moduleParameters.computeIfAbsent(annotationClazz, value -> new TreeSet<>()).addAll(params));
    }

    public void mergeEntityClasses(EndpointMethod endpointMethod) {
        Set<Class<?>> methodTrackedEntities = endpointMethod.getTrackedEntities();
        methodTrackedEntities.forEach(entityClass -> entityClasses.putIfAbsent(entityClass, getTableName(entityClass)));
    }

    public void printModuleSummary() {
        String decoration = StringUtils.repeat('~', 20);
        LOGGER.info("\n{} Module '{}' summary {}:", decoration, moduleName, decoration);
        String entityClassesToTableName = entityClasses.entrySet().stream()
            .map(e -> e.getKey().getName() + "(tableName = " + e.getValue() + ")")
            .collect(Collectors.joining("\n"));
        LOGGER.info("Tracked Entities:\n{}", entityClassesToTableName);
        moduleParameters.forEach((annotationClazz, params) ->
            LOGGER.info("\n{}:\n{}", annotationClazz.getSimpleName(), getAggregatedParams(params)));
    }

    private String getTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        return tableAnnotation == null ? entityClass.getSimpleName() : tableAnnotation.name();
    }

    private String getAggregatedParams(Set<String> params) {
        Map<String, Set<String>> aggregatedParams = new TreeMap<>();
        params.stream()
            .map(p -> p.split("\\s"))
            .forEach(pair -> aggregatedParams.computeIfAbsent(pair[0], names -> new HashSet<>()).add(pair[1]));
        return aggregatedParams.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), String.join(", ", e.getValue())))
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining("\n"));
    }

    public void collectWarnings(EndpointMethod endpoint, ErrorCollector errorCollector) {
        Map<UserTrackWarning, StringBuilder> endpointWarnings = errorCollector.getWarnings();
        String methodKey = endpoint.getDeclaredClass().getName() + endpoint.getMethodSignature();
        if (endpointWarnings.isEmpty() || !uniqueMethodWarnings.add(methodKey)) {
            return;
        }
        String[] rowValues = Stream.concat(Stream.of(endpoint.getDeclaredClass().getName(), endpoint.getMethodSignature()),
            Arrays.stream(UserTrackWarning.values())
                .map(w -> Optional.ofNullable(endpointWarnings.get(w)).map(StringBuilder::toString).orElse(""))
        ).toArray(String[]::new);
        warningsSheet.addRow(rowValues);
    }

    public void collectErrors(EndpointMethod endpoint, ErrorCollector errorCollector) {
        Map<UserTrackError, StringBuilder> endpointErrors = errorCollector.getErrors();
        String methodKey = endpoint.getDeclaredClass().getName() + endpoint.getMethodSignature();
        if (endpointErrors.isEmpty() || !uniqueMethodErrors.add(methodKey)) {
            return;
        }
        String[] rowValues = Stream.concat(Stream.of(endpoint.getDeclaredClass().getName(), endpoint.getMethodSignature()),
            Arrays.stream(UserTrackError.values())
                .map(e -> Optional.ofNullable(endpointErrors.get(e)).map(StringBuilder::toString).orElse(""))
        ).toArray(String[]::new);
        errorsSheet.addRow(rowValues);
    }

    public void exportEntitiesDataToSheet() {
        entityClasses.forEach((entityClass, tableName) -> entitiesSheet.addRow(entityClass.getName(), tableName));
    }

    public void exportParametersDataToSheet() {
        List<Class<?>> classes = List.of(PathVariable.class, RequestParam.class, RequestBody.class, ModelAttribute.class);
        Map<String, Set<Class<?>>> paramClassToAnnotations = new TreeMap<>();
        moduleParameters.forEach((annotation, params) ->
            params.stream()
                .map(p -> p.split("\\s")[0])
                .forEach(classname -> paramClassToAnnotations.computeIfAbsent(classname,
                    annotations -> new HashSet<>(4)).add(annotation)));
        paramClassToAnnotations.keySet().forEach(paramClass -> {
            String[] rowValues = new String[PARAMETERS_TABLE_HEADERS.length];
            rowValues[0] = paramClass;
            for (int i = 0; i < classes.size(); i++) {
                Class<?> annotationClass = classes.get(i);
                String cellValue = moduleParameters.getOrDefault(annotationClass, emptySortedSet()).stream()
                    .map(p -> p.split("\\s"))
                    .filter(p -> p[0].equals(paramClass))
                    .map(p -> p[1])
                    .sorted()
                    .collect(Collectors.joining(", "));
                rowValues[i + 1] = cellValue;
            }
            parametersSheet.addRow(rowValues);
        });
    }

    public String getModuleName() {
        return moduleName;
    }

    public ExcelSheet getEndpointsSheet() {
        return endpointsSheet;
    }

    public List<ExcelSheet> getSheets() {
        return Arrays.asList(endpointsSheet, errorsSheet, warningsSheet, entitiesSheet, parametersSheet);
    }
}
