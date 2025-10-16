package uk.gov.moj.cpp.notification.integration.test.dataaccess;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.toSqlTimestamp;
import static uk.gov.moj.cpp.notification.common.FieldNames.USER_ID;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

public class SubscriptionJdbcInserter {

    private static final String INSERT_SUBSCRIPTION_SQL =
            "INSERT INTO subscription(id, owner_id, filter, created, modified) VALUES (?, ?, ?, ?, ?)";

    private static final String GET_MODIFICATION_TIME_SQL =
            "SELECT modified FROM subscription WHERE id = ?";
    private static final String CONTEXT_NAME = "notification";
    private static final String SET_MODIFICATION_DATE_SQL = "UPDATE subscription SET modified = ? where id = ?";
    private final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();

    public void insertSubscriptions(final int count, final int expiryDurationMilliSeconds) {
        for (int i = 0; i < count; i++) {
            final ZonedDateTime lastModified = new UtcClock().now().minusSeconds(RandomGenerator.integer(expiryDurationMilliSeconds / 1000).next()).plusMinutes(1);
            insertSubscription(randomUUID(), randomUUID(), now(), lastModified);
        }
    }

    public void insertExpiredSubscriptions(final int count, final int expiryDurationMilliSeconds) {
        for (int i = 0; i < count; i++) {
            final ZonedDateTime lastModified = new UtcClock().now().minusSeconds(expiryDurationMilliSeconds / 1000 + RandomGenerator.integer(24 * 60 * 60).next());
            insertSubscription(randomUUID(), randomUUID(), now(), lastModified);
        }
    }

    public ZonedDateTime rollbackModificationDate(final UUID subscriptionId, final int numberOfHours) {

        final ZonedDateTime expiredModificationDate = getModificationDate(subscriptionId).minusHours(numberOfHours);

        try(final Connection viewStoreConnection = testJdbcConnectionProvider.getViewStoreConnection(CONTEXT_NAME);
            final PreparedStatement preparedStatement = viewStoreConnection.prepareStatement(SET_MODIFICATION_DATE_SQL)) {

            preparedStatement.setTimestamp(1, toSqlTimestamp(expiredModificationDate));
            preparedStatement.setObject(2, subscriptionId);

            preparedStatement.executeUpdate();

            return expiredModificationDate;

        } catch (final SQLException e) {
            throw new DataAccessException("Failed to update modification date on subscription table", e);
        }
    }

    private ZonedDateTime getModificationDate(final UUID subscriptionId) {
        try(final Connection viewStoreConnection = testJdbcConnectionProvider.getViewStoreConnection(CONTEXT_NAME);
            final PreparedStatement preparedStatement = viewStoreConnection.prepareStatement(GET_MODIFICATION_TIME_SQL)) {

            preparedStatement.setObject(1, subscriptionId);

            try(final ResultSet resultSet = preparedStatement.executeQuery()) {
                if(resultSet.next()) {
                    return fromSqlTimestamp(resultSet.getTimestamp("modified"));
                }

                throw new RuntimeException(format("No subscription found with id '%s'", subscriptionId));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to get modification date from subscription table", e);
        }
    }

    public void insertSubscription(final UUID subscriptionId, final UUID userId, final ZonedDateTime created, final ZonedDateTime modified) {

        final JsonObject jsonObject = createObjectBuilder()
                .add("type", FIELD.toString())
                .add("name", USER_ID.name())
                .add("value", userId.toString())
                .add("operation", "EQUALS")
                .build();

        insertIntoSubscriptionTable(subscriptionId, userId, jsonObject, created, modified);
    }

    public void insertUserIdSubscription(final UUID subscriptionId, final UUID userId) {

        final JsonObject jsonObject = createObjectBuilder()
                .add("type", FIELD.toString())
                .add("name", USER_ID.name())
                .add("value", userId.toString())
                .add("operation", EQUALS.name())
                .build();

        insertIntoSubscriptionTable(subscriptionId, userId, jsonObject);
    }

    private void insertIntoSubscriptionTable(
            final UUID subscriptionId,
            final UUID ownerId,
            final JsonObject jsonObject) {

        final ZonedDateTime timestamp = now();
        insertIntoSubscriptionTable(subscriptionId, ownerId, jsonObject, timestamp, timestamp);
    }

    private void insertIntoSubscriptionTable(
            final UUID subscriptionId,
            final UUID ownerId,
            final JsonObject jsonObject,
            final ZonedDateTime createdOn,
            final ZonedDateTime lastModified) {

        final String json = jsonObject.toString();

        try (
                final Connection viewStoreConnection = testJdbcConnectionProvider.getViewStoreConnection(CONTEXT_NAME);
                final PreparedStatement preparedStatement = viewStoreConnection.prepareStatement(INSERT_SUBSCRIPTION_SQL)
        ) {
            preparedStatement.setObject(1, subscriptionId);
            preparedStatement.setObject(2, ownerId);
            preparedStatement.setString(3, json);
            preparedStatement.setTimestamp(4, toSqlTimestamp(createdOn));
            preparedStatement.setTimestamp(5, toSqlTimestamp(lastModified));

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert event using SQL: " + INSERT_SUBSCRIPTION_SQL, e);
        }
    }
}
