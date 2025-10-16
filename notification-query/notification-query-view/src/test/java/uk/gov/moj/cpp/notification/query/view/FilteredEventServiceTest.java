package uk.gov.moj.cpp.notification.query.view;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.notification.common.FieldNames.USER_ID;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;

import uk.gov.moj.cpp.notification.persistence.EventCacheJdbcRepository;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.util.List;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class FilteredEventServiceTest {

    @Mock
    private EventCacheJdbcRepository publicEventJdbcRepository;

    @InjectMocks
    private FilteredEventService filteredEventService;

    @Test
    public void shouldFindEventsByQueryFilterWithoutCorrelationId() {
        final JsonObject filter = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", USER_ID.name())
                .add("value", "testUser")
                .add("operation", EQUALS.name())
                .build();

        final List<EventCache> publicEvents = singletonList(mock(EventCache.class));

        final Optional<String> optionalCorrelationId = Optional.empty();
        when(publicEventJdbcRepository.queryByFilter("USER_ID = 'testUser'", optionalCorrelationId)).thenReturn(publicEvents);

        final List<EventCache> events = filteredEventService.findEventsBy(filter, optionalCorrelationId);

        verify(publicEventJdbcRepository).queryByFilter("USER_ID = 'testUser'", optionalCorrelationId);
        assertThat(events, is(publicEvents));
    }

    @Test
    public void shouldFindEventsByQueryFilterUsingClientCorrelationId() {
        final JsonObject filter = Json.createObjectBuilder()
                .add("type", FIELD.name())
                .add("name", USER_ID.name())
                .add("value", "testUser")
                .add("operation", EQUALS.name())
                .build();

        final List<EventCache> publicEvents = singletonList(mock(EventCache.class));

        final Optional<String> optionalCorrelationId = Optional.of(randomUUID().toString());
        when(publicEventJdbcRepository.queryByFilter("USER_ID = 'testUser'", optionalCorrelationId)).thenReturn(publicEvents);

        final List<EventCache> events = filteredEventService.findEventsBy(filter, optionalCorrelationId);

        verify(publicEventJdbcRepository).queryByFilter("USER_ID = 'testUser'", optionalCorrelationId);
        assertThat(events, is(publicEvents));
    }
}
