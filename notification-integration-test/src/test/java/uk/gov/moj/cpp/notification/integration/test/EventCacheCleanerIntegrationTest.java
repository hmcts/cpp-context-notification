package uk.gov.moj.cpp.notification.integration.test;

import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.EventCachePoller;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.EventJdbcInserter;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventCacheCleanerIntegrationTest {

    private static final String CONTEXT_NAME = "notification";

    private static final int RETRY_COUNT = 1000;
    private static final int EVENT_CACHE_COUNT = 500;
    private static final int MAX_MINUTES_IN_PAST = 30000;
    private static final int ONE = 1;

    private static final ZonedDateTime CURRENT_TIME = new UtcClock().now();
    private static final String EVENT_JSON = Json.createObjectBuilder()
            .add("userId", randomUUID().toString())
            .add("name", "public.listing.hearing-changes-saved")
            .build()
            .toString();

    private static final ZonedDateTime expiredCreatedDateTime = CURRENT_TIME.minusHours(ONE)
            .minusMinutes(RandomGenerator.integer(MAX_MINUTES_IN_PAST).next());

    private static final ZonedDateTime unexpiredCreatedDateTime = CURRENT_TIME
            .minusMinutes(RandomGenerator.integer(59).next());

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final EventJdbcInserter eventJdbcInserter = new EventJdbcInserter();
    private final EventCachePoller eventCachePoller = new EventCachePoller();

    @BeforeEach
    public void cleanTheDatabaseBefore() {
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "event_cache", "subscription");
    }

    @AfterEach
    public void cleanTheDatabaseAfter() {
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "event_cache", "subscription");
    }

    @Test
    public void shouldRemoveExpiredEventsFromEventCache() throws Exception {
        final UUID userId = randomUUID();
        final int expiredEventCachesCount = RandomGenerator.integer(EVENT_CACHE_COUNT).next();
        final int unexpiredEventCachesCount = EVENT_CACHE_COUNT - expiredEventCachesCount;

        range(0, expiredEventCachesCount)
                .forEach(i -> createEventWith(userId, expiredCreatedDateTime));

        range(0, unexpiredEventCachesCount)
                .forEach(i -> createEventWith(userId, unexpiredCreatedDateTime));

        final List<EventCache> events = eventCachePoller.pollByUserIdUntilResultSize(userId, unexpiredEventCachesCount, RETRY_COUNT);

        assertThat(events.size(), is(unexpiredEventCachesCount));
    }

    private void createEventWith(final UUID userId, final ZonedDateTime created) {
        eventJdbcInserter.insertUserIdAndCreatedEvent(userId, created, EVENT_JSON);
    }
}
