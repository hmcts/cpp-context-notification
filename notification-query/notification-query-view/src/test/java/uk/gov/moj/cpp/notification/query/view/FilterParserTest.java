package uk.gov.moj.cpp.notification.query.view;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.notification.common.FieldNames.NAME;
import static uk.gov.moj.cpp.notification.common.FieldNames.STREAM_ID;
import static uk.gov.moj.cpp.notification.common.FieldNames.USER_ID;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;

import uk.gov.moj.cpp.notification.common.FilterType;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class FilterParserTest {

    private static final JsonObject USER_ID_FILTER = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", USER_ID.name())
            .add("value", "testUser")
            .add("operation", EQUALS.name())
            .build();

    private static final JsonObject STREAM_ID_FILTER_1 = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", STREAM_ID.name())
            .add("value", "streamValue_1")
            .add("operation", EQUALS.name())
            .build();

    private static final JsonObject STREAM_ID_FILTER_2 = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", STREAM_ID.name())
            .add("value", "streamValue_2")
            .add("operation", EQUALS.name())
            .build();

    private static final JsonObject STREAM_ID_FILTER_3 = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", STREAM_ID.name())
            .add("value", "streamValue_3")
            .add("operation", EQUALS.name())
            .build();

    private static final JsonObject NAME_FILTER = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", NAME.name())
            .add("value", "eventName")
            .add("operation", EQUALS.name())
            .build();

    @Test
    public void shouldBuildFilterQueryForFieldTypeQuery() {
        final String filterQuery = FilterParser.parse(USER_ID_FILTER);

        assertThat(filterQuery, is("USER_ID = 'testUser'"));
    }

    @Test
    public void shouldBuildFilterQueryForORFieldTypeQuery() {
        final JsonObject filter = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(USER_ID_FILTER)
                        .add(STREAM_ID_FILTER_1)
                        .build())
                .build();

        final String filterQuery = FilterParser.parse(filter);

        assertThat(filterQuery, is("(USER_ID = 'testUser' OR STREAM_ID = 'streamValue_1')"));
    }

    @Test
    public void shouldBuildFilterQueryForANDFieldTypeQuery() {
        final JsonObject filter = Json.createObjectBuilder()
                .add("type", FilterType.AND.name())
                .add("value", Json.createArrayBuilder()
                        .add(USER_ID_FILTER)
                        .add(STREAM_ID_FILTER_1)
                        .build())
                .build();

        final String filterQuery = FilterParser.parse(filter);

        assertThat(filterQuery, is("(USER_ID = 'testUser' AND STREAM_ID = 'streamValue_1')"));
    }

    @Test
    public void shouldBuildFilterQueryForNestedAndFieldTypeQuery() {
        final JsonObject nestedFilter = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(USER_ID_FILTER)
                        .add(STREAM_ID_FILTER_1)
                        .build())
                .build();

        final JsonObject filter = Json.createObjectBuilder()
                .add("type", FilterType.AND.name())
                .add("value", Json.createArrayBuilder()
                        .add(NAME_FILTER)
                        .add(nestedFilter)
                        .build())
                .build();

        final String filterQuery = FilterParser.parse(filter);

        assertThat(filterQuery, is("(NAME = 'eventName' AND (USER_ID = 'testUser' OR STREAM_ID = 'streamValue_1'))"));
    }

    @Test
    public void shouldBuildFilterQueryForNestedAndFieldTypeQueryWithMoreThanTwoFiltersPerLevel() {
        final JsonObject nestedFilter = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(STREAM_ID_FILTER_1)
                        .add(STREAM_ID_FILTER_2)
                        .add(STREAM_ID_FILTER_3)
                        .build())
                .build();

        final JsonObject filter = Json.createObjectBuilder()
                .add("type", FilterType.AND.name())
                .add("value", Json.createArrayBuilder()
                        .add(NAME_FILTER)
                        .add(USER_ID_FILTER)
                        .add(nestedFilter)
                        .build())
                .build();

        final String filterQuery = FilterParser.parse(filter);

        assertThat(filterQuery, is("(NAME = 'eventName' AND USER_ID = 'testUser' AND (STREAM_ID = 'streamValue_1' OR STREAM_ID = 'streamValue_2' OR STREAM_ID = 'streamValue_3'))"));
    }

    @Test
    public void shouldBuildFilterQueryForMultipleNestedLevels() {
        final JsonObject nestedFilter_3 = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(STREAM_ID_FILTER_3)
                        .build())
                .build();

        final JsonObject nestedFilter_2 = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(STREAM_ID_FILTER_2)
                        .add(nestedFilter_3)
                        .build())
                .build();

        final JsonObject nestedFilter_1 = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(STREAM_ID_FILTER_1)
                        .add(nestedFilter_2)
                        .build())
                .build();

        final JsonObject filter = Json.createObjectBuilder()
                .add("type", FilterType.AND.name())
                .add("value", Json.createArrayBuilder()
                        .add(NAME_FILTER)
                        .add(USER_ID_FILTER)
                        .add(nestedFilter_1)
                        .build())
                .build();

        final String filterQuery = FilterParser.parse(filter);

        assertThat(filterQuery, is("(NAME = 'eventName' AND USER_ID = 'testUser' AND (STREAM_ID = 'streamValue_1' OR (STREAM_ID = 'streamValue_2' OR (STREAM_ID = 'streamValue_3'))))"));
    }
}