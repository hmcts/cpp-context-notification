package uk.gov.moj.cpp.notification.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.notification.test.utils.builder.FilterJsonBuilder.randomFilterJsonBuilder;

import uk.gov.justice.json.schemas.domains.notification.FilterUpdated;
import uk.gov.justice.json.schemas.domains.notification.Subscribed;
import uk.gov.justice.json.schemas.domains.notification.Unsubscribed;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.notification.aggregate.Subscription;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationCommandHandlerTest {

    private static final String SUBSCRIBED_EVENT_NAME = "notification.subscribed";
    private static final String UNSUBSCRIBED_EVENT_NAME = "notification.unsubscribed";
    private static final String UPDATE_FILTER_EVENT_NAME = "notification.filter-updated";

    private static final UUID subscriptionId = randomUUID();
    private static final UUID ownerId = randomUUID();
    private static final JsonObject filter = randomFilterJsonBuilder().build();
    private static final JsonObject newFilter = randomFilterJsonBuilder().build();

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private Subscription aggregate;

    @Mock
    private Stream<Object> events;

    @Mock
    private Stream<JsonEnvelope> jsonEnvelopes;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(Subscribed.class, Unsubscribed.class, FilterUpdated.class);

    @Spy
    private Clock clock = new StoppedClock(fromString("2016-10-11T17:05:10.151Z"));

    @InjectMocks
    private NotificationCommandHandler notificationCommandHandler;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSubscribe() throws Exception {
        when(eventSource.getStreamById(subscriptionId)).thenReturn(eventStream);
        final Subscription subscription = new Subscription();

        when(aggregateService.get(eventStream, Subscription.class)).thenReturn(subscription);

        final JsonEnvelope command = createSubscribeCommand();

        notificationCommandHandler.subscribe(command);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(command)
                                .withName(SUBSCRIBED_EVENT_NAME),
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.subscriptionId", equalTo(subscriptionId.toString())),
                                        withJsonPath("$.ownerId", equalTo(ownerId.toString())),
                                        withJsonPath("$.created", equalTo(ZonedDateTimes.toString(clock.now()))),
                                        withJsonPath("$.filter.type", equalTo(filter.getString("type"))),
                                        withJsonPath("$.filter.name", equalTo(filter.getString("name"))),
                                        withJsonPath("$.filter.value", equalTo(filter.getString("value"))),
                                        withJsonPath("$.filter.operation", equalTo(filter.getString("operation")))
                                ))
                )
        ));
    }

    @Test
    public void shouldProduceUnSubscribedEvent() throws Exception {

        when(eventSource.getStreamById(subscriptionId)).thenReturn(eventStream);
        final Subscription subscription = new Subscription();
        final Subscribed subscribed = Subscribed.subscribed().withCreated(clock.now())
                .withFilter(filter)
                .withOwnerId(ownerId)
                .withSubscriptionId(subscriptionId)
                .build();
        subscription.apply(subscribed);
        when(aggregateService.get(eventStream, Subscription.class)).thenReturn(subscription);

        final JsonEnvelope command = createUnsubscribeCommand();

        notificationCommandHandler.unsubscribe(command);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(command)
                                .withName(UNSUBSCRIBED_EVENT_NAME),
                        payload()
                                .isJson(
                                        withJsonPath("$.subscriptionId", equalTo(subscriptionId.toString()))
                                )
                )
        ));
    }

    @Test
    public void shouldUpdateFilter() throws Exception {

        when(eventSource.getStreamById(subscriptionId)).thenReturn(eventStream);
        final Subscription subscription = new Subscription();
        final Subscribed subscribed = Subscribed.subscribed().withCreated(clock.now())
                .withFilter(filter)
                .withOwnerId(ownerId)
                .withSubscriptionId(subscriptionId)
                .build();
        subscription.apply(subscribed);
        when(aggregateService.get(eventStream, Subscription.class)).thenReturn(subscription);

        final JsonEnvelope command = createUpdateFilterCommand();

        notificationCommandHandler.subscribe(command);

        assertThat(verifyAppendAndGetArgumentFrom(eventStream), streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(command)
                                .withName(UPDATE_FILTER_EVENT_NAME),
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.filter.type", equalTo(newFilter.getString("type"))),
                                        withJsonPath("$.filter.name", equalTo(newFilter.getString("name"))),
                                        withJsonPath("$.filter.value", equalTo(newFilter.getString("value"))),
                                        withJsonPath("$.filter.operation", equalTo(newFilter.getString("operation")))
                                ))
                )
        ));

    }

    private JsonEnvelope createSubscribeCommand() {
        return envelope()
                .with(metadataWithRandomUUID(SUBSCRIBED_EVENT_NAME))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .withPayloadOf(ownerId, "ownerId")
                .withPayloadOf(filter, "filter")
                .build();
    }

    private JsonEnvelope createUnsubscribeCommand() {
        return envelope()
                .with(metadataWithRandomUUID(UNSUBSCRIBED_EVENT_NAME))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build();
    }

    private JsonEnvelope createUpdateFilterCommand() {
        return envelope()
                .with(metadataWithRandomUUID(UPDATE_FILTER_EVENT_NAME))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .withPayloadOf(ownerId, "ownerId")
                .withPayloadOf(newFilter, "filter")
                .build();
    }
}
