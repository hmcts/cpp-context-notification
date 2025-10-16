package uk.gov.moj.cpp.notification.query.view;

import uk.gov.moj.cpp.notification.common.FilterType;
import uk.gov.moj.cpp.notification.common.OperationType;

import java.util.Objects;

public class Filter {

    private final FilterType type;
    private final String name;
    private final String value;
    private final OperationType operation;

    public Filter(
            final FilterType type,
            final String name,
            final String value,
            final OperationType operation) {
        this.type = type;
        this.name = name;
        this.value = value;
        this.operation = operation;
    }

    public FilterType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public OperationType getOperation() {
        return operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Filter)) {
            return false;
        }

        Filter filter = (Filter) o;
        return getType() == filter.getType() &&
                Objects.equals(getName(), filter.getName()) &&
                Objects.equals(getValue(), filter.getValue()) &&
                Objects.equals(getOperation(), filter.getOperation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getName(), getValue(), getOperation());
    }

    @Override
    public String toString() {
        return "Filter{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", operation='" + operation + '\'' +
                '}';
    }
}
