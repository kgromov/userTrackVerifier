package dk.teamonline.domain;

import dk.teamonline.enums.UserTrackError;
import dk.teamonline.enums.UserTrackWarning;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

public class ErrorCollector {
    private final Map<UserTrackError, StringBuilder> errors = new EnumMap<>(UserTrackError.class);
    private final Map<UserTrackWarning, StringBuilder> warnings = new EnumMap<>(UserTrackWarning.class);

    public void appendError(UserTrackError error, String message) {
        errors.computeIfAbsent(error, messages -> new StringBuilder()).append('\n').append(message);
    }

    public void appendWarning(UserTrackWarning warning, String message) {
        warnings.computeIfAbsent(warning, messages -> new StringBuilder()).append('\n').append(message);
    }

    public Map<UserTrackError, StringBuilder> getErrors() {
        return Collections.unmodifiableMap(errors);
    }

    public Map<UserTrackWarning, StringBuilder> getWarnings() {
        return Collections.unmodifiableMap(warnings);
    }

    public String getErrorsAsString(UserTrackError... errorTypes) {
        Set<UserTrackError> targetErrorTypes = Optional.ofNullable(errorTypes).map(Set::of).orElse(emptySet());
        return errors.entrySet().stream()
            .filter(e -> targetErrorTypes.isEmpty() || targetErrorTypes.contains(e.getKey()))
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining("\n"));
    }

    public String getWarningsAsString(UserTrackWarning... warningTypes) {
        Set<UserTrackWarning> targetWarningTypes = Optional.ofNullable(warningTypes).map(Set::of).orElse(emptySet());
        return warnings.entrySet().stream()
            .filter(e -> targetWarningTypes.isEmpty() || targetWarningTypes.contains(e.getKey()))
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining("\n"));
    }
}
