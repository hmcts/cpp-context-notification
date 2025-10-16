package uk.gov.moj.cpp.notification.query.api;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.notification.common.accesscontrol.SubscriptionOwnerProvider;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class GetSubscriptionAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String SYSTEM_USER_GROUPS = "System Users";

    @Mock
    private SubscriptionOwnerProvider subscriptionOwnerProvider;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;

    public GetSubscriptionAccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder()
                .put(UserAndGroupProvider.class, userAndGroupProvider)
                .put(SubscriptionOwnerProvider.class, subscriptionOwnerProvider)
                .build();
    }

    @Test
    public void shouldAllowSystemUserToGetSubscription() throws Exception {

        when(userAndGroupProvider.isSystemUser(any(Action.class))).thenReturn(true);

        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();

        final JsonEnvelope query = envelope()
                .with(metadataWithRandomUUID("notification.get-subscription")
                        .withUserId(userId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build();

        final Action action = new Action(query);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowNormalUserEvenIfOwnerToGetSubscription() throws Exception {
        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();

        final JsonEnvelope query = envelope()
                .with(metadataWithRandomUUID("notification.get-subscription")
                        .withUserId(userId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build();

        final Action action = new Action(query);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldNotAllowNormalUserToGetSubscription() throws Exception {
        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();

        final JsonEnvelope query = envelope()
                .with(metadataWithRandomUUID("notification.get-subscription")
                        .withUserId(userId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build();

        final Action action = new Action(query);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @SuppressWarnings("Duplicates")
    @Test
    public void shouldFailTheActionNameIsIncorrect() throws Exception {

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
    public void shouldFailTheUserIdIsMissing() throws Exception {

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withName("notification.get-subscription")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }
}
