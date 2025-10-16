package uk.gov.moj.cpp.notification.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.getValueOfField;

import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class EventCacheJdbcRepositoryProducerTest {

    @Mock
    private EventCacheJdbcRepositoryConfig eventCacheJdbcRepositoryConfig;

    @Mock
    private ViewStoreJdbcDataSourceProvider viewStoreJdbcDataSourceProvider;

    @Mock
    private PreparedStatementWrapperFactory preparedStatementWrapperFactory;

    @InjectMocks
    private EventCacheJdbcRepositoryProducer eventCacheJdbcRepositoryProducer;

    @Test
    public void shouldCreateNewEventCacheJdbcRepository() throws Exception {

        final EventCacheJdbcRepository eventCacheJdbcRepository = eventCacheJdbcRepositoryProducer.eventCacheJdbcRepository();

        assertThat(getValueOfField(eventCacheJdbcRepository, "eventCacheJdbcRepositoryConfig", EventCacheJdbcRepositoryConfig.class), is(eventCacheJdbcRepositoryConfig));
        assertThat(getValueOfField(eventCacheJdbcRepository, "viewStoreJdbcDataSourceProvider", ViewStoreJdbcDataSourceProvider.class), is(viewStoreJdbcDataSourceProvider));
        assertThat(getValueOfField(eventCacheJdbcRepository, "preparedStatementWrapperFactory", PreparedStatementWrapperFactory.class), is(preparedStatementWrapperFactory));
        assertThat(getValueOfField(eventCacheJdbcRepository, "logger", Logger.class), is(notNullValue()));
    }
}
