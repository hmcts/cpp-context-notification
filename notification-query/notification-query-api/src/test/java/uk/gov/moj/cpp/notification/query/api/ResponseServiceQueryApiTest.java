package uk.gov.moj.cpp.notification.query.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.notification.query.view.NotificationQueryView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResponseServiceQueryApiTest {

    @InjectMocks
    private ResponseServiceQueryApi responseServiceQueryApi;

    @Mock
    private JsonEnvelope query;

    @Mock
    private NotificationQueryView notificationQueryView;

    @Test
    public void shouldVerifyFindEventsIsPassedThroughToTheQueryView() {
        responseServiceQueryApi.findEvents(query);
        verify(notificationQueryView).findEvents(query);
        assertThat(responseServiceQueryApi,
                isHandler(QUERY_API)
                        .with(method("findEvents")
                                .thatHandles("notification.get-events")
                        ));
    }

    @Test
    public void shouldVerifyGetSubscriptionIsPassedThroughToTheQueryView() {
        responseServiceQueryApi.getSubscription(query);
        verify(notificationQueryView).getSubscription(query);
        assertThat(responseServiceQueryApi,
                isHandler(QUERY_API)
                        .with(method("getSubscription")
                                .thatHandles("notification.get-subscription")
                        ));
    }

    @Test
    public void shouldVerifyFindExpiredSubscriptionsIsPassedThroughToTheQueryView() {
        responseServiceQueryApi.findExpiredSubscriptions(query);
        verify(notificationQueryView).findExpiredSubscriptions(query);
        assertThat(responseServiceQueryApi,
                isHandler(QUERY_API)
                        .with(method("findExpiredSubscriptions")
                                .thatHandles("notification.find-expired-subscriptions")
                        ));
    }
}
