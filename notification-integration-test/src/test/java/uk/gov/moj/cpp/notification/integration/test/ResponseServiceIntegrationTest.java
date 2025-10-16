package uk.gov.moj.cpp.notification.integration.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIn.isIn;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.notification.common.FieldNames.NAME;
import static uk.gov.moj.cpp.notification.common.FieldNames.STREAM_ID;
import static uk.gov.moj.cpp.notification.common.FieldNames.USER_ID;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.setupUserAsSystemUser;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.stubUserWithPermission;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.common.FilterType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResponseServiceIntegrationTest extends BaseIT {

    private static final String NOTIFICATION_ADDED_COMMAND_NAME = "public.listing.hearing-changes-saved";
    private static final String MATCHING_EVENTS_PAYLOAD_KEY = "jsonKeyB";
    private static final String NON_MATCHING_EVENTS_PAYLOAD_KEY = "jsonKeyA";
    private static final String AN_EMPTY_STRING = "";
    private static final String SUBSCRIPTION_COMMAND_API_PATH = "/notification-command-api/command/api/rest/notification/subscriptions/%s";
    private static final String SUBSCRIPTION_QUERY_API_PATH = "/notification-query-api/query/api/rest/notifications/subscriptions/%s";
    private static final String EVENTS_QUERY_API_PATH = "/notification-query-api/query/api/rest/notifications/subscriptions/%s/events";
    private static final UUID USER_UUID = randomUUID();

    private static final int NUMBER_OF_NON_MATCHING_EVENTS = 10 + RandomGenerator.integer(10).next();
    private static final int NUMBER_OF_MATCHING_EVENTS = 10 + RandomGenerator.integer(10).next();
    private static final String CONTEXT_NAME = "notification";

    private final RestClient restClient = new RestClient();
    private final Collection<String> expectedPayloadValues = new ArrayList<>();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();

    private final String SYSTEM_USER_ID = randomUUID().toString();


    private static final UUID COMPLEX_FILTER_USER_ID = randomUUID();
    private static final UUID COMPLEX_FILTER_STREAM_ID = randomUUID();
    private static final UUID COMPLEX_FILTER_STREAM_ID_2 = randomUUID();

    private static final JsonObject USER_ID_FILTER = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", USER_ID.name())
            .add("value", COMPLEX_FILTER_USER_ID.toString())
            .add("operation", EQUALS.name())
            .build();

    private static final JsonObject STREAM_ID_FILTER_1 = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", STREAM_ID.name())
            .add("value", COMPLEX_FILTER_STREAM_ID.toString())
            .add("operation", EQUALS.name())
            .build();

    private static final JsonObject STREAM_ID_FILTER_2 = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", STREAM_ID.name())
            .add("value", COMPLEX_FILTER_STREAM_ID_2.toString())
            .add("operation", EQUALS.name())
            .build();

    private static final JsonObject NAME_FILTER = Json.createObjectBuilder()
            .add("type", FIELD.name())
            .add("name", NAME.name())
            .add("value", "public.listing.hearing-changes-saved")
            .add("operation", EQUALS.name())
            .build();

    @BeforeEach
    public void cleanTheDatabase() {
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanEventLogTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "subscription", "event_cache");
        setupUserAsSystemUser(SYSTEM_USER_ID);
        stubUserWithPermission(randomUUID().toString());
    }

    @Test
    public void shouldCreateSubscriptionThenRaiseEventsMatchingSubscriptionThenQueryThoseEvents() throws Exception {
        final UUID subscriptionId = randomUUID();
        createSubscription(subscriptionId);
        generateSomeRandomMatchingAndNonMatchingEvents();
        assertThatMatchingEventsAreSearchableBySubscription(subscriptionId);

        removeSubscription(subscriptionId);

        assertSubscriptionIsRemoved(subscriptionId);
    }

    @Test
    public void shouldGetEventsForEventNameFilter() {
        final UUID streamId = randomUUID();
        final UUID subscriptionId = randomUUID();
        getEventsForEventNameFilter(subscriptionId, streamId);
    }


    @Test
    public void shouldGetEventsForStreamIdFilter() {
        final UUID subscriptionId = randomUUID();
        final UUID stream_id = randomUUID();

        final JsonObject streamIdFilter = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", STREAM_ID.name())
                .add("value", stream_id.toString())
                .add("operation", EQUALS.name())
                .build();

        createSubscription(subscriptionId, streamIdFilter);

        insertMatchingPublicEvents(stream_id, "streamIdFilter");
        insertMatchingPublicEvents(stream_id, "streamIdFilter");
        insertMatchingPublicEvents(randomUUID(), "streamIdFilter");
        insertMatchingPublicEvents(randomUUID(), "streamIdFilter");
        insertMatchingPublicEvents(randomUUID(), "streamIdFilter");

        final String url = getBaseUri() + format(EVENTS_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(uk.gov.justice.services.common.http.HeaderConstants.USER_ID, USER_UUID)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(2)),
                                withJsonPath("$.events[0]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[0].payload", is("streamIdFilter")),
                                withJsonPath("$.events[1]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[1].payload", is("streamIdFilter"))
                        )));
    }

    @Test
    public void shouldGetEventsForUserIdFilter() {
        final UUID subscriptionId = randomUUID();

        final JsonObject userIdFilter = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", USER_ID.name())
                .add("value", USER_UUID.toString())
                .add("operation", EQUALS.name())
                .build();

        createSubscription(subscriptionId, userIdFilter);

        generateSomeRandomMatchingAndNonMatchingEvents();

        assertThatMatchingEventsAreSearchableBySubscription(subscriptionId);
    }

    @Test
    public void shouldGetEventsForUserIdFilterUsingSpecialUserFilterType() {
        final UUID subscriptionId = randomUUID();

        final JsonObject userIdFilter = Json.createObjectBuilder()
                .add("type", USER_ID.name())
                .build();

        createSubscription(subscriptionId, userIdFilter);

        generateSomeRandomMatchingAndNonMatchingEvents();

        assertThatMatchingEventsAreSearchableBySubscription(subscriptionId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldGetEventsForComplexFilter() {
        final UUID subscriptionId = randomUUID();
        final UUID stream_id = randomUUID();

        final String filterEventName = "public.listing.hearing-changes-saved";
        final String payload = "eventNameFilter";

        final JsonObject nestedFilter = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(USER_ID_FILTER)
                        .add(STREAM_ID_FILTER_1)
                        .build())
                .build();

        final JsonObject complexFilter = Json.createObjectBuilder()
                .add("type", FilterType.AND.name())
                .add("value", Json.createArrayBuilder()
                        .add(NAME_FILTER)
                        .add(nestedFilter)
                        .build())
                .build();
        createSubscription(subscriptionId, complexFilter);

        // Matching events
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID, payload, filterEventName);
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID, payload, filterEventName);
        insertMatchingPublicEvents(randomUUID(), payload, filterEventName, COMPLEX_FILTER_USER_ID);

        // Non matching events
        insertMatchingPublicEvents(stream_id, payload, "public.event.test-notification");
        insertMatchingPublicEvents(stream_id, payload, "public.event.test-notification");

        final String url = getBaseUri() + format(EVENTS_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(uk.gov.justice.services.common.http.HeaderConstants.USER_ID, USER_UUID)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(3)),
                                withJsonPath("$.events[0]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[0].payload", is(payload)),
                                withJsonPath("$.events[1]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[1].payload", is(payload)),
                                withJsonPath("$.events[2]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[2].payload", is(payload))
                        )));
    }


    @Test
    public void shouldUpdateStreamIdFilterAndGetEvents() {
        final UUID subscriptionId = randomUUID();
        final UUID stream_id = randomUUID();
        final UUID stream_id2 = randomUUID();

        final JsonObject streamIdFilter = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", STREAM_ID.name())
                .add("value", stream_id.toString())
                .add("operation", EQUALS.name())
                .build();

        createSubscription(subscriptionId, streamIdFilter);

        insertMatchingPublicEvents(stream_id, "streamIdFilter", "public.listing.hearing-changes-saved");
        insertMatchingPublicEvents(stream_id, "streamIdFilter", "public.listing.hearing-changes-saved");
        insertMatchingPublicEvents(stream_id2, "streamIdFilter2", "public.listing.hearing-changes-saved");
        insertadditionalMatchingPublicEvents(randomUUID(), "streamIdFilter");
        insertadditionalMatchingPublicEvents(randomUUID(), "streamIdFilter");


        final String url = getBaseUri() + format(EVENTS_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(uk.gov.justice.services.common.http.HeaderConstants.USER_ID, USER_UUID)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(2)),
                                withJsonPath("$.events[0]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[0].payload", is("streamIdFilter")),
                                withJsonPath("$.events[1]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[1].payload", is("streamIdFilter"))
                        )));

        final JsonObject streamIdFilter2 = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", NAME.name())
                .add("value", "public.listing.hearing-changes-saved")
                .add("operation", EQUALS.name())
                .build();

        createSubscription(subscriptionId, streamIdFilter2);

        final List<String> streamIdList = asList(stream_id.toString(), stream_id2.toString());
        final List<String> payloads = asList("streamIdFilter", "streamIdFilter2");

        poll(requestParams)
                .timeout(20L, SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(3)),
                                withJsonPath("$.events[0]._metadata.stream.id", isIn(streamIdList)),
                                withJsonPath("$.events[0].payload", isIn(payloads)),
                                withJsonPath("$.events[0]._metadata.name", is("public.listing.hearing-changes-saved")),
                                withJsonPath("$.events[1]._metadata.stream.id", isIn(streamIdList)),
                                withJsonPath("$.events[1].payload", isIn(payloads)),
                                withJsonPath("$.events[1]._metadata.name", is("public.listing.hearing-changes-saved")),
                                withJsonPath("$.events[2]._metadata.stream.id", isIn(streamIdList)),
                                withJsonPath("$.events[2].payload", isIn(payloads)),
                                withJsonPath("$.events[2]._metadata.name", is("public.listing.hearing-changes-saved"))
                        )));
    }

    private void getEventsForEventNameFilter(UUID subscriptionId, UUID streamId) {

        final String filterEventName = "public.prosecutioncasefile.events.validation-completed";
        final String payload = "eventNameFilter";
        final JsonObject eventNameFilter = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", NAME.name())
                .add("value", filterEventName)
                .add("operation", EQUALS.name())
                .build();

        createSubscription(subscriptionId, eventNameFilter);

        insertMatchingPublicEvents(streamId, payload, filterEventName);
        insertMatchingPublicEvents(streamId, payload, filterEventName);
        insertMatchingPublicEvents(streamId, payload, filterEventName);

        // Non matching events
        insertMatchingPublicEvents(streamId, payload, "public.listing.hearing-changes-saved");
        insertMatchingPublicEvents(streamId, payload, "public.listing.hearing-changes-saved");

        final String url = getBaseUri() + format(EVENTS_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(uk.gov.justice.services.common.http.HeaderConstants.USER_ID, USER_UUID)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(3)),
                                withJsonPath("$.events[0]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[0].payload", is(payload)),
                                withJsonPath("$.events[1]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[1].payload", is(payload))
                        )));
    }

    @Test
    public void shouldUpdateEventnameFilterToStreamIdFilterAndGetEvents() {

        final UUID subscriptionId = randomUUID();
        final UUID stream_id = randomUUID();
        final UUID stream_id2 = randomUUID();

        getEventsForEventNameFilter(subscriptionId, stream_id);

        final String filterEventName = "public.listing.hearing-changes-saved";
        final String payload = "eventNameFilter";


        insertMatchingPublicEvents(stream_id2, payload, filterEventName);

        final String url = getBaseUri() + format(EVENTS_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(uk.gov.justice.services.common.http.HeaderConstants.USER_ID, USER_UUID)
                .build();

        final JsonObject streamIdFilter = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", STREAM_ID.name())
                .add("value", stream_id.toString())
                .add("operation", EQUALS.name())
                .build();

        createSubscription(subscriptionId, streamIdFilter);


        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(5)),
                                withJsonPath("$.events[0]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[0].payload", is(payload)),
                                withJsonPath("$.events[1]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[1].payload", is(payload)),
                                withJsonPath("$.events[2]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[2].payload", is(payload)),
                                withJsonPath("$.events[3]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[3].payload", is(payload)),
                                withJsonPath("$.events[4]._metadata.stream.id", is(stream_id.toString())),
                                withJsonPath("$.events[4].payload", is(payload))
                        )));

    }

    @Test
    public void shouldUpdateComplexFilterAndGetEvents() {
        final UUID subscriptionId = randomUUID();
        final UUID stream_id = randomUUID();

        final String filterEventName = "public.listing.hearing-changes-saved";
        final String payload = "eventNameFilter";

        final JsonObject nestedFilter = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(USER_ID_FILTER)
                        .add(STREAM_ID_FILTER_1)
                        .build())
                .build();

        final JsonObject complexFilter = Json.createObjectBuilder()
                .add("type", FilterType.AND.name())
                .add("value", Json.createArrayBuilder()
                        .add(NAME_FILTER)
                        .add(nestedFilter)
                        .build())
                .build();
        createSubscription(subscriptionId, complexFilter);

        // Matching events
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID, payload, filterEventName);
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID, payload, filterEventName);
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID, payload, filterEventName, COMPLEX_FILTER_USER_ID);
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID_2, payload, filterEventName, randomUUID());
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID_2, payload, filterEventName);
        insertMatchingPublicEvents(COMPLEX_FILTER_STREAM_ID_2, payload, filterEventName);

        // Non matching events
        insertMatchingPublicEvents(stream_id, payload, "public.event.test-notification");
        insertMatchingPublicEvents(stream_id, payload, "public.event.test-notification");

        final String url = getBaseUri() + format(EVENTS_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(uk.gov.justice.services.common.http.HeaderConstants.USER_ID, USER_UUID)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(3)),
                                withJsonPath("$.events[0]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[0].payload", is(payload)),
                                withJsonPath("$.events[1]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[1].payload", is(payload)),
                                withJsonPath("$.events[2]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[2].payload", is(payload))
                        )));

        final JsonObject nestedFilter_1 = Json.createObjectBuilder()
                .add("type", FilterType.OR.name())
                .add("value", Json.createArrayBuilder()
                        .add(USER_ID_FILTER)
                        .add(STREAM_ID_FILTER_2)
                        .build())
                .build();

        final JsonObject complexFilter_1 = Json.createObjectBuilder()
                .add("type", FilterType.AND.name())
                .add("value", Json.createArrayBuilder()
                        .add(NAME_FILTER)
                        .add(nestedFilter_1)
                        .build())
                .build();

        createSubscription(subscriptionId, complexFilter_1);

        List<String> streamIdList = asList(COMPLEX_FILTER_STREAM_ID_2.toString(), COMPLEX_FILTER_STREAM_ID.toString());

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(4)),
                                withJsonPath("$.events[0]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[0]._metadata.stream.id", isIn(streamIdList)),
                                withJsonPath("$.events[0].payload", is(payload)),
                                withJsonPath("$.events[1]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[1]._metadata.stream.id", isIn(streamIdList)),
                                withJsonPath("$.events[1].payload", is(payload)),
                                withJsonPath("$.events[2]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[2]._metadata.stream.id", isIn(streamIdList)),
                                withJsonPath("$.events[2].payload", is(payload)),
                                withJsonPath("$.events[3]._metadata.name", is(filterEventName)),
                                withJsonPath("$.events[3]._metadata.stream.id", isIn(streamIdList)),
                                withJsonPath("$.events[3].payload", is(payload))
                        )));
    }


    private void insertMatchingPublicEvents(final UUID stream_id, final String payloadValue) {
        final JsonObject payload = createObjectBuilder().add("payload", payloadValue).build();
        final String eventName = "public.listing.hearing-changes-saved";
        final JsonEnvelope publicEvent = createPublicEvent(stream_id, eventName, randomUUID(), payload);

        publicMessageProducerClient.sendMessage(eventName, publicEvent);
    }

    private void insertadditionalMatchingPublicEvents(final UUID stream_id, final String payloadValue) {
        final JsonObject payload = createObjectBuilder().add("payload", payloadValue).build();
        final String eventName = "public.directionsmanagement.case-direction-updated";
        final JsonEnvelope publicEvent = createPublicEvent(stream_id, eventName, randomUUID(), payload);

        publicMessageProducerClient.sendMessage(eventName, publicEvent);
    }

    private void insertMatchingPublicEvents(final UUID stream_id, final String payloadValue, final String eventName) {
        final JsonObject payload = createObjectBuilder().add("payload", payloadValue).build();
        final JsonEnvelope publicEvent = createPublicEvent(stream_id, eventName, randomUUID(), payload);

        publicMessageProducerClient.sendMessage(eventName, publicEvent);
    }

    private void insertMatchingPublicEvents(final UUID stream_id, final String payloadValue, final String eventName, final UUID userId) {
        final JsonObject payload = createObjectBuilder().add("payload", payloadValue).build();
        final JsonEnvelope publicEvent = createPublicEvent(stream_id, eventName, userId, payload);

        publicMessageProducerClient.sendMessage(eventName, publicEvent);
    }

    private void createSubscription(final UUID subscriptionId) {
        createSubscription(subscriptionId, AN_EMPTY_STRING, "application/vnd.notification.subscribe-by-user-id+json");
    }

    private void createSubscription(final UUID subscriptionId, final JsonObject filter) {
        createSubscription(subscriptionId, filter.toString(), "application/vnd.notification.filter+json");
    }

    private void createSubscription(final UUID subscriptionId, final String filter, String mediaType) {
        final String subscribeCommandUrl = getBaseUri() + format(SUBSCRIPTION_COMMAND_API_PATH, subscriptionId);

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, USER_UUID);

        final Response response = restClient.postCommand(subscribeCommandUrl, mediaType, filter, headers);
        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
    }

    private void generateSomeRandomMatchingAndNonMatchingEvents() {
        for (int i = 0; i < NUMBER_OF_NON_MATCHING_EVENTS; i++) {
            final JsonObject payload = createObjectBuilder().add(NON_MATCHING_EVENTS_PAYLOAD_KEY, randomUUID().toString()).build();
            final JsonEnvelope command = createCommand(randomUUID(), payload);
            publicMessageProducerClient.sendMessage(NOTIFICATION_ADDED_COMMAND_NAME, command);
        }

        for (int i = 0; i < NUMBER_OF_MATCHING_EVENTS; i++) {
            final String randomPayloadValue = randomUUID().toString();
            final JsonObject payload = createObjectBuilder().add(MATCHING_EVENTS_PAYLOAD_KEY, randomPayloadValue).build();
            final JsonEnvelope command = createCommand(USER_UUID, payload);
            publicMessageProducerClient.sendMessage(NOTIFICATION_ADDED_COMMAND_NAME, command);
            expectedPayloadValues.add(randomPayloadValue);
        }
    }

    private void assertThatMatchingEventsAreSearchableBySubscription(final UUID subscriptionId) {
        final String url = getBaseUri() + format(EVENTS_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(HeaderConstants.USER_ID, USER_UUID)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(NUMBER_OF_MATCHING_EVENTS)),
                                withJsonPath("$.events[*].jsonKeyB", containsInAnyOrder(expectedPayloadValues.toArray())),
                                withJsonPath("$.events[*]._metadata.context.user", everyItem(equalTo(USER_UUID.toString())))
                        )));

    }

    private void removeSubscription(final UUID subsriptionId) {
        final String subscribeCommandUrl = getBaseUri() + format(SUBSCRIPTION_COMMAND_API_PATH, subsriptionId);

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, SYSTEM_USER_ID);

        final Response response = restClient.postCommand(
                subscribeCommandUrl, "application/vnd.notification.unsubscribe+json",
                AN_EMPTY_STRING, headers);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
    }

    private void assertSubscriptionIsRemoved(final UUID subscriptionId) {
        final String url = getBaseUri() + format(SUBSCRIPTION_QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.subscription+json";
        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(HeaderConstants.USER_ID, SYSTEM_USER_ID)
                .build();
        poll(requestParams).until(status().is(NOT_FOUND));
    }

    private JsonEnvelope createCommand(final UUID userId, final JsonObject payload) {
        return envelopeFrom(
                metadataWithRandomUUID(NOTIFICATION_ADDED_COMMAND_NAME)
                        .withClientCorrelationId(randomUUID().toString())
                        .withSessionId(randomUUID().toString())
                        .withUserId(userId.toString())
                        .withStreamId(randomUUID())
                        .build(), payload);
    }

    private JsonEnvelope createPublicEvent(final UUID streamId, final String eventName, final UUID userId, final JsonObject payload) {
        return envelopeFrom(
                metadataWithRandomUUID(eventName)
                        .withClientCorrelationId(randomUUID().toString())
                        .withSessionId(randomUUID().toString())
                        .withUserId(userId.toString())
                        .withStreamId(streamId)
                        .build(), payload);
    }

}
