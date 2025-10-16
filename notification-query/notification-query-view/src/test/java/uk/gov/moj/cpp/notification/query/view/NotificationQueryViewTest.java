package uk.gov.moj.cpp.notification.query.view;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.notification.persistence.SubscriptionRepository;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class NotificationQueryViewTest {

    private static final String ATTRIBUTE_SUBSCRIPTION_ID = "subscriptionId";
    private static final String ATTRIBUTE_CLIENT_CORRELATION_ID = "clientCorrelationId";
    private static final String ATTRIBUTE_SUBSCRIPTIONS_IDS = "subscriptionsIds";

    @Spy
    Enveloper enveloper = createEnveloper();

    @Mock
    EventCacheListConverter eventCacheListConverter;

    @Mock
    ExpiredSubscriptionsListConverter expiredSubscriptionsListConverter;

    @Mock
    FilteredEventService filteredEventService;

    @Mock
    FilterService filterService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private NotificationQueryView notificationQueryView;

    @Test
    public void shouldFindTheCorrectFilterThenGetTheEventsForThatFilterAndParseIntoAJsonArray() {

        final UUID subscriptionId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf(subscriptionId.toString(), ATTRIBUTE_SUBSCRIPTION_ID)
                .build();

        final JsonObject responseJson = createObjectBuilder()
                .add("events", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("newPayloadName", "newPayloadValue")
                                .build())
                        .build())
                .build();

        final JsonObject filter = mock(JsonObject.class);
        final List<EventCache> publicEvents = singletonList(mock(EventCache.class));

        when(filterService.findJsonFilter(subscriptionId)).thenReturn(of(filter));
        when(filteredEventService.findEventsBy(filter, Optional.empty())).thenReturn(publicEvents);

        when(eventCacheListConverter.convert(publicEvents)).thenReturn(responseJson);

        final JsonEnvelope events = notificationQueryView.findEvents(query);

        final JsonObject payload = events.payloadAsJsonObject();

        final JsonArray payloadJsonArray = payload.getJsonArray("events");

        assertThat(payloadJsonArray.size(), is(1));
        assertThat(payloadJsonArray.getJsonObject(0).getString("newPayloadName"), is("newPayloadValue"));
    }

    @Test
    public void shouldFindTheCorrectFilterThenGetTheEventsForThatFilterTakingIntoAccountFurtherFilteringUsingSuppliedCorrelationId() {

        final UUID subscriptionId = randomUUID();
        final UUID clientCorrelationId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf(subscriptionId.toString(), ATTRIBUTE_SUBSCRIPTION_ID)
                .withPayloadOf(clientCorrelationId.toString(), ATTRIBUTE_CLIENT_CORRELATION_ID)
                .build();

        final JsonObject responseJson = createObjectBuilder()
                .add("events", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("newPayloadName", "newPayloadValue")
                                .build())
                        .build())
                .build();

        final JsonObject filter = mock(JsonObject.class);
        final List<EventCache> publicEvents = singletonList(mock(EventCache.class));

        when(filterService.findJsonFilter(subscriptionId)).thenReturn(of(filter));
        when(filteredEventService.findEventsBy(filter, Optional.of(clientCorrelationId.toString()))).thenReturn(publicEvents);

        when(eventCacheListConverter.convert(publicEvents)).thenReturn(responseJson);

        final JsonEnvelope events = notificationQueryView.findEvents(query);

        final JsonObject payload = events.payloadAsJsonObject();

        final JsonArray payloadJsonArray = payload.getJsonArray("events");

        assertThat(payloadJsonArray.size(), is(1));
        assertThat(payloadJsonArray.getJsonObject(0).getString("newPayloadName"), is("newPayloadValue"));

        verify(filterService).findJsonFilter(subscriptionId);
        verify(filteredEventService).findEventsBy(filter, Optional.of(clientCorrelationId.toString()));
    }

    @Test
    public void shouldReturnConvertedEmptyListPayloadIfNoFiltersFound() {
        // this scenario cannot happen as a subscription cannot be present without a filter
        final UUID subscriptionId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf(subscriptionId.toString(), ATTRIBUTE_SUBSCRIPTION_ID)
                .build();

        final JsonObject responseJson = createObjectBuilder()
                .add("events", createArrayBuilder()
                        .build())
                .build();

        when(filterService.findJsonFilter(subscriptionId)).thenReturn(Optional.empty());
        when(eventCacheListConverter.convert(any())).thenReturn(responseJson);

        final JsonEnvelope events = notificationQueryView.findEvents(query);
        final JsonObject payload = events.payloadAsJsonObject();
        assertThat(payload.getJsonArray("events"), empty());

        verify(eventCacheListConverter).convert(any());
    }

    @Test
    public void shouldFindTheCorrectFilterThenGetTheEventsMetadataForThatFilterAndParseIntoAJsonArray() {

        final UUID subscriptionId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf(subscriptionId.toString(), ATTRIBUTE_SUBSCRIPTION_ID)
                .build();

        final JsonObject responseJson = createObjectBuilder()
                .add("events", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("newPayloadName", "newPayloadValue")
                                .build())
                        .build())
                .build();

        final JsonObject filter = mock(JsonObject.class);
        EventCache eventCache = mock(EventCache.class);

        JSONObject eventJson = new JSONObject();
        JSONObject metadataJson = new JSONObject();
        metadataJson.put("createdAt", randomUUID());
        eventJson.put("_metadata",metadataJson);
        eventJson.put("caseId", randomUUID());

        when(eventCache.getEventJson()).thenReturn(eventJson.toString());
        final List<EventCache> publicEvents = singletonList(eventCache);

        when(filterService.findJsonFilter(subscriptionId)).thenReturn(of(filter));
        when(filteredEventService.findEventsBy(filter, Optional.empty())).thenReturn(publicEvents);

        when(eventCacheListConverter.convert(any())).thenReturn(responseJson);

        final JsonEnvelope events = notificationQueryView.findEventsMetadata(query);

        final JsonObject payload = events.payloadAsJsonObject();

        final JsonArray payloadJsonArray = payload.getJsonArray("events");

        assertThat(payloadJsonArray.size(), is(1));
        assertThat(payloadJsonArray.getJsonObject(0).getString("newPayloadName"), is("newPayloadValue"));
    }


    @Test
    public void shouldReturnConvertedEmptyListMetadataPayloadIfNoFiltersFound() {
        // this scenario cannot happen as a subscription cannot be present without a filter
        final UUID subscriptionId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf(subscriptionId.toString(), ATTRIBUTE_SUBSCRIPTION_ID)
                .build();

        final JsonObject responseJson = createObjectBuilder()
                .add("events", createArrayBuilder()
                        .build())
                .build();

        when(filterService.findJsonFilter(subscriptionId)).thenReturn(Optional.empty());
        when(eventCacheListConverter.convert(any())).thenReturn(responseJson);

        final JsonEnvelope events = notificationQueryView.findEventsMetadata(query);
        final JsonObject payload = events.payloadAsJsonObject();
        assertThat(payload.getJsonArray("events"), empty());

        verify(eventCacheListConverter).convert(any());
    }

    @Test
    public void shouldGetSubscriptionByIdReturnedAsJson() {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();

        final Subscription subscription = mock(Subscription.class);

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf(subscriptionId.toString(), ATTRIBUTE_SUBSCRIPTION_ID)
                .build();

        when(subscription.getId()).thenReturn(subscriptionId);
        when(subscription.getOwnerId()).thenReturn(userId);
        when(subscriptionRepository.findBy(subscriptionId)).thenReturn(subscription);

        final JsonEnvelope jsonEnvelope = notificationQueryView.getSubscription(query);

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        assertThat(payload.getString(ATTRIBUTE_SUBSCRIPTION_ID), is(subscriptionId.toString()));
        assertThat(payload.getString("ownerId"), is(userId.toString()));

        final JsonObject metadata = jsonEnvelope.metadata().asJsonObject();
        assertThat(metadata.getString("name"), is("notification.subscription"));
    }

    @Test
    public void shouldReturnNullPayloadIfNoSubscriptionFound() {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();

        final Subscription subscription = mock(Subscription.class);

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .withPayloadOf(subscriptionId.toString(), ATTRIBUTE_SUBSCRIPTION_ID)
                .build();

        when(subscriptionRepository.findBy(subscriptionId)).thenReturn(null);

        final JsonEnvelope jsonEnvelope = notificationQueryView.getSubscription(query);
        assertThat(jsonEnvelope.payload(), is(NULL));

        final JsonObject metadata = jsonEnvelope.metadata().asJsonObject();
        assertThat(metadata.getString("name"), is("notification.subscription"));
    }

    @Test
    public void shouldReturnExpiredSubscriptions() {

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .build();

        String subscriptionId1 = String.valueOf(randomUUID());
        String subscriptionId2 = String.valueOf(randomUUID());

        final JsonObject responseJson = createObjectBuilder()
                .add(ATTRIBUTE_SUBSCRIPTIONS_IDS, createArrayBuilder()
                        .add(subscriptionId1)
                        .add(subscriptionId2)
                        .build())
                .build();

        final List<Subscription> subscriptionsList = singletonList(mock(Subscription.class));


        when(subscriptionRepository.findExpiredSubscriptions()).thenReturn(subscriptionsList);

        when(expiredSubscriptionsListConverter.convert(subscriptionsList)).thenReturn(responseJson);

        final JsonEnvelope subscriptions = notificationQueryView.findExpiredSubscriptions(query);

        final JsonObject payload = subscriptions.payloadAsJsonObject();

        final JsonArray payloadJsonArray = payload.getJsonArray(ATTRIBUTE_SUBSCRIPTIONS_IDS);

        assertThat(payloadJsonArray.size(), is(2));

        assertEquals(payloadJsonArray.getString(0), subscriptionId1);
        assertEquals(payloadJsonArray.getString(1), subscriptionId2);
    }

    @Test
    public void shouldReturnEmptyListOfExpiredSubscriptions() {

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults())
                .build();

        final JsonObject responseJson = createObjectBuilder()
                .add(ATTRIBUTE_SUBSCRIPTIONS_IDS, createArrayBuilder()
                        .build())
                .build();

        final List<Subscription> subscriptionsList = singletonList(mock(Subscription.class));

        when(subscriptionRepository.findExpiredSubscriptions()).thenReturn(subscriptionsList);

        when(expiredSubscriptionsListConverter.convert(subscriptionsList)).thenReturn(responseJson);

        final JsonEnvelope subscriptions = notificationQueryView.findExpiredSubscriptions(query);

        final JsonObject payload = subscriptions.payloadAsJsonObject();

        final JsonArray payloadJsonArray = payload.getJsonArray(ATTRIBUTE_SUBSCRIPTIONS_IDS);

        assertThat(payloadJsonArray.size(), is(0));

    }


}
