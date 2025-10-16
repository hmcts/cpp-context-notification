package uk.gov.moj.cpp.notification.common.accesscontrol;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.notification.query.view.NotificationQueryView;

import java.util.List;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionProviderTest {

    private static final List<String> SYSTEM_USER_GROUPS = singletonList("System Users");

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Mock
    private NotificationQueryView notificationQueryView;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private SubscriptionProvider subscriptionProvider;

    @Test
    public void shouldReturnTrueIfTheOwnerIdInTheSubscriptionMatchesThePassedUserId() throws Exception {
        final String subscriptionId = randomUUID().toString();
        final String userId = randomUUID().toString();

        final Action action = new Action(envelope()
                .with(metadataWithRandomUUID("notification.get-subscription")
                        .withUserId(userId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build());

        final JsonEnvelope response = envelope()
                .with(metadataWithRandomUUID("notification.subscription"))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .withPayloadOf(userId, "ownerId")
                .build();

        when(notificationQueryView.getSubscription(argThat(
                jsonEnvelope(
                        metadata().withName("notification.get-subscription"),
                        payload().isJson(
                                withJsonPath("$.subscriptionId", equalTo(subscriptionId)))))))
                .thenReturn(response);

        assertThat(subscriptionProvider.isSubscriptionOwner(action), is(true));
    }

    @Test
    public void shouldReturnFalseIfTheOwnerIdInTheSubscriptionDoesNotMatchThePassedUserId() throws Exception {

        final String subscriptionId = randomUUID().toString();
        final String subscriptionsUserId = randomUUID().toString();
        final String usersUserId = randomUUID().toString();

        final Action action = new Action(envelope()
                .with(metadataWithRandomUUID("notification.get-subscription")
                        .withUserId(usersUserId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build());

        final JsonEnvelope response = envelope()
                .with(metadataWithRandomUUID("notification.subscription"))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .withPayloadOf(subscriptionsUserId, "ownerId")
                .build();

        when(notificationQueryView.getSubscription(argThat(
                jsonEnvelope(
                        metadata().withName("notification.get-subscription"),
                        payload().isJson(
                                withJsonPath("$.subscriptionId", equalTo(subscriptionId)))))))
                .thenReturn(response);

        assertThat(subscriptionProvider.isSubscriptionOwner(action), is(false));
    }

    @Test
    public void shouldReturnFalseIfSubscriptionDoesNotExist() throws Exception {

        final String subscriptionId = randomUUID().toString();
        final String usersUserId = randomUUID().toString();

        final Action action = new Action(envelope()
                .with(metadataWithRandomUUID("notification.get-subscription")
                        .withUserId(usersUserId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build());

        final JsonEnvelope response = envelopeFrom(
                metadataWithRandomUUID("notification.subscription"),
                JsonValue.NULL);

        when(notificationQueryView.getSubscription(argThat(
                jsonEnvelope(
                        metadata().withName("notification.get-subscription"),
                        payload().isJson(
                                withJsonPath("$.subscriptionId", equalTo(subscriptionId)))))))
                .thenReturn(response);

        assertThat(subscriptionProvider.isSubscriptionOwner(action), is(false));
    }

    @Test
    public void shouldReturnFalseIfNoUserIdExistsInTheEnvelopeMetadata() throws Exception {

        final Action action = new Action(envelope()
                .with(metadataWithRandomUUID("notification.get-subscription"))
                .build());

        assertThat(subscriptionProvider.isSubscriptionOwner(action), is(false));
    }

    @Test
    public void shouldReturnFalseIfNoSubscriptionIdExistsInThePayload() throws Exception {

        final String userId = randomUUID().toString();

        final Action action = new Action(envelope()
                .with(metadataWithRandomUUID("notification.get-subscription")
                        .withUserId(userId))
                .build());

        assertThat(subscriptionProvider.isSubscriptionOwner(action), is(false));
    }
}
