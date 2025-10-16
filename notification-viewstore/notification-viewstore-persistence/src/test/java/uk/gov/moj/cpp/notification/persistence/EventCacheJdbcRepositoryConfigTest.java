package uk.gov.moj.cpp.notification.persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class EventCacheJdbcRepositoryConfigTest {


    @InjectMocks
    private EventCacheJdbcRepositoryConfig eventCacheJdbcRepositoryConfig;

    @Test
    public void shouldGetTheBatchSize() throws Exception {

        setField(eventCacheJdbcRepositoryConfig, "batchSize", "23");

        assertThat(eventCacheJdbcRepositoryConfig.getBatchSize(), is(23));
    }
}
