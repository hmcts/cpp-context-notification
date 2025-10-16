package uk.gov.moj.cpp.notification.integration.test;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.jmx.api.domain.SystemCommandStatus;
import uk.gov.justice.services.jmx.api.mbean.SystemCommanderMBean;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.SubscriptionJdbcInserter;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.SubscriptionJdbcPoller;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_COMPLETE;
import static uk.gov.justice.services.jmx.api.mbean.CommandRunMode.FORCED;
import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.setupUserAsSystemUser;
import static uk.gov.moj.cpp.notification.system.management.commands.subscriptions.clean.CleanSubscriptionsCommand.CLEAN_SUBSCRIPTIONS;


public class SubscriptionExpiredIntegrationTest {

    private static final String CONTEXT_NAME = "notification";
    private static final String SUBSCRIPTIONS_COMMAND_URI = "/notification-command-api/command/api/rest/notification/subscriptions/%s";
    private static final String QUERY_API_PATH = "/notification-query-api/query/api/rest/notifications/subscriptions/expired-subscriptions";
    private static final int MILLI_SECONDS_IN_8_HOURS = 28800000;
    private static final int TOTAL_SUBSCRIPTION_COUNT = 10;
    private static final String AN_EMPTY_STRING = "";
    private static final String HOST = getHost();
    private static final int PORT = 9990;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private final RestClient restClient = new RestClient();
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final SubscriptionJdbcInserter subscriptionJdbcInserter = new SubscriptionJdbcInserter();
    private final String SYSTEM_USER_ID = "0dac8d08-c5e4-427c-81e5-870b4c57984a";
    private final SubscriptionJdbcPoller subscriptionJdbcPoller = new SubscriptionJdbcPoller();

    private final TestSystemCommanderClientFactory systemCommanderClientFactory = new TestSystemCommanderClientFactory();

    private final Poller poller = new Poller();

    private static final UUID NULL_COMMAND_RUNTIME_ID = null;
    private static final String NULL_COMMAND_RUNTIME_STRING = null;

    @BeforeEach
    public void cleanTheDatabase() {

        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanEventLogTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "subscription", "event_cache");
        setupUserAsSystemUser(SYSTEM_USER_ID);
    }

    @Test
    public void shouldBeAbleToFindExpiredSubscriptionsAsSystemUser() {

        final String url = getBaseUri() + QUERY_API_PATH;
        final String mediaType = "application/vnd.notification.expired.subscriptions+json";
        final int numberOfUnexpiredSubscriptions = RandomGenerator.integer(TOTAL_SUBSCRIPTION_COUNT).next();
        final int numberOfExpiredSubscriptions = TOTAL_SUBSCRIPTION_COUNT - numberOfUnexpiredSubscriptions;
        final int expiryDurationMillis = MILLI_SECONDS_IN_8_HOURS;

        subscriptionJdbcInserter.insertExpiredSubscriptions(numberOfExpiredSubscriptions, expiryDurationMillis);
        subscriptionJdbcInserter.insertSubscriptions(numberOfUnexpiredSubscriptions, expiryDurationMillis);

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(USER_ID, SYSTEM_USER_ID)
                .build();

        poll(requestParams).with().pollDelay(2, TimeUnit.SECONDS)
                .until(
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.expiredSubscriptions.length()", is(numberOfExpiredSubscriptions))
                                )));
    }

    @Test
    public void shouldCleanExpiredSubscriptions() throws Exception {

        final String url = getBaseUri() + QUERY_API_PATH;
        final String mediaType = "application/vnd.notification.expired.subscriptions+json";

        final UUID userId = randomUUID();
        final UUID subscriptionId_1 = randomUUID();
        final UUID subscriptionId_2 = randomUUID();

        createSubscription(subscriptionId_1, userId.toString());
        createSubscription(subscriptionId_2, userId.toString());

        final Optional<Subscription> subscription_1 = subscriptionJdbcPoller.pollUntilFound(subscriptionId_1);
        final Optional<Subscription> subscription_2 = subscriptionJdbcPoller.pollUntilFound(subscriptionId_2);

        assertThat(subscription_1.isPresent(), is(true));
        assertThat(subscription_2.isPresent(), is(true));

        subscriptionJdbcInserter.rollbackModificationDate(subscriptionId_1, 8);
        subscriptionJdbcInserter.rollbackModificationDate(subscriptionId_2, 8);

        final RequestParams requestParams = requestParams(url, mediaType)
                .withHeader(USER_ID, SYSTEM_USER_ID)
                .build();

        poll(requestParams).with().pollDelay(2, TimeUnit.SECONDS)
                .until(
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.expiredSubscriptions.length()", is(2))
                                )));

        runCleanSubscriptions();

        poll(requestParams).with().pollDelay(2, TimeUnit.SECONDS)
                .until(
                        payload()
                                .isJson(allOf(
                                        withJsonPath("$.expiredSubscriptions.length()", is(0))
                                )));

    }

    private void createSubscription(final UUID subscriptionId, final String userId) {
        final String subscribeCommandUrl = getBaseUri() + format(SUBSCRIPTIONS_COMMAND_URI, subscriptionId.toString());

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, userId);

        final Response response = restClient.postCommand(
                subscribeCommandUrl, "application/vnd.notification.subscribe-by-user-id+json",
                AN_EMPTY_STRING, headers);

        assertThat(response.getStatus(), CoreMatchers.is(ACCEPTED.getStatusCode()));
    }

    private void runCleanSubscriptions() {

        final JmxParameters jmxParameters = jmxParameters()
                .withContextName(CONTEXT_NAME)
                .withHost(HOST)
                .withPort(PORT)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .build();

        try (final SystemCommanderClient systemCommanderClient = systemCommanderClientFactory.create(jmxParameters)) {
            final SystemCommanderMBean systemCommanderMBean = systemCommanderClient.getRemote(CONTEXT_NAME);
            final UUID commandId = systemCommanderMBean.call(CLEAN_SUBSCRIPTIONS, NULL_COMMAND_RUNTIME_ID, NULL_COMMAND_RUNTIME_STRING, FORCED.isGuarded());

            final Optional<SystemCommandStatus> systemCommandStatus = poller.pollUntilFound(() -> {
                final SystemCommandStatus commandStatus = systemCommanderMBean.getCommandStatus(commandId);

                if (commandStatus.getCommandState() == COMMAND_COMPLETE) {
                    return of(commandStatus);
                }

                return empty();
            });

            if (systemCommandStatus.isPresent()) {
                assertThat(systemCommandStatus.get().getCommandState(), is(COMMAND_COMPLETE));
            } else {
                fail(format("Command %s failed to complete", CLEAN_SUBSCRIPTIONS));
            }
        }
    }
}
