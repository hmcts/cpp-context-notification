package uk.gov.moj.cpp.notification.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.moj.cpp.notification.test.utils.builder.FilterJsonBuilder.randomFilterJsonBuilder;

import uk.gov.justice.json.schemas.domains.notification.FilterUpdated;
import uk.gov.justice.json.schemas.domains.notification.Subscribed;
import uk.gov.justice.json.schemas.domains.notification.Unsubscribed;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.notification.persistence.SubscriptionRepository;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionEventListenerTest {

    private static final UUID subscriptionId = randomUUID();
    private static final UUID ownerId = randomUUID();
    private static final ZonedDateTime createdDate = PAST_LOCAL_DATE.next().atStartOfDay(UTC);
    private static final ZonedDateTime modifiedDate = PAST_LOCAL_DATE.next().atStartOfDay(UTC).plusHours(1L);
    private static final JsonObject filter = randomFilterJsonBuilder().build();
    private static final JsonObject updatedFilter = randomFilterJsonBuilder().build();

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Captor
    private ArgumentCaptor<Subscription> subscriptionCaptor;

    @InjectMocks
    private SubscriptionEventListener subscriptionEventListener;

    @Test
    public void shouldPersistNewSubscription() {
        final Envelope<Subscribed> envelope = getEnvelope(
                "notification.subscribed",
                new Subscribed(createdDate, filter, ownerId, subscriptionId));

        subscriptionEventListener.subscribed(envelope);

        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue().getCreated(), is(createdDate));
        assertThat(subscriptionCaptor.getValue().getOwnerId(), is(ownerId));
        assertThat(subscriptionCaptor.getValue().getId(), is(subscriptionId));
        assertThat(subscriptionCaptor.getValue().getFilter(), is(filter.toString()));
    }

    @Test
    public void shouldRemoveExistingSubscription() {
        final Envelope<Unsubscribed> envelope = getEnvelope(
                "notification.unsubscribed",
                new Unsubscribed(subscriptionId));

        subscriptionEventListener.unsubscribed(envelope);

        verify(subscriptionRepository).removeByPrimaryKey(subscriptionId);
    }

    @Test
    public void shouldUpdateFilterOnSubscription() {
        final Subscription subscription = Mockito.mock(Subscription.class);
        when(subscriptionRepository.findBy(subscriptionId)).thenReturn(subscription);

        final Envelope<FilterUpdated> envelope = getEnvelope(
                "notification.filter-updated",
                new FilterUpdated(updatedFilter, modifiedDate, subscriptionId));

        subscriptionEventListener.filterUpdated(envelope);

        verify(subscription).setFilter(updatedFilter.toString());
        verify(subscription).setModified(modifiedDate);
        verify(subscriptionRepository).save(subscription);
    }

    private <T> Envelope<T> getEnvelope(final String name, final T payload) {
        return envelopeFrom(Envelope
                .metadataBuilder().withName(name)
                .withId(UUID.randomUUID()), payload);
    }
}