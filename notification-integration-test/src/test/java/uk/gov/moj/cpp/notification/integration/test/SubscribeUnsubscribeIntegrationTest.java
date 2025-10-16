package uk.gov.moj.cpp.notification.integration.test;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.setupUserAsSystemUser;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.common.FieldNames;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.SubscriptionJdbcFinder;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.SubscriptionJdbcPoller;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SubscribeUnsubscribeIntegrationTest {

    private static final String CONTEXT_NAME = "notification";
    private static final String SUBSCRIPTIONS_COMMAND_URI = "/notification-command-api/command/api/rest/notification/subscriptions/%s";

    private final SubscriptionJdbcPoller subscriptionJdbcPoller = new SubscriptionJdbcPoller();
    private final SubscriptionJdbcFinder subscriptionJdbcFinder = new SubscriptionJdbcFinder();

    private static final String AN_EMPTY_STRING = "";

    private final RestClient restClient = new RestClient();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
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
    public void shouldCreateASubscriptionAndQueryForExistingSubscription() throws Exception {

        final UUID userId = randomUUID();
        final UUID subscriptionId = randomUUID();

        createSubscription(subscriptionId, userId.toString());

        final Optional<Subscription> subscription = subscriptionJdbcPoller.pollUntilFound(subscriptionId);

        assertThat(subscription.isPresent(), is(true));

        assertThat(subscription.get().getOwnerId(), is(userId));
        assertThat(subscription.get().getId(), is(subscriptionId));
        final String filter = subscription.get().getFilter();

        with(filter)
                .assertThat("$.type", is(FIELD.name()))
                .assertThat("$.name", is(FieldNames.USER_ID.toString()))
                .assertThat("$.value", is(userId.toString()))
                .assertThat("$.operation", is(EQUALS.toString()));

        // Query for existing subscription
        final Optional<Subscription> subscriptionInDatabase = subscriptionJdbcFinder.findSubscription(subscriptionId);

        assertThat(subscriptionInDatabase.isPresent(), is(true));
        assertThat(subscriptionInDatabase.get().getOwnerId(), is(userId));
        assertThat(subscriptionInDatabase.get().getId(), is(subscriptionId));
        final String filterFromDatabase = subscriptionInDatabase.get().getFilter();

        with(filterFromDatabase)
                .assertThat("$.type", is("FIELD"))
                .assertThat("$.name", is(FieldNames.USER_ID.toString()))
                .assertThat("$.value", is(userId.toString()))
                .assertThat("$.operation", is("EQUALS"));
    }

    @Test
    public void shouldUnsubscribeSubscriptionAsASystemUser() throws Exception {

        final UUID userId = randomUUID();
        final UUID subscriptionId = randomUUID();
        createSubscription(subscriptionId, userId.toString());

        final String uri = getBaseUri() + format(SUBSCRIPTIONS_COMMAND_URI, subscriptionId);

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, SYSTEM_USER_ID);

        final Response response = restClient.postCommand(
                uri, "application/vnd.notification.unsubscribe+json",
                AN_EMPTY_STRING,
                headers);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));

        subscriptionJdbcPoller.pollUntilNotFound(subscriptionId);
    }

    private void createSubscription(final UUID subscriptionId, final String userId) {
        final String subscribeCommandUrl = getBaseUri() + format(SUBSCRIPTIONS_COMMAND_URI, subscriptionId.toString());

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(HeaderConstants.USER_ID, userId);

        final Response response = restClient.postCommand(
                subscribeCommandUrl, "application/vnd.notification.subscribe-by-user-id+json",
                AN_EMPTY_STRING, headers);

        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));
    }
}
