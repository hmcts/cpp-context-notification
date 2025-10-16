package uk.gov.moj.cpp.notification.persistence.entity;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "event_cache")
public class EventCache {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "client_correlation_id")
    private String clientCorrelationId;

    @Column(name = "stream_id")
    private UUID streamId;

    @Column(name = "event_json")
    private String eventJson;

    @Column(name = "created")
    private ZonedDateTime created;

    @Column(name = "name")
    private String name;

    public EventCache() {
    }

    public EventCache(
            final UUID id,
            final UUID userId,
            final UUID sessionId,
            final String clientCorrelationId,
            final UUID streamId,
            final String eventJson,
            final ZonedDateTime created,
            final String name) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.clientCorrelationId = clientCorrelationId;
        this.streamId = streamId;
        this.eventJson = eventJson;
        this.created = created;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getClientCorrelationId() {
        return clientCorrelationId;
    }

    public UUID getStreamId() {
        return streamId;
    }

    public String getEventJson() {
        return eventJson;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EventCache that = (EventCache) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
