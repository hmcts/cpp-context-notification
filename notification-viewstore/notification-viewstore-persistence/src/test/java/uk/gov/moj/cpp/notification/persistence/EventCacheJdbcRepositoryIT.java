package uk.gov.moj.cpp.notification.persistence;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.UUID.randomUUID;
import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.within;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.Every.everyItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;

public class EventCacheJdbcRepositoryIT {

    private static final int EVENT_CACHE_COUNT = 500;
    private static final int MAX_MINUTES_IN_PAST = 30000;
    private static final int ONE = 1;
    private static final long ONE_HOUR_IN_SECONDS = 3600L;

    private static final String LIQUIBASE_VIEW_STORE_CHANGELOG_XML = "liquibase/notification-view-store-db-changelog.xml";

    private final DataSource viewStoreDataSource = anInMemoryDataSource();
    private final EventCacheJdbcDataInserter eventCacheJdbcDataInserter = new EventCacheJdbcDataInserter(viewStoreDataSource);

    private EventCacheJdbcRepository jdbcRepository;

    @Before
    public void initializeDependencies() throws Exception {

        final EventCacheJdbcRepositoryConfig eventCacheJdbcRepositoryConfig = mock(EventCacheJdbcRepositoryConfig.class);
        final ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider = mock(ViewStoreJdbcDataSourceProvider.class);


        jdbcRepository = new EventCacheJdbcRepository(
                eventCacheJdbcRepositoryConfig,
                viewStoreJdbcDataSourceProvider,
                new PreparedStatementWrapperFactory(),
                getLogger(EventCacheJdbcRepository.class)
        );

        when(eventCacheJdbcRepositoryConfig.getBatchSize()).thenReturn(10);
        when(viewStoreJdbcDataSourceProvider.getDataSource()).thenReturn(viewStoreDataSource);
    }

    @Before
    public void initDatabase() throws Exception {
        final Liquibase liquibase = new Liquibase(LIQUIBASE_VIEW_STORE_CHANGELOG_XML,
                new ClassLoaderResourceAccessor(), new JdbcConnection(viewStoreDataSource.getConnection()));
        liquibase.dropAll();
        liquibase.update("");
    }
    @Test
    public void shouldCleanExpiredEventCaches() throws Exception {
        final int expiredEventCachesCount = RandomGenerator.integer(EVENT_CACHE_COUNT).next();
        final int unexpiredEventCachesCount = EVENT_CACHE_COUNT - expiredEventCachesCount;
        final ZonedDateTime currentDateTime = new UtcClock().now();
        final ZonedDateTime beforeDateTime = currentDateTime.minusSeconds(ONE_HOUR_IN_SECONDS);

        eventCacheJdbcDataInserter.insertEventCaches(generateEventCaches(expiredEventCachesCount, true));
        eventCacheJdbcDataInserter.insertEventCaches(generateEventCaches(unexpiredEventCachesCount, false));

        jdbcRepository.removeExpiredEventCaches(beforeDateTime);

        final List<EventCache> eventCaches = eventCacheJdbcDataInserter.findAllEventCaches();
        assertThat(eventCaches, hasSize(unexpiredEventCachesCount));
        assertThat(eventCaches, everyItem(hasProperty("created", is(within(1L, HOURS, currentDateTime)))));
    }

    @Test
    public void shouldGetEventsByUserIdFilter() throws Exception {
        final UUID streamId = randomUUID();
        final UUID userId = randomUUID();
        final String eventName = "public.events.test-notification";

        final List<EventCache> eventCache = new ArrayList<>();

        final UUID knownClientCorrelationId = randomUUID();
        final EventCache eventWithKnownCorrelationId = new EventCache(randomUUID(), userId, randomUUID(),
                knownClientCorrelationId.toString(), streamId,
                "", new UtcClock().now(), eventName);
        final EventCache eventWithRandomCorrelationId = new EventCache(randomUUID(), userId, randomUUID(),
                STRING.next(), streamId,
                "", new UtcClock().now(), eventName);

        eventCache.add(eventWithKnownCorrelationId);
        eventCache.add(eventWithRandomCorrelationId);

        eventCacheJdbcDataInserter.insertEventCaches(eventCache);

        final List<EventCache> allMatchingEvents = jdbcRepository.queryByFilter("USER_ID = '" + userId + "'", Optional.empty());

        assertThat(allMatchingEvents, is(notNullValue()));
        assertThat(allMatchingEvents.size(), is(2));

        final List<EventCache> matchingEventsForCorrelationId = jdbcRepository.queryByFilter("USER_ID = '" + userId + "'", Optional.of(knownClientCorrelationId.toString()));

        assertThat(matchingEventsForCorrelationId, is(notNullValue()));
        assertThat(matchingEventsForCorrelationId.size(), is(1));
        assertThat(matchingEventsForCorrelationId.get(0).getId(), is(eventWithKnownCorrelationId.getId()));
    }

    @Test
    public void shouldGetEventsByStreamIdFilter() throws Exception {
        final UUID streamId = randomUUID();
        final UUID userId = randomUUID();
        final String eventName = "public.events.test-notification";

        final List<EventCache> eventCache = new ArrayList<>();

        final UUID knownClientCorrelationId = randomUUID();
        final EventCache eventWithKnownCorrelationId = new EventCache(randomUUID(), userId, randomUUID(),
                knownClientCorrelationId.toString(), streamId,
                "", new UtcClock().now(), eventName);
        final EventCache eventWithRandomCorrelationId = new EventCache(randomUUID(), userId, randomUUID(),
                STRING.next(), streamId,
                "", new UtcClock().now(), eventName);

        eventCache.add(eventWithKnownCorrelationId);
        eventCache.add(eventWithRandomCorrelationId);

        eventCacheJdbcDataInserter.insertEventCaches(eventCache);

        final List<EventCache> allMatchingEvents = jdbcRepository.queryByFilter("STREAM_ID = '" + streamId + "'", Optional.empty());

        assertThat(allMatchingEvents, is(notNullValue()));
        assertThat(allMatchingEvents.size(), is(2));

        final List<EventCache> matchingEventsForCorrelationId = jdbcRepository.queryByFilter("STREAM_ID = '" + streamId + "'", Optional.of(knownClientCorrelationId.toString()));

        assertThat(matchingEventsForCorrelationId, is(notNullValue()));
        assertThat(matchingEventsForCorrelationId.size(), is(1));
        assertThat(matchingEventsForCorrelationId.get(0).getId(), is(eventWithKnownCorrelationId.getId()));
    }

    @Test
    public void shouldGetEventsByEventNameFilter() throws Exception {
        final UUID streamId = randomUUID();
        final UUID userId = randomUUID();
        final String eventName = "public.events.test-notification";

        final List<EventCache> eventCache = new ArrayList<>();

        final UUID knownClientCorrelationId = randomUUID();
        final EventCache eventWithKnownCorrelationId = new EventCache(randomUUID(), userId, randomUUID(),
                knownClientCorrelationId.toString(), streamId,
                "", new UtcClock().now(), eventName);
        final EventCache eventWithRandomCorrelationId = new EventCache(randomUUID(), userId, randomUUID(),
                STRING.next(), streamId,
                "", new UtcClock().now(), eventName);

        eventCache.add(eventWithKnownCorrelationId);
        eventCache.add(eventWithRandomCorrelationId);

        eventCacheJdbcDataInserter.insertEventCaches(eventCache);

        final List<EventCache> allMatchingEvents = jdbcRepository.queryByFilter("NAME = '" + eventName + "'", Optional.empty());

        assertThat(allMatchingEvents, is(notNullValue()));
        assertThat(allMatchingEvents.size(), is(2));

        final List<EventCache> matchingEventsForCorrelationId = jdbcRepository.queryByFilter("NAME = '" + eventName + "'", Optional.of(knownClientCorrelationId.toString()));

        assertThat(matchingEventsForCorrelationId, is(notNullValue()));
        assertThat(matchingEventsForCorrelationId.size(), is(1));
        assertThat(matchingEventsForCorrelationId.get(0).getId(), is(eventWithKnownCorrelationId.getId()));
    }


    private List<EventCache> generateEventCaches(final int count, final boolean expiredEvent) {

        final ZonedDateTime zonedDateTime;
        if (expiredEvent) {
            zonedDateTime = new UtcClock().now().minusHours(ONE).minusMinutes(RandomGenerator.integer(MAX_MINUTES_IN_PAST).next());
        } else {
            zonedDateTime = new UtcClock().now().minusMinutes(RandomGenerator.integer(59).next());
        }

        final List<EventCache> eventCaches = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            eventCaches.add(
                    new EventCache(randomUUID(), randomUUID(), randomUUID(),
                            STRING.next(), randomUUID(),
                            "", zonedDateTime, STRING.next()));
        }
        return eventCaches;
    }

    private static DataSource anInMemoryDataSource() {

        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;MV_STORE=FALSE;MVCC=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        return dataSource;
    }
}
