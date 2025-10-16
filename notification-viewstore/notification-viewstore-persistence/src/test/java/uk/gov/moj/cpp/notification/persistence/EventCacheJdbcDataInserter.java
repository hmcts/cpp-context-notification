package uk.gov.moj.cpp.notification.persistence;

import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;

import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.naming.NamingException;
import javax.sql.DataSource;

public class EventCacheJdbcDataInserter {

    private static final String INSERT_EVENT_SQL =
            "INSERT INTO event_cache(" +
                    "id, user_id, session_id, client_correlation_id, stream_id, event_json, created, name" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_ALL_EVENT_CACHE_ENTRIES = "SELECT * from event_cache";
    private static final String DELETE_ALL = "DELETE FROM event_cache";

    private final DataSource dataSource;

    public EventCacheJdbcDataInserter(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insertEventCaches(final List<EventCache> eventCaches) throws SQLException, NamingException {
        final Connection connection = dataSource.getConnection();
        eventCaches.forEach(eventCache -> insertEventCache(eventCache, connection));
    }

    public List<EventCache> findAllEventCaches() throws SQLException {
        final List<EventCache> eventCaches = new ArrayList<>();
        try (
                final PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement(FIND_ALL_EVENT_CACHE_ENTRIES);
                final ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                final EventCache eventCache = new EventCache(
                        (UUID) resultSet.getObject("id"),
                        (UUID) resultSet.getObject("user_id"),
                        (UUID) resultSet.getObject("session_id"),
                        resultSet.getString("client_correlation_id"),
                        (UUID) resultSet.getObject("stream_id"),
                        resultSet.getString("event_json"),
                        fromSqlTimestamp(resultSet.getTimestamp("created")),
                        resultSet.getString("name"));

                eventCaches.add(eventCache);
            }
        }
        
        return eventCaches;
    }

    public void deleteAll() throws Exception {
        try (final PreparedStatement preparedStatement = dataSource.getConnection().prepareStatement(DELETE_ALL)) {
            preparedStatement.executeUpdate();
        }
    }

    private void insertEventCache(final EventCache eventCache, final Connection connection) {
        try (
                final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_EVENT_SQL)
        ) {
            preparedStatement.setObject(1, eventCache.getId());
            preparedStatement.setObject(2, eventCache.getUserId());
            preparedStatement.setObject(3, eventCache.getSessionId());
            preparedStatement.setString(4, eventCache.getClientCorrelationId());
            preparedStatement.setObject(5, eventCache.getStreamId());
            preparedStatement.setString(6, eventCache.getEventJson());
            preparedStatement.setTimestamp(7, toSqlTimestamp(eventCache.getCreated()));
            preparedStatement.setString(8, eventCache.getName());
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert event using SQL: " + INSERT_EVENT_SQL, e);
        }
    }
}
