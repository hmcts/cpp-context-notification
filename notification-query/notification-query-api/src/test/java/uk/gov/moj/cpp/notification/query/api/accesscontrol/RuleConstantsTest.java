package uk.gov.moj.cpp.notification.query.api.accesscontrol;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.notification.common.accesscontrol.SubscriptionOwnerProvider;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class RuleConstantsTest extends BaseDroolsAccessControlTest {

    protected Action action;
    @Mock
    protected UserAndGroupProvider userAndGroupProvider;
    @Mock
    protected SubscriptionOwnerProvider subscriptionOwnerProvider;

    public RuleConstantsTest() {
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
    public void shouldNotAllowAuthorisedUserWithPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "notification.get-events");
        action = createActionFor(metadata);
        given(userAndGroupProvider.hasPermission(action,RuleConstants.getNonCpsDocumentPermission())).willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserToNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "notification.get-events");
        action = createActionFor(metadata);
        given(userAndGroupProvider.hasPermission(action,RuleConstants.getNonCpsDocumentPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentPermission());
    }

    @Test
    public void shouldAllowAuthorisedUserPermissionNonCpsDocuments() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "notification.get-events");
        action = createActionFor(metadata);
        given(userAndGroupProvider.hasPermission(action,RuleConstants.getNonCpsDocumentPermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getNonCpsDocumentPermission());
    }
}
