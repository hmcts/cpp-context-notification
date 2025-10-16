package uk.gov.moj.cpp.notification.query.view;

import static uk.gov.moj.cpp.notification.query.view.FilterParser.parse;

import uk.gov.moj.cpp.notification.persistence.EventCacheJdbcRepository;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public class FilteredEventService {

    @Inject
    EventCacheJdbcRepository eventCacheJdbcRepository;

    public List<EventCache> findEventsBy(final JsonObject filter, Optional<String> clientCorrelationId) {
        return eventCacheJdbcRepository.queryByFilter(parse(filter), clientCorrelationId);
    }
}
