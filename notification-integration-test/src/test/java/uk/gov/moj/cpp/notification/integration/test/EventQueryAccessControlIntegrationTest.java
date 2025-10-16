package uk.gov.moj.cpp.notification.integration.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.setupUserAsSystemUser;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.stubUserWithNoPermission;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.stubUserWithPermission;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.EventJdbcInserter;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.SubscriptionJdbcInserter;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventQueryAccessControlIntegrationTest {

    private static final String CONTEXT_NAME = "notification";
    private static final String QUERY_EVENTS_BASE_URL = "/notification-query-api/query/api/rest/notifications/subscriptions/%s/events";

    private final SubscriptionJdbcInserter subscriptionJdbcInserter = new SubscriptionJdbcInserter();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final EventJdbcInserter eventJdbcInserter = new EventJdbcInserter();

    @BeforeEach
    public void cleanTheDatabase() {

        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "event_cache", "subscription");
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void shouldGetAListOfEventsBySubscriptionId() throws Exception {

        final UUID subscriptionId_1 = randomUUID();
        final UUID subscriptionId_2 = randomUUID();
        final UUID userId = randomUUID();
        stubUserWithPermission(userId.toString());

        insertSomeSubscriptionsAndEventsIntoTheDatabase(
                subscriptionId_1,
                subscriptionId_2,
                userId);

        final String url = getBaseUri() + format(QUERY_EVENTS_BASE_URL, subscriptionId_1);
        final String mediaType = "application/vnd.notification.events+json";

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(USER_ID, userId)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.events", hasSize(2)),
                                withJsonPath("$.events[0].userId", is("jsonUserId_2")),
                                withJsonPath("$.events[1].userId", is("jsonUserId_1"))
                        )));
    }

    @SuppressWarnings("squid:S2699")
    @Test
    public void shouldFailRequestOnAccessControlIfUserIdIsDifferentInTheHeader() throws Exception {

        final UUID subscriptionId_1 = randomUUID();
        final UUID subscriptionId_2 = randomUUID();
        final UUID userId = randomUUID();

        setupUserAsSystemUser(userId.toString());
        stubUserWithNoPermission(userId.toString());

        insertSomeSubscriptionsAndEventsIntoTheDatabase(
                subscriptionId_1,
                subscriptionId_2,
                userId);

        final String url = getBaseUri() + format(QUERY_EVENTS_BASE_URL, subscriptionId_1);
        final String mediaType = "application/vnd.notification.events+json";


        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(USER_ID, randomUUID())
                .build();

        poll(requestParams)
                .until(
                        status().is(FORBIDDEN),
                        payload().isJson(allOf(
                                withJsonPath("$.error", startsWith("Access Control failed for json envelope")),
                                withJsonPath("$.error", endsWith("Reason: Rules failed to match"))
                        )));
    }

    @SuppressWarnings("Duplicates")
    private void insertSomeSubscriptionsAndEventsIntoTheDatabase(
            final UUID subscriptionId_1,
            final UUID subscriptionId_2,
            final UUID userId) {

        final String json_1 = "{\"userId\": \"jsonUserId_1\"}";
        final String json_2 = "{\"userId\": \"jsonUserId_2\"}";

        subscriptionJdbcInserter.insertUserIdSubscription(subscriptionId_1, userId);
        eventJdbcInserter.insertUserIdEvent(userId, json_1);
        eventJdbcInserter.insertUserIdEvent(userId, json_2);

        final String json_3 = "{\"sessionId\": \"jsonSessionId_1\"}";
        final String json_4 = "{\"sessionId\": \"jsonSessionId_2\"}";

        subscriptionJdbcInserter.insertUserIdSubscription(subscriptionId_2, userId);
        eventJdbcInserter.insertUserIdEvent(randomUUID(), json_3);
        eventJdbcInserter.insertUserIdEvent(randomUUID(), json_4);
    }

}
