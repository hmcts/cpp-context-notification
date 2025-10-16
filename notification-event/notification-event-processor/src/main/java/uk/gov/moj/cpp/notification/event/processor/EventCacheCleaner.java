package uk.gov.moj.cpp.notification.event.processor;

import static java.lang.Long.parseLong;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.moj.cpp.notification.persistence.EventCacheJdbcRepository;

import java.time.ZonedDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
public class EventCacheCleaner {

    @Inject
    @Value(key = "eventCacheCleanerTimeToLiveSeconds", defaultValue = "3600")
    String eventCacheCleanerTimeToLiveSeconds;

    @Inject
    Logger logger;

    @Inject
    EventCacheJdbcRepository eventCacheJdbcRepository;

    @Inject
    Clock clock;

    public void removeExpiredEventCaches() {
        logger.info("Started removing expired EventCaches");
        eventCacheJdbcRepository.removeExpiredEventCaches(beforeDateTime());
    }

    private ZonedDateTime beforeDateTime() {
        return clock.now().minusSeconds(parseLong(eventCacheCleanerTimeToLiveSeconds));
    }
}
