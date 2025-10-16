package uk.gov.moj.cpp.notification.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.notification.aggregate.Subscription;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
public class NotificationCommandHandler {

    private static final String SUBSCRIPTION_ID_FIELD_NAME = "subscriptionId";
    private static final String OWNER_ID_FIELD_NAME = "ownerId";
    private static final String FILTER_FIELD_NAME = "filter";

    @Inject
    AggregateService aggregateService;

    @Inject
    EventSource eventSource;

    @Inject
    Enveloper enveloper;

    @Inject
    Clock clock;

    @Handles("notification.subscribe")
    public void subscribe(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final UUID subscriptionId = UUID.fromString(payload.getString(SUBSCRIPTION_ID_FIELD_NAME));
        final UUID ownerId = UUID.fromString(payload.getString(OWNER_ID_FIELD_NAME));
        final JsonObject filter = payload.getJsonObject(FILTER_FIELD_NAME);
        final ZonedDateTime created = clock.now();

        final EventStream eventStream = eventSource.getStreamById(subscriptionId);
        final Subscription aggregate = aggregateService.get(eventStream, Subscription.class);

        final Stream<Object> events;
        if (aggregate.exists()) {
            events = aggregate.updateFilter(filter, created);
        }
        else {
            events = aggregate.create(created, filter, ownerId, subscriptionId);
        }

        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }

    @Handles("notification.unsubscribe")
    public void unsubscribe(final JsonEnvelope command) throws EventStreamException {
        final UUID subscriptionId = UUID.fromString(command.payloadAsJsonObject().getString(SUBSCRIPTION_ID_FIELD_NAME));

        final EventStream eventStream = eventSource.getStreamById(subscriptionId);
        final Subscription aggregate = aggregateService.get(eventStream, Subscription.class);

        final Stream<Object> events = aggregate.cancel();

        eventStream.append(events.map(enveloper.withMetadataFrom(command)));
    }
}
