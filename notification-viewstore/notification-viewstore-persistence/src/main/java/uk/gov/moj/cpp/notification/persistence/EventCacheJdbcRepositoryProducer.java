package uk.gov.moj.cpp.notification.persistence;

import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class EventCacheJdbcRepositoryProducer {

    @Inject
    private EventCacheJdbcRepositoryConfig eventCacheJdbcRepositoryConfig;

    @Inject
    private ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;

    @Inject
    private PreparedStatementWrapperFactory preparedStatementWrapperFactory;


    @Produces
    public EventCacheJdbcRepository eventCacheJdbcRepository() {

        return new EventCacheJdbcRepository(
                eventCacheJdbcRepositoryConfig,
                viewStoreJdbcDataSourceProvider,
                preparedStatementWrapperFactory,
                getLogger(EventCacheJdbcRepository.class)
        );
    }
}
