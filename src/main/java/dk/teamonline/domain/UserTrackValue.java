package dk.teamonline.domain;

import com.google.common.base.Objects;
import dk.eg.sensum.userTrack.domain.UserTrackParameter;

public class UserTrackValue {
    private final String expression;
    private final Class<?> type;

    public UserTrackValue(UserTrackParameter userTrackParameter) {
        this.expression = userTrackParameter.expression();
        this.type = userTrackParameter.type();
    }

    public String getExpression() {
        return expression;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTrackValue that = (UserTrackValue) o;
        return Objects.equal(expression, that.expression) &&
            Objects.equal(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expression, type);
    }

    @Override
    public String toString() {
        return "@UserTrackParameter(" +
            "expression = \"" + expression + '\"' +
            ", type = " + type.getSimpleName() + ".class)";
    }
}
