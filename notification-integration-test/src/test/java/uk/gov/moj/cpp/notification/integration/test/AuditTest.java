package uk.gov.moj.cpp.notification.integration.test;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.notification.integration.test.dataaccess.WireMockStubUtils.setupUserAsSystemUser;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;

public class AuditTest {
    private static final String SUBSCRIBE_URI = "/notification-command-api/command/api/rest/notification/subscriptions/%s";
    private static final String QUERY_API_PATH = "/notification-query-api/query/api/rest/notifications/subscriptions/%s";

    private JmsMessageConsumerClient auditRecordedConsumer = newPrivateJmsMessageConsumerClientProvider("auditing")
            .withEventNames("audit.events.audit-recorded")
            .getMessageConsumerClient();

    private final RestClient restClient = new RestClient();

    @Test
    public void shouldAuditSubscriptionCreation() {
        final UUID userId = randomUUID();
        final UUID subscriptionId = randomUUID();

        subscribeUser(userId, subscriptionId);

        verifyAuditMessageFor(userId, subscriptionId, "notification.subscribe-by-user-id", "COMMAND_API");
    }

    @Test
    public void shouldAuditQueryForSubscription() throws Exception {
        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();

        subscribeUser(userId, subscriptionId);

        final UUID sysUserId = randomUUID();
        setupUserAsSystemUser(sysUserId.toString());

        verifyAuditMessageFor(userId, subscriptionId, "notification.subscribe-by-user-id", "COMMAND_API");

        final RequestParams requestParams = requestParams(getBaseUri() + format(QUERY_API_PATH, subscriptionId),
                "application/vnd.notification.subscription+json")
                .withHeader(USER_ID, sysUserId)
                .build();

        poll(requestParams).until(status().is(OK));

        verifyAuditMessageFor(sysUserId, subscriptionId, "notification.get-subscription", "QUERY_API");
    }

    private void subscribeUser(final UUID userId, final UUID subscriptionId) {
        final String uri = getBaseUri() + format(SUBSCRIBE_URI, subscriptionId);

        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putSingle(USER_ID, userId);

        restClient.postCommand(uri, "application/vnd.notification.subscribe-by-user-id+json", "", headers);
    }


    private void verifyAuditMessageFor(final UUID userId, final UUID subscriptionId, final String actionName, final String component) {
        final Poller poller = new Poller(10, 500);

        final Optional<String> message = poller.pollUntilFound(() -> {

            final Optional<String> optionalMessage = auditRecordedConsumer.retrieveMessage();
            return optionalMessage.filter(s -> s.contains(actionName));

        });

        assertThat(message.isPresent(), is(true));

        with(message.get())
                .assertThat("$.content", notNullValue())
                .assertThat("$.content.subscriptionId", is(subscriptionId.toString()))
                .assertThat("$.content._metadata.context.user", is(userId.toString()))
                .assertThat("$.content._metadata.name", is(actionName))
                .assertThat("$.component", is(component));
    }
}
