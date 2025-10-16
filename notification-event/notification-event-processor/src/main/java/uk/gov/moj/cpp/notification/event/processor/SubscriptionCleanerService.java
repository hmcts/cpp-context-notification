package uk.gov.moj.cpp.notification.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;

import org.slf4j.Logger;

@ApplicationScoped
public class SubscriptionCleanerService {

    @Inject
    Logger logger;

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    Requester requester;

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    Sender sender;

    static final String SUBSCRIPTION_QUERY = "notification.find-expired-subscriptions";
    static final String UNSUBSCRIBE_COMMAND = "notification.unsubscribe";

    public void unsubscribeExpiredSubscriptions() {
        logger.trace("Started getting expired subscriptions");
        final JsonEnvelope expiredSubscriptionEnvelope = getExpiredSubscriptionEnvelope();
        final JsonEnvelope subscriptions = requester.requestAsAdmin(expiredSubscriptionEnvelope);
        final JsonArray subscriptionsToExpire = subscriptions.payloadAsJsonObject().getJsonArray("expiredSubscriptions");
        if (subscriptionsToExpire != null) {
            for (int i = 0; i < subscriptionsToExpire.size(); i++) {
                final JsonEnvelope unsubscribeCommand = getUnsubscribeCommand(
                        fromString(subscriptionsToExpire.getJsonObject(i).getJsonString("subscriptionId").getString()));
                sender.sendAsAdmin(unsubscribeCommand);
            }
        }
        logger.trace("Completed unsubscribing expired subscriptions");
    }

    JsonEnvelope getExpiredSubscriptionEnvelope() {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(SUBSCRIPTION_QUERY),
                createObjectBuilder()
                        .build());
    }

    JsonEnvelope getUnsubscribeCommand(UUID subscriptionId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(UNSUBSCRIBE_COMMAND),
                createObjectBuilder()
                        .add("subscriptionId", subscriptionId.toString())
                        .build());
    }
}