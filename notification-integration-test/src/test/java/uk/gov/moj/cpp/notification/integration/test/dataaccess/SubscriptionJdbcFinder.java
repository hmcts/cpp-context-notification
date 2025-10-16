package uk.gov.moj.cpp.notification.integration.test.dataaccess;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;

import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

public class SubscriptionJdbcFinder {

    private static final String SQL = "SELECT * FROM subscription where id = ?";

    private final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();

    public Optional<Subscription> findSubscription(final UUID subscriptionId) {


        try (
                final Connection connection = testJdbcConnectionProvider.getViewStoreConnection("notification");
                final PreparedStatement preparedStatement = connection.prepareStatement(SQL)
        ) {

            preparedStatement.setObject(1, subscriptionId);

            try (final ResultSet resultSet = preparedStatement.executeQuery()) {

                if (resultSet.next()) {
                    final UUID ownerId = (UUID) resultSet.getObject("owner_id");
                    final String filter = resultSet.getString("filter");
                    final ZonedDateTime created = fromSqlTimestamp(resultSet.getTimestamp("created"));
                    final Subscription subscription = new Subscription(
                            subscriptionId,
                            ownerId,
                            filter,
                            created);

                    return of(subscription);
                }
            }
        } catch (SQLException e) {
            throw new AssertionError("Failed to query database using statement " + SQL, e);
        }

        return empty();
    }
}
