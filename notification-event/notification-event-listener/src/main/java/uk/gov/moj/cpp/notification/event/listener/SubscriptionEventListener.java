package uk.gov.moj.cpp.notification.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.json.schemas.domains.notification.FilterUpdated;
import uk.gov.justice.json.schemas.domains.notification.Subscribed;
import uk.gov.justice.json.schemas.domains.notification.Unsubscribed;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.notification.persistence.SubscriptionRepository;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SubscriptionEventListener {

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Handles("notification.subscribed")
    public void subscribed(final Envelope<Subscribed> event) {
        final Subscribed payload = event.payload();

        final UUID subscriptionId = payload.getSubscriptionId();
        final UUID ownerId = payload.getOwnerId();
        final String filter = payload.getFilter().toString();
        final ZonedDateTime created = payload.getCreated();

        subscriptionRepository.save(new Subscription(subscriptionId, ownerId, filter, created));
    }

    @Handles("notification.unsubscribed")
    public void unsubscribed(final Envelope<Unsubscribed> event) {
        final UUID subscriptionId = event.payload().getSubscriptionId();

        subscriptionRepository.removeByPrimaryKey(subscriptionId);
    }

    @Handles("notification.filter-updated")
    public void filterUpdated(final Envelope<FilterUpdated> event) {
        final FilterUpdated payload = event.payload();

        final UUID subscriptionId = payload.getSubscriptionId();

        final Subscription subscription = subscriptionRepository.findBy(subscriptionId);
        subscription.setModified(payload.getModified());
        subscription.setFilter(payload.getFilter().toString());

        subscriptionRepository.save(subscription);
    }

}
