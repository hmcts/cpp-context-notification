package uk.gov.moj.cpp.notification.common.accesscontrol;

import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.notification.query.view.NotificationQueryView;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public abstract class AbstractSubscriptionProvider {

    private static final String SUBSCRIPTION_ID_FIELD_NAME = "subscriptionId";
    private static final String SUBSCRIPTION_QUERY = "notification.get-subscription";

    @Inject
    Enveloper enveloper;

    @Inject
    private NotificationQueryView notificationQueryView;

    public boolean isSubscriptionOwner(final Action action) {

        final Optional<String> userId = action.userId();
        final JsonEnvelope envelope = action.envelope();
        final JsonObject payload = envelope.payloadAsJsonObject();

        if (userId.isPresent() && payload.containsKey(SUBSCRIPTION_ID_FIELD_NAME)) {

            final JsonEnvelope subscriptionQuery = enveloper.withMetadataFrom(envelope, SUBSCRIPTION_QUERY)
                    .apply(createObjectBuilder().add("subscriptionId", payload.getString(SUBSCRIPTION_ID_FIELD_NAME)).build());

            final JsonEnvelope subscription = notificationQueryView.getSubscription(subscriptionQuery);

            if (subscription == null || NULL.equals(subscription.payload())) {
                return false;
            }

            return userId.get().equals(
                    subscription.payloadAsJsonObject().getString("ownerId"));
        }
        return false;
    }

    public abstract Requester getRequester();
}

