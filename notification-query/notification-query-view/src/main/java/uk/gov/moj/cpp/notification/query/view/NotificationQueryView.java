package uk.gov.moj.cpp.notification.query.view;

import static java.util.Collections.emptyList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.notification.query.view.FilterService.CLIENT_CORRELATION_ID_PROPERTY_NAME;
import static uk.gov.moj.cpp.notification.query.view.FilterService.SUBSCRIPTION_ID_PROPERTY_NAME;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.notification.persistence.SubscriptionRepository;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.json.JSONObject;

public class NotificationQueryView {

    public static final String METADATA = "_metadata";
    @Inject
    private FilterService filterService;

    @Inject
    private FilteredEventService filteredEventService;

    @Inject
    private EventCacheListConverter eventCacheListConverter;

    @Inject
    private ExpiredSubscriptionsListConverter expiredSubscriptionsListConverter;

    @Inject
    private Enveloper enveloper;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    public JsonEnvelope findEvents(final JsonEnvelope query) {

        final JsonObject payload = query.payloadAsJsonObject();

        final UUID subscriptionId = UUID.fromString(payload
                .getString(SUBSCRIPTION_ID_PROPERTY_NAME));

        final Optional<String> optionalClientCorrelationId = payload.containsKey(CLIENT_CORRELATION_ID_PROPERTY_NAME)
                ? Optional.of(payload.getString(CLIENT_CORRELATION_ID_PROPERTY_NAME))
                : Optional.empty();

        final Optional<JsonObject> jsonFilter = filterService.findJsonFilter(subscriptionId);
        final List<EventCache> matchingEvents = jsonFilter.isPresent()
                ? filteredEventService.findEventsBy(jsonFilter.get(), optionalClientCorrelationId)
                : emptyList();

        return enveloper.withMetadataFrom(query, "notification.events").apply(eventsPayloadFrom(matchingEvents));
    }

    public JsonEnvelope findEventsMetadata(final JsonEnvelope query) {

        final JsonObject payload = query.payloadAsJsonObject();

        final UUID subscriptionId = UUID.fromString(payload
                .getString(SUBSCRIPTION_ID_PROPERTY_NAME));

        final Optional<String> optionalClientCorrelationId = payload.containsKey(CLIENT_CORRELATION_ID_PROPERTY_NAME)
                ? Optional.of(payload.getString(CLIENT_CORRELATION_ID_PROPERTY_NAME))
                : Optional.empty();

        final Optional<JsonObject> jsonFilter = filterService.findJsonFilter(subscriptionId);
        final List<EventCache> matchingEvents = jsonFilter.isPresent()
                ? filteredEventService.findEventsBy(jsonFilter.get(), optionalClientCorrelationId)
                : emptyList();

        final List<EventCache> modifiedEvents = matchingEvents.stream().map(this::filterMetadata).collect(Collectors.toList());
        return envelopeFrom(metadataFrom(query.metadata()).withName("notification.events"),
                createObjectBuilder()
                        .add("events", eventsPayloadFrom(modifiedEvents).getJsonArray("events"))
        );
    }

    private EventCache filterMetadata(final EventCache eventCache) {
        final String eventJson = eventCache.getEventJson();
        final JSONObject eventJsonObject = new JSONObject(eventJson);
        JSONObject metadata = new JSONObject();
        if (eventJsonObject.has(METADATA)) {
            metadata = new JSONObject(eventJson).getJSONObject(METADATA);
        }
        final JSONObject jsonWithMetaDataOnly = new JSONObject();
        jsonWithMetaDataOnly.put(METADATA,metadata);
        return new EventCache(eventCache.getId(),eventCache.getUserId(),eventCache.getSessionId(),eventCache.getClientCorrelationId(),eventCache.getStreamId(),jsonWithMetaDataOnly.toString(),eventCache.getCreated(),eventCache.getName());

    }

    public JsonEnvelope getSubscription(final JsonEnvelope query) {

        final UUID subscriptionId = UUID.fromString(query
                .payloadAsJsonObject()
                .getString(SUBSCRIPTION_ID_PROPERTY_NAME));

        final Subscription subscription = subscriptionRepository.findBy(subscriptionId);

        return enveloper.withMetadataFrom(query, "notification.subscription").apply(payloadFrom(subscription));
    }

    public JsonEnvelope findExpiredSubscriptions(final JsonEnvelope query) {

        final List<Subscription> subscriptions = subscriptionRepository.findExpiredSubscriptions();

        return enveloper.withMetadataFrom(query, "notification.expired.subscription").apply(expiredSubscriptionsPayloadFrom(subscriptions));
    }

    private JsonObject payloadFrom(final Subscription subscription) {
        if (null == subscription) {
            return null;
        } else {
            return createObjectBuilder()
                    .add("subscriptionId", subscription.getId().toString())
                    .add("ownerId", subscription.getOwnerId().toString())
                    .build();
        }
    }

    private JsonObject expiredSubscriptionsPayloadFrom(final List<Subscription> subscriptions) {
        return expiredSubscriptionsListConverter.convert(subscriptions);
    }

    private JsonObject eventsPayloadFrom(final List<EventCache> events) {
        return eventCacheListConverter.convert(events);
    }
}
