package uk.gov.moj.cpp.notification.integration.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.stubUserWithNoPermission;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.stubUserWithPermission;
import static uk.gov.moj.cpp.notification.integration.test.utils.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.EventJdbcInserter;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.SubscriptionJdbcInserter;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventQueryIntegrationTest {

    private static final String CONTEXT_NAME = "notification";
    private static final String QUERY_EVENTS_PATH = "/notification-query-api/query/api/rest/notifications/subscriptions/%s/events";
    private static final String QUERY_EVENTS_PATH_WITH_CLIENT_CORRELATION_ID = QUERY_EVENTS_PATH + "?clientCorrelationId=%s";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final EventJdbcInserter eventJdbcInserter = new EventJdbcInserter();
    private final SubscriptionJdbcInserter subscriptionJdbcInserter = new SubscriptionJdbcInserter();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private final static String UNKNOWN_EVENT_ATTRIBUTE_NAME = randomAlphanumeric(6);
    private final static String KNOWN_EVENT_ATTRIBUTE_NAME = randomAlphanumeric(6);
    private final static String KNOWN_CORRELATION_ID = randomUUID().toString();
    private final static UUID KNOWN_EVENT_ID = randomUUID();

    @BeforeEach
    public void cleanTheDatabase() {
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "event_cache", "subscription");
    }

    @Test
    public void shouldGetAListOfEventsBySubscriptionId() {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();
        stubUserWithPermission(userId.toString());

        createUserSubscriptionAndEvents(subscriptionId, userId, true);

        poll(getEventsFor(subscriptionId, userId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(2)),
                                withJsonPath("$.events[0]." + UNKNOWN_EVENT_ATTRIBUTE_NAME, notNullValue()),
                                withJsonPath("$.events[1]." + KNOWN_EVENT_ATTRIBUTE_NAME, notNullValue())
                        )));
    }

    @Test
    public void shouldGetAListOfEventsMetadataBySubscriptionId() {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();
        stubUserWithPermission(userId.toString());

        createUserSubscriptionAndEventsWithMetadata(subscriptionId, userId, true);

        poll(getEventsForMetadata(subscriptionId, userId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(5)),
                                withJsonPath("$.events[0]" , hasKey("_metadata")),
                                withJsonPath("$.events[0]." + "_metadata", notNullValue()),
                                withJsonPath("$.events[0]" , not(hasKey("caseId")))
                        )));
    }

    @Test
    public void shouldGetAListOfEventsBySubscriptionIdAndClientCorrelationId() {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();
        stubUserWithPermission(userId.toString());

        createUserSubscriptionAndEvents(subscriptionId, userId, true);

        poll(getEventsFor(subscriptionId, userId, KNOWN_CORRELATION_ID))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(1)),
                                withJsonPath("$.events[0]." + KNOWN_EVENT_ATTRIBUTE_NAME, notNullValue())
                        )));
    }

    @Test
    public void shouldGetAnEmptyListOfEventsBySubscriptionIdWhenNoEventsPresentForValidSubscription() {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();

        createUserSubscriptionAndEvents(subscriptionId, userId, false);

        poll(getEventsFor(subscriptionId, userId))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", empty())
                        )));
    }

    @Test
    public void shouldForbiddenExceptionWhenSubscriptionNotValidForUser() {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();
        stubUserWithNoPermission(userId.toString());

        poll(getEventsFor(subscriptionId, userId))
                .until(
                        status().is(FORBIDDEN));
    }

    private void createUserSubscriptionAndEvents(final UUID subscriptionId, final UUID userId, final boolean addEvents) {
        subscriptionJdbcInserter.insertUserIdSubscription(subscriptionId, userId);
        if (addEvents) {
            eventJdbcInserter.insertEventIdUserIdAndCorrelationIdEvent(KNOWN_EVENT_ID, userId, KNOWN_CORRELATION_ID, getJsonObject(KNOWN_EVENT_ATTRIBUTE_NAME, randomAlphanumeric(10)).toString());
            eventJdbcInserter.insertUserIdEvent(userId, getJsonObject(UNKNOWN_EVENT_ATTRIBUTE_NAME, randomAlphanumeric(10)).toString());
        }
    }

    private void createUserSubscriptionAndEventsWithMetadata(final UUID subscriptionId, final UUID userId, final boolean addEvents) {
        subscriptionJdbcInserter.insertUserIdSubscription(subscriptionId, userId);
        if (addEvents) {

            final JsonObject payload = stringToJsonObjectConverter.convert(getPayload("caseOwnershipTransferredToCMS.json"));

            eventJdbcInserter.insertEventIdUserIdAndCorrelationIdEvent(randomUUID(), userId, KNOWN_CORRELATION_ID, payload.toString());
            eventJdbcInserter.insertEventIdUserIdAndCorrelationIdEvent(randomUUID(), userId, KNOWN_CORRELATION_ID, payload.toString());
            eventJdbcInserter.insertEventIdUserIdAndCorrelationIdEvent(randomUUID(), userId, KNOWN_CORRELATION_ID, payload.toString());
            eventJdbcInserter.insertEventIdUserIdAndCorrelationIdEvent(randomUUID(), userId, KNOWN_CORRELATION_ID, payload.toString());

            eventJdbcInserter.insertUserIdEvent(userId, payload.toString());
        }
    }

    private JsonObject getJsonObject(final String attributeName, final String attributeValue) {
        return Json.createObjectBuilder().add(attributeName, attributeValue).build();
    }

    private RequestParams getEventsFor(final UUID subscriptionId, final UUID userId) {
        final String url = getBaseUri() + format(QUERY_EVENTS_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, userId)
                .build();
    }

    private RequestParams getEventsForMetadata(final UUID subscriptionId, final UUID userId) {
        final String url = getBaseUri() + format(QUERY_EVENTS_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.events.metadata+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, userId)
                .build();
    }

    private RequestParams getEventsFor(final UUID subscriptionId, final UUID userId, final String clientCorrelationId) {
        final String url = getBaseUri() + format(QUERY_EVENTS_PATH_WITH_CLIENT_CORRELATION_ID, subscriptionId, clientCorrelationId);
        final String mediaType = "application/vnd.notification.events+json";

        return requestParams(url, mediaType)
                .withHeader(USER_ID, userId)
                .build();
    }
}
