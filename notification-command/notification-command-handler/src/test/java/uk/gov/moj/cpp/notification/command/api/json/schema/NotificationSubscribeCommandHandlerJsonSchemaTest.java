package uk.gov.moj.cpp.notification.command.api.json.schema;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.notification.test.utils.matcher.CatalogedSchemaValidationMatcher.failsValidationForAnyMissingField;
import static uk.gov.moj.cpp.notification.test.utils.matcher.CatalogedSchemaValidationMatcher.failsValidationWithMessage;
import static uk.gov.moj.cpp.notification.test.utils.matcher.CatalogedSchemaValidationMatcher.isValidForSchema;

import org.junit.jupiter.api.Test;

public class NotificationSubscribeCommandHandlerJsonSchemaTest {

    private static final String JSON_SCHEMA = "json/schema/notification.subscribe.json";

    private static final String VALID_USERID_FILTER_JSON = "raml/json/notification.subscribe.valid-userid-filter.json";
    private static final String VALID_STREAMID_FILTER_JSON = "raml/json/notification.subscribe.valid-streamid-filter.json";
    private static final String VALID_EVENTNAME_FILTER_JSON = "raml/json/notification.subscribe.valid-eventname-filter.json";
    private static final String VALID_NESTED_FILTER_JSON = "raml/json/notification.subscribe.valid-nested-filter.json";
    private static final String INVALID_MISSING_FIELD_JSON = "raml/json/notification.subscribe.invalid-missing-field.json";
    private static final String INVALID_MISSING_FILTER_FIELD_JSON = "raml/json/notification.subscribe.invalid-missing-filter-field.json";
    private static final String INVALID_EXTRA_FIELD_JSON = "raml/json/notification.subscribe.invalid.extra.field.json";
    private static final String INVALID_EXTRA_FILTER_FIELD_JSON = "raml/json/notification.subscribe.invalid.extra.filter.field.json";
    private static final String INVALID_SUBSCRIPTION_ID_JSON = "raml/json/notification.subscribe.invalid.subscription.id.json";
    private static final String INVALID_TYPE_JSON = "raml/json/notification.subscribe.invalid.type.json";
    private static final String INVALID_OPERATION_JSON = "raml/json/notification.subscribe.invalid.operation.json";
    private static final String INVALID_OWNER_ID_JSON = "raml/json/notification.subscribe.invalid.owner.id.json";
    private static final String OR_FILTER_JSON = "raml/json/notification.subscribe.or-filter.json";
    private static final String AND_FILTER_JSON = "raml/json/notification.subscribe.and-filter.json";
    private static final String AND_WITH_USERID_FILTER_JSON = "raml/json/notification.subscribe.and-with-userid-filter.json";
    private static final String AND_WITH_INVALID_TYPE_FILTER_JSON = "raml/json/notification.subscribe.and-with-invalid-type-filter.json";
    private static final String AND_WITH_INVALID_NAME_FILTER_JSON = "raml/json/notification.subscribe.and-with-invalid-name-filter.json";
    private static final String AND_WITH_INVALID_OPERATION_FILTER_JSON = "raml/json/notification.subscribe.and-with-invalid-operation-filter.json";
    private static final String INVALID_BOOLEAN_OPERATOR_FILTER_JSON = "raml/json/notification.subscribe.invalid-boolean-filter.json";

    @Test
    public void shouldSuccessfullyValidateUserIdFilter() throws Exception {
        assertThat(VALID_USERID_FILTER_JSON, isValidForSchema(JSON_SCHEMA));
    }

    @Test
    public void shouldSuccessfullyValidateStreamIdFilter() throws Exception {
        assertThat(VALID_STREAMID_FILTER_JSON, isValidForSchema(JSON_SCHEMA));
    }

    @Test
    public void shouldSuccessfullyValidateEventNameFilter() throws Exception {
        assertThat(VALID_EVENTNAME_FILTER_JSON, isValidForSchema(JSON_SCHEMA));
    }

    @Test
    public void shouldSuccessfullyValidateNestedFilter() throws Exception {
        assertThat(VALID_NESTED_FILTER_JSON, isValidForSchema(JSON_SCHEMA));
    }

    @Test
    public void shouldFailWhenSubscriptionIdIsNotAValidUUID() throws Exception {
        assertThat(INVALID_SUBSCRIPTION_ID_JSON, failsValidationWithMessage(JSON_SCHEMA,
                "#/subscriptionId: string [d06f6539-2a7c-bca3-f1b17ebcfa47] does not match pattern ^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"));
    }

    @Test
    public void shouldFailWhenOneOfTheFieldIsMissingFromJsonRoot() throws Exception {
        assertThat(INVALID_MISSING_FIELD_JSON, failsValidationForAnyMissingField(JSON_SCHEMA));
    }

    @Test
    public void shouldFailWhenThereIsExtraFieldOutsideFilterObject() throws Exception {
        assertThat(INVALID_EXTRA_FIELD_JSON, failsValidationWithMessage(JSON_SCHEMA,
                "#: extraneous key [extraField] is not permitted"));
    }

    @Test
    public void shouldFailWhenTypeIsNotAValidEnum() throws Exception {
        assertThat(INVALID_TYPE_JSON, failsValidationWithMessage(JSON_SCHEMA,
                "#/filter: #: only 1 subschema matches out of 2"));
    }

    @Test
    public void shouldFailWhenOperationIsNotAValidEnum() throws Exception {
        assertThat(INVALID_OPERATION_JSON, failsValidationWithMessage(JSON_SCHEMA,
                "#/filter: #: only 1 subschema matches out of 2"));
    }

    @Test
    public void shouldFailWhenUserIdIsNotAValidUUID() throws Exception {
        assertThat(INVALID_OWNER_ID_JSON, failsValidationWithMessage(JSON_SCHEMA,
                "#/ownerId: string [2c2f4447-f54a-4c93-bf7e074cbf84] does not match pattern ^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"));
    }

    @Test
    public void shouldPassValidationForComplexOrFilter() throws Exception {
        assertThat(OR_FILTER_JSON, isValidForSchema(JSON_SCHEMA));
    }

    @Test
    public void shouldPassValidationForComplexAndFilter() throws Exception {
        assertThat(AND_FILTER_JSON, isValidForSchema(JSON_SCHEMA));
    }

    @Test
    public void shouldPassValidationForComplexAndFilterWithAUserIfFilterType() throws Exception {
        assertThat(AND_WITH_USERID_FILTER_JSON, isValidForSchema(JSON_SCHEMA));
    }

    // TODO this fails on sub-schema validation, so we really need to make the exception messages clearer
    @Test
    public void shouldFailWhenInvalidBooleanComparatorType() throws Exception {
        assertThat(INVALID_BOOLEAN_OPERATOR_FILTER_JSON, failsValidationWithMessage(JSON_SCHEMA, "#/filter: #: only 1 subschema matches out of 2"));
    }

    // TODO this fails on sub-schema validation, so we really need to make the exception messages clearer
    @Test
    public void shouldFailWhenInvalidAndTypeFilter() throws Exception {
        assertThat(AND_WITH_INVALID_TYPE_FILTER_JSON, failsValidationWithMessage(JSON_SCHEMA, "#/filter: #: only 1 subschema matches out of 2"));
    }

    // TODO this fails on sub-schema validation, so we really need to make the exception messages clearer
    @Test
    public void shouldFailWhenInvalidAndNameFilter() throws Exception {
        assertThat(AND_WITH_INVALID_NAME_FILTER_JSON, failsValidationWithMessage(JSON_SCHEMA, "#/filter: #: only 1 subschema matches out of 2"));
    }

    @Test
    public void shouldFailWhenInvalidAndOperationFilter() throws Exception {
        assertThat(AND_WITH_INVALID_OPERATION_FILTER_JSON, failsValidationWithMessage(JSON_SCHEMA, "#/filter: #: only 1 subschema matches out of 2"));
    }

    @Test
    public void shouldFailWhenOneOfTheFilterFieldIsMissing() throws Exception {
        assertThat(INVALID_MISSING_FILTER_FIELD_JSON, failsValidationForAnyMissingField(JSON_SCHEMA));
    }

    @Test
    public void shouldFailWhenThereIsAnExtraFieldWithinFilterObject() throws Exception {
        assertThat(INVALID_EXTRA_FILTER_FIELD_JSON, failsValidationWithMessage(JSON_SCHEMA,
                "#/filter: #: only 1 subschema matches out of 2"));
    }

}
