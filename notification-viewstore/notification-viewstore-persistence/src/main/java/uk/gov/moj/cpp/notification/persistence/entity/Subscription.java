package uk.gov.moj.cpp.notification.persistence.entity;

import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "filter")
    private String filter;

    @Column(name = "created")
    private ZonedDateTime created;

    @Column(name = "modified")
    private ZonedDateTime modified;

    // for JPA
    public Subscription() {
    }

    public Subscription(
            final UUID id,
            final UUID ownerId,
            final String filter,
            final ZonedDateTime created) {
        this.id = id;
        this.ownerId = ownerId;
        this.filter = filter;
        this.created = created;
        this.modified = created;
    }

    public UUID getId() {
        return id;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(final String filter) {
        this.filter = filter;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public ZonedDateTime getModified() {
        return modified;
    }

    public void setModified(final ZonedDateTime modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", filter='" + filter + '\'' +
                ", created=" + ZonedDateTimes.toString(created) +
                ", modified=" + ZonedDateTimes.toString(modified) +
                '}';
    }

}

