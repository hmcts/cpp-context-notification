package uk.gov.moj.cpp.notification.integration.test.dataaccess;

import uk.gov.justice.services.test.utils.core.helper.Sleeper;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.util.Optional;
import java.util.UUID;

import static java.util.Optional.empty;

public class SubscriptionJdbcPoller {

    private static final int DELAY_INTERVAL_MILLIS = 300;
    private static final int POLLING_DURATION_MILLIS = (30 * 1000);
    private static final int RETRY_COUNT = POLLING_DURATION_MILLIS /DELAY_INTERVAL_MILLIS; //It's failing in validation pipeline with fewer intervals, however polling will anyway stop once condition is satisfied and won't wait for 30 secs

    private final SubscriptionJdbcFinder subscriptionJdbcFinder = new SubscriptionJdbcFinder();
    private final Sleeper sleeper = new Sleeper();

    public Optional<Subscription> pollUntilFound(final UUID subscriptionId) {

        for (int i = 0; i < RETRY_COUNT; i++) {
            final Optional<Subscription> subscription = subscriptionJdbcFinder.findSubscription(subscriptionId);
            if(subscription.isPresent()) {
                return subscription;
            }

            sleeper.sleepFor(DELAY_INTERVAL_MILLIS);
        }

        return empty();
    }

    public void pollUntilNotFound(final UUID subscriptionId) {
        final Poller poller = new Poller(RETRY_COUNT, DELAY_INTERVAL_MILLIS);
        poller.pollUntilNotFound(() -> subscriptionJdbcFinder.findSubscription(subscriptionId));

        if(subscriptionJdbcFinder.findSubscription(subscriptionId).isPresent()) {
            throw new AssertionError("Subscription with subscriptionId " + subscriptionId + " still found in database after " + RETRY_COUNT + " attempts");
        }
    }
}
