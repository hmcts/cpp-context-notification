package uk.gov.moj.cpp.notification.test.utils.builder;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;

import uk.gov.moj.cpp.notification.common.FilterType;
import uk.gov.moj.cpp.notification.common.OperationType;

import javax.json.JsonObject;

public class FilterJsonBuilder {

    private FilterType type;
    private String name;
    private String value;
    private OperationType operation;

    private FilterJsonBuilder() {
    }

    public static FilterJsonBuilder filterJsonBuilder() {
        return new FilterJsonBuilder();
    }

    public static FilterJsonBuilder randomFilterJsonBuilder() {
        FilterJsonBuilder builder = new FilterJsonBuilder();
        builder.withType(randomEnum(FilterType.class).next());
        builder.withName(STRING.next());
        builder.withValue(STRING.next());
        builder.withOperation(randomEnum(OperationType.class).next());

        return builder;
    }

    public FilterJsonBuilder withType(final FilterType type) {
        this.type = type;
        return this;
    }

    public FilterJsonBuilder withName(final String name) {
        this.name = name;
        return this;
    }

    public FilterJsonBuilder withValue(final String value) {
        this.value = value;
        return this;
    }

    public FilterJsonBuilder withOperation(final OperationType operation) {
        this.operation = operation;
        return this;
    }

    public JsonObject build() {
        return createObjectBuilder()
                .add("type", type.name())
                .add("name", name)
                .add("value", value)
                .add("operation", operation.name())
                .build();
    }
}
