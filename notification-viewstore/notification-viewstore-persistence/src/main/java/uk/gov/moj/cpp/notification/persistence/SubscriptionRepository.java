package uk.gov.moj.cpp.notification.persistence;

import static java.lang.Integer.parseInt;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class SubscriptionRepository {

    @PersistenceContext(unitName = "notification")
    EntityManager entityManager;

    @Inject
    @Value(key = "subscription_expiry_duration_seconds", defaultValue = "28800")
    String subscriptionExpiryDurationSeconds;

    public List<Subscription> findByModifiedLessThan(final ZonedDateTime expiredTime) {
        return entityManager.createQuery(
                        "SELECT s FROM Subscription s WHERE s.modified < :expiredTime",
                        Subscription.class)
                .setParameter("expiredTime", expiredTime)
                .getResultList();
    }

    public List<Subscription> findExpiredSubscriptions() {
        final ZonedDateTime subscriptionExpired = now(UTC).minusSeconds(parseInt(subscriptionExpiryDurationSeconds));

        return findByModifiedLessThan(subscriptionExpired);
    }

    public void removeByPrimaryKey(final UUID subscriptionId) {
        final Subscription subscription = findBy(subscriptionId);

        if (subscription != null) {
            remove(subscription);
        }
    }

    public Subscription findBy(final UUID id) {
        return entityManager.find(Subscription.class, id);
    }

    public List<Subscription> findAll() {
        return entityManager.createQuery("SELECT s FROM Subscription s", Subscription.class)
                .getResultList();
    }

    public Subscription save(final Subscription subscription) {
        return entityManager.merge(subscription);
    }

    public void remove(final Subscription subscription) {
        final Subscription managed = entityManager.contains(subscription) ? subscription : entityManager.merge(subscription);
        entityManager.remove(managed);
    }
}
