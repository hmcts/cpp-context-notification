package uk.gov.moj.cpp.notification.integration.test.dataaccess;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import uk.gov.justice.services.test.utils.core.helper.Sleeper;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class EventCachePoller {

    private static final int RETRY_COUNT = 5;
    private static final long DELAY_INTERVAL_MILLIS = 2000L;

    private final EventFinder eventFinder = new EventFinder();
    private final Sleeper sleeper = new Sleeper();

    public List<EventCache> pollByUserIdUntilFound(final UUID userId) {
        return pollUntilFoundBy(() -> {
            try {
                return eventFinder.findByUserId(userId);
            } catch (final SQLException e) {
                throw new RuntimeException(format("Failed to run SQL query for findByUserId with %s", userId), e);
            }
        });
    }

    public List<EventCache> pollByClientCorrelationIdUntilFound(final String clientCorrelationId) {
        return pollUntilFoundBy(() -> {
            try {
                return eventFinder.findByClientCorrelationId(clientCorrelationId);
            } catch (final SQLException e) {
                throw new RuntimeException(format("Failed to run SQL query for findByClientCorrelationId with %s", clientCorrelationId), e);
            }
        });
    }

    public List<EventCache> pollByStreamIdUntilFound(final UUID streamId) {
        return pollUntilFoundBy(() -> {
            try {
                return eventFinder.findByStreamId(streamId);
            } catch (final SQLException e) {
                throw new RuntimeException(format("Failed to run SQL query for findByStreamId with %s", streamId), e);
            }
        });
    }

    public List<EventCache> pollByEventNameUntilFound(final String eventName) {
        return pollUntilFoundBy(() -> {
            try {
                return eventFinder.findByEventName(eventName);
            } catch (final SQLException e) {
                throw new RuntimeException(format("Failed to run SQL query for findByEventName with %s", eventName), e);
            }
        });
    }

    public List<EventCache> pollUntilFoundBy(final Supplier<List<EventCache>> eventCacheSupplier) {
        for (int i = 0; i < RETRY_COUNT; i++) {
            final List<EventCache> eventCaches = eventCacheSupplier.get();
            if (!eventCaches.isEmpty()) {
                return eventCaches;
            }

            sleeper.sleepFor(DELAY_INTERVAL_MILLIS);
        }

        return emptyList();
    }

    public List<EventCache> pollByUserIdUntilResultSize(final UUID userId, final int resultSize, final int retryCount) {
        return pollUntilResultSize(() -> {
            try {
                return eventFinder.findByUserId(userId);
            } catch (final SQLException e) {
                throw new RuntimeException(format("Failed to run SQL query for findByUserId with %s", userId), e);
            }
        }, resultSize, retryCount);
    }

    public List<EventCache> pollUntilResultSize(final Supplier<List<EventCache>> eventCacheSupplier,
                                                final int resultSize,
                                                final int retryCount) {
        for (int i = 0; i < retryCount; i++) {
            final List<EventCache> eventCaches = eventCacheSupplier.get();

            if (resultSize == eventCaches.size()) {
                return eventCaches;
            }

            sleeper.sleepFor(DELAY_INTERVAL_MILLIS);
        }

        return emptyList();
    }
}
