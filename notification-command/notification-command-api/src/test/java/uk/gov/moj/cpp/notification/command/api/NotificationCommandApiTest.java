package uk.gov.moj.cpp.notification.command.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.notification.common.FieldNames.STREAM_ID;
import static uk.gov.moj.cpp.notification.common.FieldNames.USER_ID;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;


@ExtendWith(MockitoExtension.class)
public class NotificationCommandApiTest {

    private static final String NOTIFICATION_SUBSCRIBE_COMMAND = "notification.subscribe";
    private static final String NOTIFICATION_SUBSCRIBE_BY_USER_ID_COMMAND = "notification.subscribe-by-user-id";

    @Mock
    private Sender sender;

    @Spy
    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private Logger logger;

    @InjectMocks
    private NotificationCommandApi notificationCommandApi;


    @Test
    public void shouldSubscribeByUserId() throws Exception {
        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID(NOTIFICATION_SUBSCRIBE_BY_USER_ID_COMMAND)
                        .withUserId(userId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build();


        notificationCommandApi.subscribeByUserId(command);

        final ArgumentCaptor<JsonEnvelope> envelopeCaptor = forClass(JsonEnvelope.class);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope alteredEnvelope = envelopeCaptor.getValue();

        assertThat(alteredEnvelope.metadata().name(), is(NOTIFICATION_SUBSCRIBE_COMMAND));

        final JsonObject payload = alteredEnvelope.payloadAsJsonObject();

        assertThat(payload.getString("subscriptionId"), is(subscriptionId));
        assertThat(payload.getString("ownerId"), is(userId));

        final JsonObject filterObject = alteredEnvelope.payloadAsJsonObject().getJsonObject("filter");

        assertThat(filterObject, is(notNullValue()));
        assertThat(filterObject.getString("type"), is("FIELD"));
        assertThat(filterObject.getString("name"), is("USER_ID"));
        assertThat(filterObject.getString("value"), is(userId));
        assertThat(filterObject.getString("operation"), is("EQUALS"));
    }

    @Test
    public void shouldSubscribeWithUserIdFilter() throws Exception {

        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();

        final JsonObject sourcePayload = createObjectBuilder()
                .add("subscriptionId", subscriptionId)
                .add("type", USER_ID.name())
                .build();

        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("notification.subscription-filter")
                .createdAt(ZonedDateTime.now())
                .withCausation(randomUUID())
                .withUserId(userId);


        final Envelope<JsonObject> command = Envelope.envelopeFrom(metadataBuilder.build(), sourcePayload);

        notificationCommandApi.subscribeWithFilter(command);

        final ArgumentCaptor<JsonEnvelope> envelopeCaptor = forClass(JsonEnvelope.class);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope alteredEnvelope = envelopeCaptor.getValue();

        assertThat(alteredEnvelope.metadata().name(), is("notification.subscribe"));

        final JsonObject payload = alteredEnvelope.payloadAsJsonObject();

        assertThat(payload.getString("subscriptionId"), is(subscriptionId));
        assertThat(payload.getString("ownerId"), is(userId));

        final JsonObject filterObject = alteredEnvelope.payloadAsJsonObject().getJsonObject("filter");

        assertThat(filterObject, is(notNullValue()));
        assertThat(filterObject.getString("type"), is("FIELD"));
        assertThat(filterObject.getString("name"), is(USER_ID.name()));
        assertThat(filterObject.getString("value"), is(userId));
        assertThat(filterObject.getString("operation"), is("EQUALS"));
    }


    @Test
    public void shouldSubscribeWithStreamIdFilter() throws Exception {

        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();
        final String streamId = randomUUID().toString();

        final JsonObject sourcePayload = createObjectBuilder()
                .add("subscriptionId", subscriptionId)
                .add("type", FIELD.name())
                .add("name", STREAM_ID.name())
                .add("value", streamId)
                .add("operation", EQUALS.name())
                .build();

        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("notification.subscription-filter")
                .createdAt(ZonedDateTime.now())
                .withCausation(randomUUID())
                .withUserId(userId);


        final Envelope<JsonObject> command = Envelope.envelopeFrom(metadataBuilder.build(), sourcePayload);

        notificationCommandApi.subscribeWithFilter(command);

        final ArgumentCaptor<JsonEnvelope> envelopeCaptor = forClass(JsonEnvelope.class);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope alteredEnvelope = envelopeCaptor.getValue();

        assertThat(alteredEnvelope.metadata().name(), is("notification.subscribe"));

        final JsonObject payload = alteredEnvelope.payloadAsJsonObject();

        assertThat(payload.getString("subscriptionId"), is(subscriptionId));
        assertThat(payload.getString("ownerId"), is(userId));

        final JsonObject filterObject = alteredEnvelope.payloadAsJsonObject().getJsonObject("filter");

        assertThat(filterObject, is(notNullValue()));
        assertThat(filterObject.getString("type"), is("FIELD"));
        assertThat(filterObject.getString("name"), is(STREAM_ID.name()));
        assertThat(filterObject.getString("value"), is(streamId));
        assertThat(filterObject.getString("operation"), is("EQUALS"));
    }

    @Test
    public void shouldPassThroughForUnsubscribe() throws Exception {
        assertThat(NotificationCommandApi.class, isHandlerClass(COMMAND_API)
                .with(method("unsubscribe")
                        .thatHandles("notification.unsubscribe")
                        .withSenderPassThrough()));
    }
}