package uk.gov.moj.cpp.notification.persistence;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class SubscriptionRepositoryTest extends BaseTransactionalJunit4Test {

    private static final UUID SUBSCRIPTION_A_UUID = randomUUID();
    private static final UUID SUBSCRIPTION_B_UUID = randomUUID();

    private static final UUID OWNER_ID_A = randomUUID();
    private static final UUID OWNER_ID_B = randomUUID();

    private static final String SUBSCRIPTION_A_FILTERS = "filterA";
    private static final String SUBSCRIPTION_B_FILTERS = "filterB";

    private static final ZonedDateTime CREATED_A = now();
    private static final ZonedDateTime CREATED_B = now();

    private Subscription subscriptionA;
    private Subscription subscriptionB;

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Override
    protected void setUpBefore() {
        subscriptionA = new Subscription(SUBSCRIPTION_A_UUID, OWNER_ID_A, SUBSCRIPTION_A_FILTERS, CREATED_A);
        subscriptionB = new Subscription(SUBSCRIPTION_B_UUID, OWNER_ID_B, SUBSCRIPTION_B_FILTERS, CREATED_B);
    }

    @Test
    public void shouldSaveASubscription() throws Exception {

        subscriptionRepository.save(subscriptionA);

        final List<Subscription> subscriptions = subscriptionRepository.findAll();

        assertThat(subscriptions, hasSize(1));

        assertThat(subscriptions.get(0).getId(), is(subscriptionA.getId()));
        assertThat(subscriptions.get(0).getFilter(), is(subscriptionA.getFilter()));
        assertThat(subscriptions.get(0).getOwnerId(), is(subscriptionA.getOwnerId()));
        assertThat(subscriptions.get(0).getCreated().toInstant(), is(subscriptionA.getCreated().toInstant()));
    }

    @Test
    public void shouldDeleteByUUID() throws Exception {

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
}
