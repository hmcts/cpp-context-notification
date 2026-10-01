package uk.gov.moj.cpp.notification.persistence;

import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class EventCacheRepository {

    @PersistenceContext(unitName = "notification")
    EntityManager entityManager;

    public List<EventCache> findByUserIdOrderByCreatedDesc(final UUID userId) {
        return entityManager.createQuery(
                        "SELECT e FROM EventCache e WHERE e.userId = :userId ORDER BY e.created DESC",
                        EventCache.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<EventCache> findByClientCorrelationIdOrderByCreatedDesc(final String clientCorrelationId) {
        return entityManager.createQuery(
                        "SELECT e FROM EventCache e WHERE e.clientCorrelationId = :clientCorrelationId ORDER BY e.created DESC",
                        EventCache.class)
                .setParameter("clientCorrelationId", clientCorrelationId)
                .getResultList();
    }

    public List<EventCache> findByStreamIdOrderByCreatedDesc(final UUID streamId) {
        return entityManager.createQuery(
                        "SELECT e FROM EventCache e WHERE e.streamId = :streamId ORDER BY e.created DESC",
                        EventCache.class)
                .setParameter("streamId", streamId)
                .getResultList();
    }

    public List<EventCache> findByNameOrderByCreatedDesc(final String eventName) {
        return entityManager.createQuery(
                        "SELECT e FROM EventCache e WHERE e.name = :name ORDER BY e.created DESC",
                        EventCache.class)
                .setParameter("name", eventName)
                .getResultList();
    }

    public List<EventCache> findAll() {
        return entityManager.createQuery("SELECT e FROM EventCache e", EventCache.class)
                .getResultList();
    }

    public EventCache save(final EventCache eventCache) {
        return entityManager.merge(eventCache);
    }
}
