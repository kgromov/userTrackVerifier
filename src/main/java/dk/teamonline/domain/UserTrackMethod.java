package dk.teamonline.domain;

import dk.eg.sensum.userTrack.domain.UserTrackAction;
import dk.eg.sensum.userTrack.domain.UserTracking;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class UserTrackMethod {
    private final UserTrackAction action;
    private final List<UserTrackValue> userTrackParameters;

    public UserTrackMethod(UserTracking userTracking) {
        this.action = userTracking.action();
        this.userTrackParameters = Arrays.stream(userTracking.parameters())
            .filter(Objects::nonNull)
            .map(UserTrackValue::new)
            .collect(Collectors.toList());
    }

    public boolean hasRedundantParameters() {
        return action == UserTrackAction.IGNORE && !userTrackParameters.isEmpty();
    }

    public boolean hasDuplicatedParameters() {
        return new HashSet<>(userTrackParameters).size() < userTrackParameters.size();
    }

    public UserTrackAction getAction() {
        return action;
    }

    public List<UserTrackValue> getUserTrackParameters() {
        return Collections.unmodifiableList(userTrackParameters);
    }

    public boolean hasUserTrackParameters() {
        return !userTrackParameters.isEmpty();
    }

    public Set<UserTrackValue> getDuplicates() {
        return new HashSet<>(CollectionUtils.subtract(userTrackParameters, new HashSet<>(userTrackParameters)));
    }

    @Override
    public String toString() {
        return "@UserTracking(" +
            "action = " + action +
            ", parameters = {" + userTrackParameters.stream()
            .map(UserTrackValue::toString).collect(Collectors.joining(",\n\t", "\n\t", "\n")) +
            "})";
    }
}
