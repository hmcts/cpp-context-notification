package uk.gov.moj.cpp.notification.query.api;

import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class FindExpiredSubscriptionsApiAccessControlTest extends BaseDroolsAccessControlTest {

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public FindExpiredSubscriptionsApiAccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowSystemUserToFindExpiredSubscriptions() throws Exception {

        final UUID userId = randomUUID();
        when(userAndGroupProvider.isSystemUser(any(Action.class))).thenReturn(true);
        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("notification.find-expired-subscriptions")
                )
                .build();

        final Action action = spy(new Action(query));
        final ExecutionResults results = executeRulesWith(action);

        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider).isSystemUser(eq(action));
    }

    @Test
    public void shouldNotAllowNonSystemUserToFindExpiredSubscriptions() throws Exception {

        final UUID userId = randomUUID();
        when(userAndGroupProvider.isSystemUser(any(Action.class))).thenReturn(false);
        final JsonEnvelope query = envelope()
                .with(metadataWithDefaults()
                        .withUserId(userId.toString())
                        .withName("notification.find-expired-subscriptions")
                )
                .build();

        final Action action = spy(new Action(query));
        final ExecutionResults results = executeRulesWith(action);

        assertFailureOutcome(results);
        verify(userAndGroupProvider).isSystemUser(eq(action));
    }
}
