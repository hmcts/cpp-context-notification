package uk.gov.justice.json.schemas.domains.notification;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

@Event("notification.subscribed")
public class Subscribed {
    private final ZonedDateTime created;

    private final JsonObject filter;

    private final UUID ownerId;

    private final UUID subscriptionId;

    public Subscribed(final ZonedDateTime created, final JsonObject filter, final UUID ownerId, final UUID subscriptionId) {
        this.created = created;
        this.filter = filter;
        this.ownerId = ownerId;
        this.subscriptionId = subscriptionId;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public JsonObject getFilter() {
        return filter;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public static Builder subscribed() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null || getClass() != obj.getClass()){
            return false;
        }
        final Subscribed that = (Subscribed) obj;

        return java.util.Objects.equals(this.created, that.created) &&
                java.util.Objects.equals(this.filter, that.filter) &&
                java.util.Objects.equals(this.ownerId, that.ownerId) &&
                java.util.Objects.equals(this.subscriptionId, that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(created, filter, ownerId, subscriptionId);
    }

    @Override
    public String toString() {
        return "Subscribed{" +
                "created='" + created + "'," +
                "filter='" + filter + "'," +
                "ownerId='" + ownerId + "'," +
                "subscriptionId='" + subscriptionId + "'" +
                "}";
    }

    public static class Builder {
        private ZonedDateTime created;

        private JsonObject filter;

        private UUID ownerId;

        private UUID subscriptionId;

        public Builder withCreated(final ZonedDateTime created) {
            this.created = created;
            return this;
        }

        public Builder withFilter(final JsonObject filter) {
            this.filter = filter;
            return this;
        }

        public Builder withOwnerId(final UUID ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder withSubscriptionId(final UUID subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public Subscribed build() {
            return new Subscribed(created, filter, ownerId, subscriptionId);
        }
    }
}
