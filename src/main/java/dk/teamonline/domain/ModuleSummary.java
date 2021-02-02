package dk.teamonline.domain;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Table;
import java.util.*;
import java.util.stream.Collectors;

public class ModuleSummary {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleSummary.class);
    private final String moduleName;

    private final Set<Class<?>> entityClasses = new TreeSet<>(Comparator.comparing(Class::getName));
    private final Map<Class<?>, SortedSet<String>> moduleParameters = new HashMap<>();

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
        entityClasses.addAll(methodTrackedEntities);
    }

    public void printModuleSummary() {
        String decoration = StringUtils.repeat('~', 20);
        LOGGER.info("\n{} Module '{}' summary {}:", decoration, moduleName, decoration);
        String entityClassesToTableName = entityClasses.stream()
            .map(c -> c.getName() + "(tableName = " + getTableName(c) + ")")
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
}
