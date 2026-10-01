package uk.gov.moj.cpp.notification.persistence;

import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class EventCacheRepositoryTest {

    private static final String PERSISTENCE_UNIT = "notification-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private EventCacheRepository eventCacheRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        eventCacheRepository = new EventCacheRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(eventCacheRepository);
    }

    @Test
    void shouldSaveAnEventCache() {

        final UUID userId = randomUUID();
        final UUID sessionId = randomUUID();
        final String clientCorrelationId = STRING.next();
        final UUID streamId = randomUUID();
        final String eventName = STRING.next();

        final EventCache eventCache = anEventCache(userId, sessionId, clientCorrelationId, streamId, eventName);

        eventCacheRepository.save(eventCache);

        final List<EventCache> eventCaches = eventCacheRepository.findAll();

        assertThat(eventCaches, hasSize(1));
        assertThat(eventCaches.get(0).getId(), is(eventCache.getId()));
        assertThat(eventCaches.get(0).getUserId(), is(eventCache.getUserId()));
        assertThat(eventCaches.get(0).getStreamId(), is(eventCache.getStreamId()));
        assertThat(eventCaches.get(0).getSessionId(), is(eventCache.getSessionId()));
        assertThat(eventCaches.get(0).getClientCorrelationId(), is(eventCache.getClientCorrelationId()));
        assertThat(eventCaches.get(0).getName(), is(eventCache.getName()));
        assertThat(eventCaches.get(0).getEventJson(), is(eventCache.getEventJson()));
        assertThat(eventCaches.get(0).getCreated().toInstant(), is(eventCache.getCreated().toInstant()));
    }

    @Test
    void shouldFindAll() {

        final EventCache eventCache_1 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), STRING.next());
        final EventCache eventCache_2 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), STRING.next());
        final EventCache eventCache_3 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), STRING.next());

        eventCacheRepository.save(eventCache_1);
        eventCacheRepository.save(eventCache_2);
        eventCacheRepository.save(eventCache_3);

        final List<EventCache> eventCaches = eventCacheRepository.findAll();

        assertThat(eventCaches, hasSize(3));

        assertThat(eventCaches, hasItem(eventCache_1));
        assertThat(eventCaches, hasItem(eventCache_2));
        assertThat(eventCaches, hasItem(eventCache_3));
    }

    @Test
    void shouldFindByUserIdOrderByCreatedDesc() {

        final UUID userId = randomUUID();

        final EventCache eventCache_1 = anEventCache(userId, randomUUID(), STRING.next(), randomUUID(), STRING.next());
        final EventCache eventCache_2 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), STRING.next());
        final EventCache eventCache_3 = anEventCache(userId, randomUUID(), STRING.next(), randomUUID(), STRING.next());

        eventCacheRepository.save(eventCache_1);
        eventCacheRepository.save(eventCache_2);
        eventCacheRepository.save(eventCache_3);

        final List<EventCache> eventCaches = eventCacheRepository.findByUserIdOrderByCreatedDesc(userId);

        assertThat(eventCaches, hasSize(2));

        assertThat(eventCaches, hasItem(eventCache_1));
        assertThat(eventCaches, hasItem(eventCache_3));
    }

    @Test
    void shouldFindByClientCorrelationIdOrderByCreatedDesc() {
        final String clientCorrelationId = STRING.next();

        final EventCache eventCache_1 = anEventCache(randomUUID(), randomUUID(), clientCorrelationId, randomUUID(), STRING.next());
        final EventCache eventCache_2 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), STRING.next());
        final EventCache eventCache_3 = anEventCache(randomUUID(), randomUUID(), clientCorrelationId, randomUUID(), STRING.next());

        eventCacheRepository.save(eventCache_1);
        eventCacheRepository.save(eventCache_2);
        eventCacheRepository.save(eventCache_3);

        final List<EventCache> eventCaches = eventCacheRepository.findByClientCorrelationIdOrderByCreatedDesc(clientCorrelationId);

        assertThat(eventCaches, hasSize(2));

        assertThat(eventCaches, hasItem(eventCache_1));
        assertThat(eventCaches, hasItem(eventCache_3));
    }

    @Test
    void shouldFindByStreamIdOrderByCreatedDesc() {
        final UUID streamId = randomUUID();

        final EventCache eventCache_1 = anEventCache(randomUUID(), randomUUID(), STRING.next(), streamId, STRING.next());
        final EventCache eventCache_2 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), STRING.next());
        final EventCache eventCache_3 = anEventCache(randomUUID(), randomUUID(), STRING.next(), streamId, STRING.next());

        eventCacheRepository.save(eventCache_1);
        eventCacheRepository.save(eventCache_2);
        eventCacheRepository.save(eventCache_3);

        final List<EventCache> eventCaches = eventCacheRepository.findByStreamIdOrderByCreatedDesc(streamId);

        assertThat(eventCaches, hasSize(2));

        assertThat(eventCaches, hasItem(eventCache_1));
        assertThat(eventCaches, hasItem(eventCache_3));
    }

    @Test
    void shouldFindByNameOrderByCreatedDesc() {
        final String eventName = STRING.next();

        final EventCache eventCache_1 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), eventName);
        final EventCache eventCache_2 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), STRING.next());
        final EventCache eventCache_3 = anEventCache(randomUUID(), randomUUID(), STRING.next(), randomUUID(), eventName);

        eventCacheRepository.save(eventCache_1);
        eventCacheRepository.save(eventCache_2);
        eventCacheRepository.save(eventCache_3);

        final List<EventCache> eventCaches = eventCacheRepository.findByNameOrderByCreatedDesc(eventName);

        assertThat(eventCaches, hasSize(2));

        assertThat(eventCaches, hasItem(eventCache_1));
        assertThat(eventCaches, hasItem(eventCache_3));
    }

    private EventCache anEventCache(final UUID userId,
                                    final UUID sessionId,
                                    final String clientCorrelationId,
                                    final UUID streamId,
                                    final String eventName) {

        final UUID id = randomUUID();
        final String eventJson = "{\"id\":\"" + randomUUID() + "\"}";
        final ZonedDateTime created = of(now(), UTC);

        return new EventCache(
                id,
                userId,
                sessionId,
                clientCorrelationId,
                streamId,
                eventJson,
                created,
                eventName);
    }
}
