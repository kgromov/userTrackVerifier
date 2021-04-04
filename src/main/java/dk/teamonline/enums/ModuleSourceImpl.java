package dk.teamonline.enums;

import dk.teamonline.ModuleSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ModuleSourceImpl implements ModuleSource {
    ACADRE("acadre", "ui"),
    ACTIVITY("activity", "extws"),
    ADMISSION("admission", "ui", "ws"),
    ASSIGNMENT("assignment", "ui", "extws"),
    ATTACHMENT("attachment", "ui"),
    AUTHORITY_TREATMENT("authoritytreatment", "ui", "extws"),

    CASES("cases", "ui"),
    CITIZEN("citizen", "ui") {
        @Override
        public List<String> getPackages() {
            return List.of(
                "dk.teamonline.citizen",
                "dk.teamonline.citizenRelation"
            );
        }
    },
    CITIZEN_PROCESS("citizenprocess", "ui", "ws", "extws") {
        // Due to keeping empty java files without class declaration like: CitizenProcessAccessHelperTest
        @Override
        public List<Path> getSourcePaths() {
            Path modulePath = Paths.get(".").normalize().toAbsolutePath().resolve(getModuleName());
            return Arrays.stream(getModuleTypes())
                .map(modulePath::resolve)
                .collect(Collectors.toList());
        }
    },
    CITIZEN_PROCESS_STATISTICS("citizenprocessStatistics", "ui"),
    COURSE("course", "ui"),
    COURSES("courses", "ui"),
    CUSTOM_FIELD("customfield", "ui") {
        @Override
        public List<String> getPackages() {
            return List.of("dk.eg.sensum.customfield.ui");
        }
    },

    DASHBOARD("dashboard", "ui") {
        @Override
        public List<String> getPackages() {
            return List.of("dk.eg.sensum.dashboard.ui");
        }
    },
    DIGITAL_POST("digitalPost", "ui", "extws"),
    DISPOSITION_BUDGET("dispositionbudget", "ui"),
    DOCUMENT("document", "ui"),
    DUBU("dubu", "ui") {
        @Override
        public List<String> getPackages() {
            return List.of(
                "dk.teamonline.childNotification.ui",
                "dk.teamonline.childPlacement.ui",
                "dk.teamonline.schoolDaycare.ui"
            );
        }
    },

    EAN13("ean13", "ui"),
    EVALUATION("evaluation", "ui", "extws") {
        @Override
        public List<String> getPackages() {
            return List.of(
                "dk.eg.sensum.evaluation.ui"
//               "dk.teamonline.subgoal.extws"        // incorrect package name - fully intersected with package defined in Subgoal module
            );
        }

        // Due to keeping empty java files without class declaration like: EvaluationChildOrgContextsFilteringTest.java
        @Override
        public List<Path> getSourcePaths() {
            Path modulePath = Paths.get(".").normalize().toAbsolutePath().resolve(getModuleName());
            return Arrays.stream(getModuleTypes())
                .map(modulePath::resolve)
                .collect(Collectors.toList());
        }
    },

    FORCE_SCHEME("forcescheme", "ui", "ws"),

    GOAL("goal", "ui", "ws", "extws"),
    GOAL_INDICATOR("goalIndicator", "ui", "ws"),

    INSTANT_MESSAGE("instantMessage", "ui", "ws", "extws") {
        @Override
        public List<String> getPackages() {
            String module = getModuleName();
            return Arrays.stream(getModuleTypes())
                .map(type -> String.format(DEFAULT_PACKAGE_PATTERN,
                    type.equals("extws") ? "internalMessages" : module,
                    type))
                .collect(Collectors.toUnmodifiableList());
        }
    },
    INSTANT_MESSAGE_DISTRIBUTION("instantMessageDistribution", "ui", "ws"),
    INVOICE("invoice", "ui"),

    JOURNAL("journal", "ui", "extws"),
    JOURNAL_INDICATOR_STATISTIC("journalIndicatorStatistic", "ui", "extws"),

    MAIL_JOURNALIZING("mailjournalizing", "ui", "ws", "extws") {
        @Override
        public List<String> getPackages() {
            String module = getModuleName();
            return Arrays.stream(getModuleTypes())
                .map(type -> {
                    String resolvedPackage = String.format(DEFAULT_PACKAGE_PATTERN, module, type);
                    return type.equals("ui") ? resolvedPackage.replace("teamonline", "eg.sensum") : resolvedPackage;
                })
                .collect(Collectors.toUnmodifiableList());
        }
    },
    MEDICINE("medicine", "ui", "extws"),

    PAYOUT("payout", "ui") {
        @Override
        public List<String> getPackages() {
            return List.of("dk.eg.sensum.payout.ui");
        }
    },
    PERSON("person", "ui", "extws") {
        @Override
        public List<String> getPackages() {
            return List.of(
                "dk.teamonline.country.ui",
                "dk.teamonline.department.ui",
                "dk.teamonline.person.ui",
                "dk.teamonline.reason.ui",
                "dk.teamonline.team.ui",
                "dk.teamonline.user.ui",
                "dk.teamonline.person.extws"
            );
        }
    },
    PLAN("plan", "ui", "ws", "extws"),

    RESIDENT("resident", "ui", "extws") {
        @Override
        public List<String> getPackages() {
            return List.of(
                "dk.teamonline.resident.ui",
                "dk.teamonline.resident.ws",
                "dk.teamonline.schoolDaycare.ui"
            );
        }
    },
    RESIDENT_AWAY("residentAway", "ui", "extws"),
    RESIDENT_DISCHARGE("residentDischarge", "ui"),
    RESIDENT_SALARY("residentsalary", "ui"),

    SUBGOAL("subgoal", "ui", "extws") {
        @Override
        public List<String> getPackages() {
            return List.of(
                "dk.teamonline.subgoal.ui",
                "dk.teamonline.subgoal.extws",
                "dk.teamonline.subgoal.intws"
            );
        }
    },
    SUPPLIER_CASE("supplierCase", "ui") {
        @Override
        public List<String> getPackages() {
            return List.of("dk.eg.sensum.supplierCase.ui");
        }
    };

    private static final String DEFAULT_PACKAGE_PATTERN = "dk.teamonline.%s.%s";
    private static final Map<String, ModuleSource> MODULE_SOURCES = Arrays.stream(values())
        .collect(Collectors.toMap(ModuleSourceImpl::getModuleName, Function.identity()));

    private final String moduleName;
    private final String[] moduleTypes;

    ModuleSourceImpl(String moduleName, String... moduleTypes) {
        this.moduleName = moduleName;
        this.moduleTypes = moduleTypes;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    public String[] getModuleTypes() {
        return moduleTypes;
    }

    @Override
    public List<String> getPackages() {
        return Arrays.stream(moduleTypes)
            .map(type -> String.format(DEFAULT_PACKAGE_PATTERN, moduleName, type))
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Path> getSourcePaths() {
        return List.of(Paths.get(".").normalize().toAbsolutePath().resolve(moduleName));
    }

    public static ModuleSource getModuleSourceByModuleName(String moduleName) {
        return Optional.ofNullable(MODULE_SOURCES.get(moduleName))
            .orElseThrow(() -> new IllegalArgumentException("Unknown module = " + moduleName));
    }
}
