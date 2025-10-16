package uk.gov.justice.json.schemas.domains.notification;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

@Event("notification.filter-updated")
public class FilterUpdated {
    private final JsonObject filter;

    private final ZonedDateTime modified;

    private final UUID subscriptionId;

    public FilterUpdated(final JsonObject filter, final ZonedDateTime modified, final UUID subscriptionId) {
        this.filter = filter;
        this.modified = modified;
        this.subscriptionId = subscriptionId;
    }

    public JsonObject getFilter() {
        return filter;
    }

    public ZonedDateTime getModified() {
        return modified;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public static Builder filterUpdated() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final FilterUpdated that = (FilterUpdated) obj;

        return java.util.Objects.equals(this.filter, that.filter) &&
                java.util.Objects.equals(this.modified, that.modified) &&
                java.util.Objects.equals(this.subscriptionId, that.subscriptionId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(filter, modified, subscriptionId);
    }

    @Override
    public String toString() {
        return "FilterUpdated{" +
                "filter='" + filter + "'," +
                "modified='" + modified + "'," +
                "subscriptionId='" + subscriptionId + "'" +
                "}";
    }

    public static class Builder {
        private JsonObject filter;

        private ZonedDateTime modified;

        private UUID subscriptionId;

        public Builder withFilter(final JsonObject filter) {
            this.filter = filter;
            return this;
        }

        public Builder withModified(final ZonedDateTime modified) {
            this.modified = modified;
            return this;
        }

        public Builder withSubscriptionId(final UUID subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public FilterUpdated build() {
            return new FilterUpdated(filter, modified, subscriptionId);
        }
    }
}
