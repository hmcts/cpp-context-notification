package uk.gov.moj.cpp.notification.persistence;

import static java.lang.String.format;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromSqlTimestamp;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryException;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapper;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.slf4j.Logger;

public class EventCacheJdbcRepository {

    private static final String DELETE_WITH_LIMIT_EXPIRED_EVENT_CACHES =
            "DELETE FROM event_cache WHERE id IN ( " +
                    "SELECT id FROM event_cache " +
                    "WHERE cast(created as timestamp) < cast(? as timestamp) LIMIT ?) ";

    private static final String CUSTOM_SELECT = "SELECT " +
            "id, user_id, session_id, client_correlation_id, stream_id, event_json, created, name " +
            "FROM event_cache " +
            "WHERE %s %s" +
            "ORDER BY CREATED DESC";

    private final EventCacheJdbcRepositoryConfig eventCacheJdbcRepositoryConfig;
    private final ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;
    private final PreparedStatementWrapperFactory preparedStatementWrapperFactory;

    @SuppressWarnings("squid:S1312")
    private final Logger logger;

    public EventCacheJdbcRepository(
            final EventCacheJdbcRepositoryConfig eventCacheJdbcRepositoryConfig,
            final ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider,
            final PreparedStatementWrapperFactory preparedStatementWrapperFactory,
            final Logger logger) {
        this.eventCacheJdbcRepositoryConfig = eventCacheJdbcRepositoryConfig;
        this.viewStoreJdbcDataSourceProvider = viewStoreJdbcDataSourceProvider;
        this.preparedStatementWrapperFactory = preparedStatementWrapperFactory;
        this.logger = logger;
    }

    public void removeExpiredEventCaches(final ZonedDateTime before) {
        logger.trace("Removing expired Event Caches");

        final DataSource dataSource = viewStoreJdbcDataSourceProvider.getDataSource();

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(DELETE_WITH_LIMIT_EXPIRED_EVENT_CACHES)) {
            ps.setString(1, ZonedDateTimes.toString(before));
            ps.setInt(2, eventCacheJdbcRepositoryConfig.getBatchSize());
            while (ps.executeUpdate() != 0) {
                logger.info("Removed expired Event Caches");
            }
        } catch (final SQLException e) {
            logger.info("Exception while removing EventCaches", e);
        }

        logger.info("Removed expired Event Caches");
    }

    public List<EventCache> queryByFilter(final String filterClause, final Optional<String> clientCorrelationId) {

        final DataSource dataSource = viewStoreJdbcDataSourceProvider.getDataSource();
        final String clientCorrelationWhereClause = clientCorrelationId.isPresent() ? " and client_correlation_id = ? " : "";
        final String query = format(CUSTOM_SELECT, filterClause, clientCorrelationWhereClause);
        try (final PreparedStatementWrapper ps = preparedStatementWrapperFactory.preparedStatementWrapperOf(dataSource, query)) {
            if (clientCorrelationId.isPresent()) {
                ps.setString(1, clientCorrelationId.get());
            }
            return extractResults(ps.executeQuery());
        } catch (SQLException e) {
            throw new JdbcRepositoryException(format("Exception while returning filtered events {%s}", filterClause), e);
        }
    }

    private List<EventCache> extractResults(final ResultSet resultSet) throws SQLException {
        final List<EventCache> events = new ArrayList<>();

        while (resultSet.next()) {
            events.add(entityFrom(resultSet));
        }

        return events;
    }

    private EventCache entityFrom(final ResultSet resultSet) throws SQLException {
        final UUID id = (UUID) resultSet.getObject("id");
        final UUID userId = (UUID) resultSet.getObject("user_id");
        final UUID sessionId = (UUID) resultSet.getObject("session_id");
        final String correlationId = resultSet.getString("client_correlation_id");
        final String eventName = resultSet.getString("name");
        final UUID streamId = (UUID) resultSet.getObject("stream_id");
        final String eventJson = resultSet.getString("event_json");
        final Timestamp created = resultSet.getTimestamp("created");
        return new EventCache(
                id,
                userId,
                sessionId,
                correlationId,
                streamId,
                eventJson,
                fromSqlTimestamp(created),
                eventName);
    }
}
