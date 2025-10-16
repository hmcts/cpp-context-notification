package uk.gov.moj.cpp.notification.query.view;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EventCacheListConverterTest {

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private EventCacheListConverter eventCacheListConverter;

    @Test
    public void shouldConvertTheListOfEventCachesToAJsonArray() throws Exception {

        final EventCache publicEvent_1 = randomEventCache(1);
        final EventCache publicEvent_2 = randomEventCache(2);

        final JsonObject jsonObject = eventCacheListConverter.convert(asList(publicEvent_1, publicEvent_2));

        final JsonArray events = jsonObject.getJsonArray("events");

        assertThat(events.size(), is(2));
        assertThat(events.getJsonObject(0).getString("propertyName_1"), is("propertyValue_1"));
        assertThat(events.getJsonObject(1).getString("propertyName_2"), is("propertyValue_2"));
    }

    private EventCache randomEventCache(int seed) {

        return new EventCache(
                randomUUID(),
                randomUUID(),
                randomUUID(),
                STRING.next(),
                randomUUID(),
                randomJson(seed),
                PAST_LOCAL_DATE.next().atStartOfDay(UTC),
                STRING.next());
    }

    private String randomJson(int seed) {

        return "{\"propertyName_" + seed + "\": \"propertyValue_" + seed + "\"}";
    }
}
