package uk.gov.moj.cpp.notification.persistence;

import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Repository
public interface EventCacheRepository extends EntityRepository<EventCache, UUID> {

    List<EventCache> findByUserIdOrderByCreatedDesc(final UUID userId);

    List<EventCache> findByClientCorrelationIdOrderByCreatedDesc(final String clientCorrelationId);

    List<EventCache> findByStreamIdOrderByCreatedDesc(final UUID streamId);

    List<EventCache> findByNameOrderByCreatedDesc(final String eventName);
}
