package uk.gov.moj.cpp.notification.event.processor;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.notification.persistence.EventCacheJdbcRepository;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class EventCacheCleanerTest {

    private static final long ONE_HOUR_IN_SECONDS = 3600L;

    @Mock
    private EventCacheJdbcRepository eventCacheJdbcRepository;

    @Mock
    private Logger logger;

    @Mock
    private List<EventCache> eventCaches;

    @Mock
    private Clock clock;

    @InjectMocks
    private EventCacheCleaner eventCacheCleaner;

    @Test
    public void shouldRemoveEventCaches() {
        eventCacheCleaner.eventCacheCleanerTimeToLiveSeconds = String.valueOf(ONE_HOUR_IN_SECONDS);

        final ZonedDateTime currentDateTime = new UtcClock().now();
        final ZonedDateTime expectedBeforeDateTime = currentDateTime.minusSeconds(ONE_HOUR_IN_SECONDS);

        when(clock.now()).thenReturn(currentDateTime);

        eventCacheCleaner.removeExpiredEventCaches();
        verify(eventCacheJdbcRepository).removeExpiredEventCaches(expectedBeforeDateTime);
    }

}