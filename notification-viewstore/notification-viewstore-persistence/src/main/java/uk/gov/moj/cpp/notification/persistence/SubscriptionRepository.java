package uk.gov.moj.cpp.notification.persistence;

import static java.lang.Integer.parseInt;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.data.api.AbstractEntityRepository;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.criteria.CriteriaSupport;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Repository
public abstract class SubscriptionRepository extends AbstractEntityRepository<Subscription, UUID> implements CriteriaSupport<Subscription> {

    @Inject
    @Value(key = "subscription_expiry_duration_seconds", defaultValue = "28800")
    String subscriptionExpiryDurationSeconds;

    public abstract List<Subscription> findByModifiedLessThan(final ZonedDateTime expiredTime);

    public List<Subscription> findExpiredSubscriptions() {
        final ZonedDateTime subscriptionExpired =  now(UTC).minusSeconds(parseInt(subscriptionExpiryDurationSeconds));

        return findByModifiedLessThan(subscriptionExpired);
    }

    public void removeByPrimaryKey(final UUID subscriptionId) {
        final Subscription subscription = findBy(subscriptionId);

        if (subscription != null) {
            remove(subscription);
        }
    }
}
