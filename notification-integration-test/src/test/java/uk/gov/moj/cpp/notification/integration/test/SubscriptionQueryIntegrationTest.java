package uk.gov.moj.cpp.notification.integration.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.setupUserAsSystemUser;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.SubscriptionJdbcInserter;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SubscriptionQueryIntegrationTest {

    private static final String CONTEXT_NAME = "notification";
    private static final String QUERY_API_PATH = "/notification-query-api/query/api/rest/notifications/subscriptions/%s";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final SubscriptionJdbcInserter subscriptionJdbcInserter = new SubscriptionJdbcInserter();
    private final String SYSTEM_USER_ID = randomUUID().toString();

    @BeforeEach
    public void cleanTheDatabase() {

        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanEventLogTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "subscription", "event_cache");
        setupUserAsSystemUser(SYSTEM_USER_ID);
    }

    @Test
    public void shouldLookUpASubscriptionAsASystemUserById() throws Exception {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();
        final String url = getBaseUri() + format(QUERY_API_PATH, subscriptionId);
        final String mediaType = "application/vnd.notification.subscription+json";

        subscriptionJdbcInserter.insertUserIdSubscription(subscriptionId, userId);

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(USER_ID, SYSTEM_USER_ID)
                .build();

        poll(requestParams)
                .until(
                        status().is(OK),
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.ownerId", is(userId.toString())),
                                        withJsonPath("$.subscriptionId", is(subscriptionId.toString())
                                        ))));
    }
}
