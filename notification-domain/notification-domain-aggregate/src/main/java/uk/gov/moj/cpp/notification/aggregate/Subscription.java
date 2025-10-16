package uk.gov.moj.cpp.notification.aggregate;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.json.schemas.domains.notification.FilterUpdated;
import uk.gov.justice.json.schemas.domains.notification.Subscribed;
import uk.gov.justice.json.schemas.domains.notification.Unsubscribed;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class Subscription implements Aggregate {

    private UUID subscriptionId;
    private boolean cancelled;
    private boolean exists;

    public Stream<Object> create(final ZonedDateTime created, JsonObject filter, final UUID ownerId, final UUID subscriptionId) {
        if (subscriptionId != null) {
            final Subscribed subscribed = Subscribed.subscribed().withOwnerId(ownerId)
                    .withSubscriptionId(subscriptionId)
                    .withFilter(filter)
                    .withCreated(created)
                    .build();
            return apply(Stream.of(subscribed));
        }
        else {
            return Stream.empty();
        }
    }

    public Stream<Object> cancel() {
        if (subscriptionId != null && !cancelled) {
            final Unsubscribed unsubscribed = Unsubscribed.unsubscribed().withSubscriptionId(subscriptionId).build();
            return apply(Stream.of(unsubscribed));
        }
            else {
            return Stream.empty();
        }
    }

    public Stream<Object> updateFilter(final JsonObject filter, final ZonedDateTime modified) {
        if (subscriptionId != null && !cancelled){
            final FilterUpdated filterUpdated = FilterUpdated.filterUpdated().withFilter(filter)
                    .withSubscriptionId(subscriptionId)
                    .withModified(modified)
                    .build();
            return apply(Stream.of(filterUpdated));
        }
        else {
            return Stream.empty();
        }
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(Subscribed.class).apply(subscribed -> {
                    subscriptionId = subscribed.getSubscriptionId();
                    exists = true;
                }),
                when(Unsubscribed.class).apply(unsubscribed -> {
                    cancelled = true;
                    exists = false;
                }),
                when(FilterUpdated.class).apply(filterUpdated -> doNothing()));
    }

    public boolean exists() {
        return exists;
    }

}
