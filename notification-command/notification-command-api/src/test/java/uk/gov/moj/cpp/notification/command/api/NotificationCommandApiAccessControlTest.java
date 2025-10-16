package uk.gov.moj.cpp.notification.command.api;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.notification.common.accesscontrol.SubscriptionProvider;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationCommandApiAccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Mock
    private SubscriptionProvider subscriptionProvider;

    public NotificationCommandApiAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder()
                .put(UserAndGroupProvider.class, userAndGroupProvider)
                .put(SubscriptionProvider.class, subscriptionProvider)
                .build();
    }

    @Test
    public void shouldFailIfActionNameIsIncorrect() throws Exception {

        final UUID userId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("some.silly.name")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldFailSubscribeByUserIdIfUserIdIsMissing() throws Exception {

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withName("notification.subscribe-by-user-id")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldCheckUserIsLoggedInForSubscribeByUserId() throws Exception {

        final UUID userId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("notification.subscribe-by-user-id")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowToUnsubscribeIfUserIdIsMissing() throws Exception {

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withName("notification.unsubscribe")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldFailIfUserIsLoggedInButDoesNotOwnToUnSubscribeByUserId() throws Exception {

        final UUID userId = randomUUID();

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("notification.unsubscribe")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowSystemUserToUnSubscribeAnySubscription() throws Exception {

        final UUID userId = randomUUID();
        when(userAndGroupProvider.isSystemUser(any(Action.class))).thenReturn(true);

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("notification.unsubscribe")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);

        verify(userAndGroupProvider).isSystemUser(eq(action));

        verifyNoInteractions(subscriptionProvider);
    }

    @Test
    public void shouldAllowSubscriptionOwnerToUnSubscribeSubscription() throws Exception {

        final UUID userId = randomUUID();
        when(userAndGroupProvider.isSystemUser(any(Action.class))).thenReturn(false);
        when(subscriptionProvider.isSubscriptionOwner(any(Action.class))).thenReturn(true);

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("notification.unsubscribe")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);

        verify(userAndGroupProvider).isSystemUser(eq(action));

        verify(subscriptionProvider).isSubscriptionOwner(eq(action));
    }

    @Test
    public void shouldNotAllowToUnsubscribeOtherUsersSubscription() throws Exception {

        final UUID userId = randomUUID();
        when(userAndGroupProvider.isSystemUser(any(Action.class))).thenReturn(false);
        when(subscriptionProvider.isSubscriptionOwner(any(Action.class))).thenReturn(false);
        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("notification.unsubscribe")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);

        verify(userAndGroupProvider).isSystemUser(eq(action));

        verify(subscriptionProvider).isSubscriptionOwner(eq(action));
    }
}
