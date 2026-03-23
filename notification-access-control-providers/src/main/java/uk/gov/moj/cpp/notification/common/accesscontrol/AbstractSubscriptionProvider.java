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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSubscriptionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSubscriptionProvider.class);
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

        LOGGER.info("isSubscriptionOwner check - userId.isPresent: {}, userId: {}",
                userId.isPresent(), userId.orElse("NONE"));
        LOGGER.info("isSubscriptionOwner check - payload keys: {}, payload: {}",
                payload.keySet(), payload.toString());
        LOGGER.info("isSubscriptionOwner check - payload.containsKey(subscriptionId): {}",
                payload.containsKey(SUBSCRIPTION_ID_FIELD_NAME));

        if (userId.isPresent() && payload.containsKey(SUBSCRIPTION_ID_FIELD_NAME)) {

            final JsonEnvelope subscriptionQuery = enveloper.withMetadataFrom(envelope, SUBSCRIPTION_QUERY)
                    .apply(createObjectBuilder().add("subscriptionId", payload.getString(SUBSCRIPTION_ID_FIELD_NAME)).build());

            final JsonEnvelope subscription = notificationQueryView.getSubscription(subscriptionQuery);

            LOGGER.info("isSubscriptionOwner check - subscription found: {}", subscription != null);

            if (subscription == null || NULL.equals(subscription.payload())) {
                LOGGER.info("isSubscriptionOwner check - returning false (subscription null or empty)");
                return false;
            }

            final String ownerId = subscription.payloadAsJsonObject().getString("ownerId");
            final boolean isOwner = userId.get().equals(ownerId);
            LOGGER.info("isSubscriptionOwner check - ownerId: {}, userId: {}, isOwner: {}",
                    ownerId, userId.get(), isOwner);
            return isOwner;
        }
        LOGGER.info("isSubscriptionOwner check - returning false (userId not present or subscriptionId not in payload)");
        return false;
    }

    public abstract Requester getRequester();
}

