package uk.gov.moj.cpp.notification.query.api;

import static uk.gov.justice.services.core.annotation.Component.QUERY_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.notification.query.view.NotificationQueryView;

import javax.inject.Inject;

@ServiceComponent(QUERY_API)
public class ResponseServiceQueryApi {

    @Inject
    private NotificationQueryView notificationQueryView;

    @Handles("notification.get-events")
    public JsonEnvelope findEvents(final JsonEnvelope query) {
        return notificationQueryView.findEvents(query);
    }

    @Handles("notification.get-events-metadata")
    public JsonEnvelope findEventsMetadata(final JsonEnvelope query) {
        return notificationQueryView.findEventsMetadata(query);
    }
    @Handles("notification.get-subscription")
    public JsonEnvelope getSubscription(final JsonEnvelope query) {
        return notificationQueryView.getSubscription(query);
    }

    @Handles("notification.find-expired-subscriptions")
    public JsonEnvelope findExpiredSubscriptions(final JsonEnvelope query) {
        return notificationQueryView.findExpiredSubscriptions(query);
    }
}
