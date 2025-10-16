package uk.gov.moj.cpp.notification.integration.test.dataaccess;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.UUID;

public class EventJdbcInserter {

    private static final String INSERT_EVENT_SQL =
            "INSERT INTO event_cache(" +
                    "id, user_id, session_id, client_correlation_id, stream_id, event_json, created, name" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();

    public void insertUserIdEvent(final UUID userId, final String eventJson) {

        insertEvent(userId, randomUUID(), eventJson, new UtcClock().now());
    }

    public void insertEventIdUserIdAndCorrelationIdEvent(final UUID id, final UUID userId, final String clientCorrelationId, final String eventJson) {

        insertEvent(id, userId, randomUUID(), eventJson, new UtcClock().now(), clientCorrelationId);
    }

    public void insertSessionIdEvent(final UUID sessionId, final String eventJson) {

        insertEvent(randomUUID(), sessionId, eventJson, new UtcClock().now());
    }

    public void insertUserIdAndCreatedEvent(final UUID userId, final ZonedDateTime created, final String eventJson) {

        insertEvent(userId, randomUUID(), eventJson, created);
    }

    private void insertEvent(final UUID userId, final UUID sessionId, final String eventJson, final ZonedDateTime created) {
        insertEvent(randomUUID(), userId, sessionId, eventJson, created, STRING.next());
    }

    private void insertEvent(final UUID id, final UUID userId, final UUID sessionId, final String eventJson, final ZonedDateTime created, final String clientCorrelationId) {
        final UUID streamId = randomUUID();
        final String eventName = STRING.next();

        try (
                final Connection viewStoreConnection = testJdbcConnectionProvider.getViewStoreConnection("notification");
                final PreparedStatement preparedStatement = viewStoreConnection.prepareStatement(INSERT_EVENT_SQL)
        ) {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, userId);
            preparedStatement.setObject(3, sessionId);
            preparedStatement.setString(4, clientCorrelationId);
            preparedStatement.setObject(5, streamId);
            preparedStatement.setString(6, eventJson);
            preparedStatement.setTimestamp(7, toSqlTimestamp(created));
            preparedStatement.setString(8, eventName);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert event using SQL: " + INSERT_EVENT_SQL, e);
        }
    }
}
