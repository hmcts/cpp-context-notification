package uk.gov.moj.cpp.notification.integration.test.dataaccess;

import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;

import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventFinder {

    private static final String USER_ID_QUERY = "SELECT " +
            "id, user_id, session_id, client_correlation_id, stream_id, event_json, created, name " +
            "FROM event_cache " +
            "WHERE user_id = ?";

    private static final String CLIENT_CORRELATION_ID_QUERY = "SELECT " +
            "id, user_id, session_id, client_correlation_id, stream_id, event_json, created, name " +
            "FROM event_cache " +
            "WHERE client_correlation_id = ?";

    private static final String STREAM_ID_QUERY = "SELECT " +
            "id, user_id, session_id, client_correlation_id, stream_id, event_json, created, name " +
            "FROM event_cache " +
            "WHERE stream_id = ?";

    private static final String NAME_QUERY = "SELECT " +
            "id, user_id, session_id, client_correlation_id, stream_id, event_json, created, name " +
            "FROM event_cache " +
            "WHERE name = ?";

    private final TestJdbcConnectionProvider jdbcConnectionProvider = new TestJdbcConnectionProvider();

    public List<EventCache> findByUserId(final UUID userId) throws SQLException {
        return find(USER_ID_QUERY, userId);
    }

    public List<EventCache> findByClientCorrelationId(final String clientCorrelationId) throws SQLException {
        return find(CLIENT_CORRELATION_ID_QUERY, clientCorrelationId);
    }

    public List<EventCache> findByStreamId(final UUID streamId) throws SQLException {
        return find(STREAM_ID_QUERY, streamId);
    }

    public List<EventCache> findByEventName(final String eventName) throws SQLException {
        return find(NAME_QUERY, eventName);
    }

    private List<EventCache> find(final String sql, final String idParam) throws SQLException {

        final List<EventCache> eventCaches = new ArrayList<>();
        try (
                final Connection connection = jdbcConnectionProvider.getViewStoreConnection("notification");
                final PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {

            preparedStatement.setObject(1, idParam);

            extractResults(eventCaches, preparedStatement);
        }

        return eventCaches;
    }

    private List<EventCache> find(final String sql, final UUID idParam) throws SQLException {

        final List<EventCache> eventCaches = new ArrayList<>();
        try (
                final Connection connection = jdbcConnectionProvider.getViewStoreConnection("notification");
                final PreparedStatement preparedStatement = connection.prepareStatement(sql)
        ) {

            preparedStatement.setObject(1, idParam);

            extractResults(eventCaches, preparedStatement);
        }

        return eventCaches;
    }

    private void extractResults(final List<EventCache> eventCaches, final PreparedStatement preparedStatement) throws SQLException {
        try (final ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                final UUID id = (UUID) resultSet.getObject("id");
                final UUID userId = (UUID) resultSet.getObject("user_id");
                final UUID sessionId = (UUID) resultSet.getObject("session_id");
                final String correlationId = resultSet.getString("client_correlation_id");
                final UUID streamId = (UUID) resultSet.getObject("stream_id");
                final String eventJson = resultSet.getString("event_json");
                final Timestamp created = resultSet.getTimestamp("created");
                final String eventName = resultSet.getString("name");


                final EventCache eventCache = new EventCache(
                        id,
                        userId,
                        sessionId,
                        correlationId,
                        streamId,
                        eventJson,
                        fromSqlTimestamp(created),
                        eventName);

                eventCaches.add(eventCache);
            }
        }
    }
}
