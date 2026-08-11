package uk.gov.moj.cpp.notification.persistence;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SubscriptionRepositoryTest {

    private static final String PERSISTENCE_UNIT = "notification-test-persistence-unit";

    private static final UUID SUBSCRIPTION_A_UUID = randomUUID();
    private static final UUID SUBSCRIPTION_B_UUID = randomUUID();

    private static final UUID OWNER_ID_A = randomUUID();
    private static final UUID OWNER_ID_B = randomUUID();

    private static final String SUBSCRIPTION_A_FILTERS = "filterA";
    private static final String SUBSCRIPTION_B_FILTERS = "filterB";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private SubscriptionRepository subscriptionRepository;

    private Subscription subscriptionA;
    private Subscription subscriptionB;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        subscriptionRepository = new SubscriptionRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(subscriptionRepository);

        subscriptionA = new Subscription(SUBSCRIPTION_A_UUID, OWNER_ID_A, SUBSCRIPTION_A_FILTERS, now(UTC));
        subscriptionB = new Subscription(SUBSCRIPTION_B_UUID, OWNER_ID_B, SUBSCRIPTION_B_FILTERS, now(UTC));
    }

    @Test
    void shouldSaveASubscription() {

        subscriptionRepository.save(subscriptionA);

        final List<Subscription> subscriptions = subscriptionRepository.findAll();

        assertThat(subscriptions, hasSize(1));

        assertThat(subscriptions.get(0).getId(), is(subscriptionA.getId()));
        assertThat(subscriptions.get(0).getFilter(), is(subscriptionA.getFilter()));
        assertThat(subscriptions.get(0).getOwnerId(), is(subscriptionA.getOwnerId()));
        assertThat(subscriptions.get(0).getCreated().toInstant(), is(subscriptionA.getCreated().toInstant()));
    }

    @Test
    void shouldDeleteByPrimaryKey() {

        subscriptionRepository.save(subscriptionA);
        subscriptionRepository.save(subscriptionB);

        final Subscription subscription_a = subscriptionRepository.findBy(SUBSCRIPTION_A_UUID);
        assertThat(subscription_a, is(notNullValue()));
        assertThat(subscription_a.getOwnerId(), is(OWNER_ID_A));

        final Subscription subscription_b = subscriptionRepository.findBy(SUBSCRIPTION_B_UUID);
        assertThat(subscription_b, is(notNullValue()));
        assertThat(subscription_b.getOwnerId(), is(OWNER_ID_B));

        subscriptionRepository.removeByPrimaryKey(SUBSCRIPTION_A_UUID);

        assertThat(subscriptionRepository.findBy(SUBSCRIPTION_A_UUID), is(nullValue()));
        assertThat(subscriptionRepository.findBy(SUBSCRIPTION_B_UUID), is(notNullValue()));
    }

    @Test
    void shouldFindByModifiedLessThan() {

        final ZonedDateTime cutoff = now(UTC);

        final Subscription expired = new Subscription(randomUUID(), randomUUID(), "expired", cutoff.minusHours(2));
        final Subscription current = new Subscription(randomUUID(), randomUUID(), "current", cutoff.plusHours(2));

        subscriptionRepository.save(expired);
        subscriptionRepository.save(current);

        final List<Subscription> subscriptions = subscriptionRepository.findByModifiedLessThan(cutoff);

        assertThat(subscriptions, hasSize(1));
        assertThat(subscriptions.get(0).getId(), is(expired.getId()));
    }
}
