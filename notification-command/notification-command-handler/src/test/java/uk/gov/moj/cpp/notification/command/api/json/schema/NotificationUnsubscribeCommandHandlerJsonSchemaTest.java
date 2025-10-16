package uk.gov.moj.cpp.notification.command.api.json.schema;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonSchemaValidationMatcher.failsValidationForAnyMissingField;
import static uk.gov.justice.services.test.utils.core.matchers.JsonSchemaValidationMatcher.failsValidationWithMessage;
import static uk.gov.justice.services.test.utils.core.matchers.JsonSchemaValidationMatcher.isValidForSchema;

import org.junit.jupiter.api.Test;

public class NotificationUnsubscribeCommandHandlerJsonSchemaTest {

    private static final String JSON_SCHEMA = "json/schema/notification.unsubscribe.json";

    private static final String VALID_JSON = "raml/json/notification.unsubscribe.valid.json";
    private static final String INVALID_SUBSCRIPTION_ID_JSON = "raml/json/notification.unsubscribe.invalid.subscription.id.json";
    private static final String VALID_EXTRA_FIELD_JSON = "raml/json/notification.unsubscribe.valid.extra.field.json";

    @Test
    public void shouldNotFailForValidJsonContent() throws Exception {
        assertThat(VALID_JSON, isValidForSchema(JSON_SCHEMA));
    }

    @Test
    public void shouldFailWhenSubscriptionIdIsNotAValidUUID() throws Exception {
        assertThat(INVALID_SUBSCRIPTION_ID_JSON, failsValidationWithMessage(JSON_SCHEMA,
                "#/subscriptionId: string [d06f6539-2a7c-bca3-f1b17ebcfa47] does not match pattern ^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"));
    }

    @Test
    public void shouldFailWhenOneOfTheFieldIsMissingFromJsonRoot() throws Exception {
        assertThat(VALID_JSON, failsValidationForAnyMissingField(JSON_SCHEMA));
    }

    @Test
    public void shouldNotFailWhenThereIsExtraFieldAtJsonRoot() throws Exception {
        assertThat(VALID_EXTRA_FIELD_JSON, isValidForSchema(JSON_SCHEMA));
    }

}
