package uk.gov.moj.cpp.notification.command.api;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.notification.common.FieldNames.USER_ID;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.notification.common.OperationType;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

@ServiceComponent(COMMAND_API)
public class NotificationCommandApi {

    private static final String NOTIFICATION_SUBSCRIBE_COMMAND = "notification.subscribe";
    private static final String SUBSCRIPTION_ID = "subscriptionId";
    private static final String OWNER_ID = "ownerId";
    private static final String FILTER = "filter";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String OPERATION = "operation";
    @Inject
    Enveloper enveloper;

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    /**
     * Converts subscribe-by-user-id to the generic subscribe command by building the appropriate
     * filter
     *
     * @param command json envelope
     */
    @Handles("notification.subscribe-by-user-id")
    public void subscribeByUserId(final JsonEnvelope command) {

        final String userId = command.metadata().userId()
                .orElseThrow(() -> new BadRequestException("Metadata must contain user id"));

        final String subscriptionId = command.payloadAsJsonObject().getString(SUBSCRIPTION_ID);
        final JsonObject payload = createObjectBuilder()
                .add(OWNER_ID, userId)
                .add(SUBSCRIPTION_ID, subscriptionId)
                .add(FILTER, createObjectBuilder()
                        .add(TYPE, FIELD.name())
                        .add(NAME, USER_ID.name())
                        .add(VALUE, userId)
                        .add(OPERATION, EQUALS.name())
                        .build())
                .build();

        final JsonEnvelope jsonEnvelope = enveloper
                .withMetadataFrom(command, NOTIFICATION_SUBSCRIBE_COMMAND)
                .apply(payload);

        sender.send(jsonEnvelope);
    }

    @Handles("notification.unsubscribe")
    public void unsubscribe(final JsonEnvelope command) {
        sender.send(command);
    }

    @Handles("notification.subscription-filter")
    public void subscribeWithFilter(final Envelope<JsonObject> command) {

        final String userId = command.metadata().userId()
                .orElseThrow(() -> new BadRequestException("Metadata must contain user id"));

        final String subscriptionId = command.payload().getString(SUBSCRIPTION_ID);

        final JsonObject filterPayload = createObjectBuilderWithFilter(command.payload(),
                key -> !key.equals(SUBSCRIPTION_ID))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add(OWNER_ID, userId)
                .add(SUBSCRIPTION_ID, subscriptionId)
                .add(FILTER, filterWithUserId(filterPayload, userId))
                .build();

        final JsonValue payLoadAsJsonValue = objectToJsonValueConverter.convert(payload);

        final JsonEnvelope jsonEnvelope =
                JsonEnvelope.envelopeFrom(metadataWithNewActionName(command.metadata(), NOTIFICATION_SUBSCRIBE_COMMAND),
                        payLoadAsJsonValue);


        sender.send(jsonEnvelope);


    }

    private JsonObject filterWithUserId(final JsonObject jsonPayload, final String userId) {

        if (jsonPayload.containsKey(TYPE)
                && USER_ID.name().equals(jsonPayload.getString(TYPE))) {

            return createObjectBuilder()
                    .add(TYPE, FIELD.name())
                    .add(NAME, USER_ID.name())
                    .add(VALUE, userId)
                    .add(OPERATION, OperationType.EQUALS.name())
                    .build();
        } else {
            return jsonPayload;
        }

    }

    public static Metadata metadataWithNewActionName(final Metadata metadata, final String actionName) {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName(actionName)
                .createdAt(ZonedDateTime.now())
                .withCausation(metadata.causation().toArray(new UUID[metadata.causation().size()]));

        metadata.clientCorrelationId().ifPresent(metadataBuilder::withClientCorrelationId);
        metadata.sessionId().ifPresent(metadataBuilder::withSessionId);
        metadata.streamId().ifPresent(metadataBuilder::withStreamId);
        metadata.userId().ifPresent(metadataBuilder::withUserId);
        metadata.version().ifPresent(metadataBuilder::withVersion);

        return metadataBuilder.build();
    }

}
