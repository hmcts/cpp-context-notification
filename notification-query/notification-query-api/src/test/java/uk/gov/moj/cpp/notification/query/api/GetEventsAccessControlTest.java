package uk.gov.moj.cpp.notification.query.api;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
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

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetEventsAccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    private SubscriptionOwnerProvider subscriptionOwnerProvider;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public GetEventsAccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return ImmutableMap.<Class<?>, Object>builder()
                .put(SubscriptionOwnerProvider.class, subscriptionOwnerProvider)
                .put(UserAndGroupProvider.class, userAndGroupProvider)
                .build();
    }

    @Test
    public void shouldAllowSubscriptionOwnerToGetEvents() throws Exception {

        when(subscriptionOwnerProvider.isSubscriptionOwner(any(Action.class))).thenReturn(true);

        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();

        final JsonEnvelope query = envelope()
                .with(metadataWithRandomUUID("notification.get-events")
                        .withUserId(userId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build();

        final Action action = new Action(query);

        when(subscriptionOwnerProvider.isSubscriptionOwner(action)).thenReturn(true);

        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowNonOwnerToGetEvents() throws Exception {

        when(subscriptionOwnerProvider.isSubscriptionOwner(any(Action.class))).thenReturn(false);

        final String userId = randomUUID().toString();
        final String subscriptionId = randomUUID().toString();

        final JsonEnvelope query = envelope()
                .with(metadataWithRandomUUID("notification.get-events")
                        .withUserId(userId))
                .withPayloadOf(subscriptionId, "subscriptionId")
                .build();

        final Action action = new Action(query);

        when(subscriptionOwnerProvider.isSubscriptionOwner(action)).thenReturn(false);

        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }

    @Test
    public void shouldFailTheUserIdIsMissing() throws Exception {

        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withName("notification.get-events")
                )
                .withPayloadOf(randomUUID().toString(), "subscriptionId")
                .build();

        final Action action = new Action(query);
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
    }
}
