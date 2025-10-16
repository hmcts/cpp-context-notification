package uk.gov.moj.cpp.notification.query.view;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class EventCacheListConverter implements Converter<List<EventCache>, JsonObject> {

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    @Override
    public JsonObject convert(List<EventCache> events) {

        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();

        events.forEach(event -> {
            jsonArrayBuilder.add(stringToJsonObjectConverter
                    .convert(event.getEventJson()));
        });

        return createObjectBuilder()
                .add("events", jsonArrayBuilder.build())
                .build();
    }

}
