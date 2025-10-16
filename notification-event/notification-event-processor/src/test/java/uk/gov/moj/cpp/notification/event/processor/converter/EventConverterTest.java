package uk.gov.moj.cpp.notification.event.processor.converter;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class EventConverterTest {

    @Mock
    private JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter;

    @Spy
    private StoppedClock clock = new StoppedClock(now());

    @Mock
    private Logger logger;

    @InjectMocks
    private EventConverter eventConverter;

    @Test
    public void shouldConvertAJsonEnvelopeToAnEventCacheEntity() throws Exception {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

        final String eventJson = "event json";
        final Metadata metadata = mock(Metadata.class);
        final UUID userId = randomUUID();
        final UUID sessionId = randomUUID();
        final String clientCorrelationId = STRING.next();
        final UUID streamId = randomUUID();
        final String eventName = STRING.next();

        when(jsonObjectEnvelopeConverter.asJsonString(jsonEnvelope)).thenReturn(eventJson);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(of(userId.toString()));
        when(metadata.sessionId()).thenReturn(of(sessionId.toString()));
        when(metadata.clientCorrelationId()).thenReturn(of(clientCorrelationId));
        when(metadata.name()).thenReturn(eventName);
        when(metadata.streamId()).thenReturn(of(streamId));

        final EventCache eventCache = eventConverter.convert(jsonEnvelope);

        assertThat(eventCache.getId(), notNullValue());
        assertThat(eventCache.getUserId(), is(userId));
        assertThat(eventCache.getSessionId(), is(sessionId));
        assertThat(eventCache.getClientCorrelationId(), is(clientCorrelationId));
        assertThat(eventCache.getStreamId(), is(streamId));
        assertThat(eventCache.getCreated(), is(clock.now()));
        assertThat(eventCache.getEventJson(), is(eventJson));
        assertThat(eventCache.getName(), is(eventName));
    }

    @Test
    public void shouldUseAnEmptyStringIfTheUserIdIsAbsent() throws Exception {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

        final String eventJson = "event json";
        final Metadata metadata = mock(Metadata.class);
        final UUID sessionId = randomUUID();
        final String clientCorrelationId = STRING.next();
        final String eventName = STRING.next();
        final UUID streamId = randomUUID();

        when(jsonObjectEnvelopeConverter.asJsonString(jsonEnvelope)).thenReturn(eventJson);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(empty());
        when(metadata.sessionId()).thenReturn(of(sessionId.toString()));
        when(metadata.clientCorrelationId()).thenReturn(of(clientCorrelationId));
        when(metadata.name()).thenReturn(eventName);
        when(metadata.streamId()).thenReturn(of(streamId));

        final EventCache eventCache = eventConverter.convert(jsonEnvelope);

        assertThat(eventCache.getUserId(), is(nullValue()));

        assertThat(eventCache.getId(), notNullValue());
        assertThat(eventCache.getSessionId(), is(sessionId));
        assertThat(eventCache.getClientCorrelationId(), is(clientCorrelationId));
        assertThat(eventCache.getName(), is(eventName));
        assertThat(eventCache.getStreamId(), is(streamId));
        assertThat(eventCache.getCreated(), is(clock.now()));
        assertThat(eventCache.getEventJson(), is(eventJson));
    }

    @Test
    public void shouldUseAnEmptyStringIfTheSessionIdIsAbsent() throws Exception {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

        final String eventJson = "event json";
        final Metadata metadata = mock(Metadata.class);
        final UUID userId = randomUUID();
        final String clientCorrelationId = STRING.next();
        final String eventName = STRING.next();
        final UUID streamId = randomUUID();

        when(jsonObjectEnvelopeConverter.asJsonString(jsonEnvelope)).thenReturn(eventJson);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(of(userId.toString()));
        when(metadata.sessionId()).thenReturn(empty());
        when(metadata.clientCorrelationId()).thenReturn(of(clientCorrelationId));
        when(metadata.streamId()).thenReturn(of(streamId));
        when(metadata.name()).thenReturn(eventName);

        final EventCache eventCache = eventConverter.convert(jsonEnvelope);

        assertThat(eventCache.getSessionId(), is(nullValue()));

        assertThat(eventCache.getId(), notNullValue());
        assertThat(eventCache.getUserId(), is(userId));
        assertThat(eventCache.getClientCorrelationId(), is(clientCorrelationId));
        assertThat(eventCache.getName(), is(eventName));
        assertThat(eventCache.getStreamId(), is(streamId));
        assertThat(eventCache.getCreated(), is(clock.now()));
        assertThat(eventCache.getEventJson(), is(eventJson));
    }

    @Test
    public void shouldUseAnEmptyStringIfTheCorrelationIdIsAbsent() throws Exception {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

        final String eventJson = "event json";
        final Metadata metadata = mock(Metadata.class);
        final UUID userId = randomUUID();
        final UUID sessionId = randomUUID();
        final UUID streamId = randomUUID();
        final String eventName = STRING.next();

        when(jsonObjectEnvelopeConverter.asJsonString(jsonEnvelope)).thenReturn(eventJson);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(of(userId.toString()));
        when(metadata.sessionId()).thenReturn(of(sessionId.toString()));
        when(metadata.clientCorrelationId()).thenReturn(empty());
        when(metadata.name()).thenReturn(eventName);
        when(metadata.streamId()).thenReturn(of(streamId));

        final EventCache eventCache = eventConverter.convert(jsonEnvelope);

        assertThat(eventCache.getClientCorrelationId(), is(nullValue()));

        assertThat(eventCache.getId(), notNullValue());
        assertThat(eventCache.getUserId(), is(userId));
        assertThat(eventCache.getSessionId(), is(sessionId));
        assertThat(eventCache.getStreamId(), is(streamId));
        assertThat(eventCache.getCreated(), is(clock.now()));
        assertThat(eventCache.getEventJson(), is(eventJson));
        assertThat(eventCache.getName(), is(eventName));
    }

    @Test
    public void shouldUseNullIfTheStreamIdIsAbsent() throws Exception {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

        final String eventJson = "event json";
        final Metadata metadata = mock(Metadata.class);
        final UUID userId = randomUUID();
        final UUID sessionId = randomUUID();
        final String clientCorrelationId = STRING.next();
        final String eventName = STRING.next();

        when(jsonObjectEnvelopeConverter.asJsonString(jsonEnvelope)).thenReturn(eventJson);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(of(userId.toString()));
        when(metadata.sessionId()).thenReturn(of(sessionId.toString()));
        when(metadata.clientCorrelationId()).thenReturn(of(clientCorrelationId));
        when(metadata.name()).thenReturn(eventName);
        when(metadata.streamId()).thenReturn(empty());

        final EventCache eventCache = eventConverter.convert(jsonEnvelope);

        assertThat(eventCache.getStreamId(), is(nullValue()));

        assertThat(eventCache.getId(), notNullValue());
        assertThat(eventCache.getUserId(), is(userId));
        assertThat(eventCache.getSessionId(), is(sessionId));
        assertThat(eventCache.getClientCorrelationId(), is(clientCorrelationId));
        assertThat(eventCache.getName(), is(eventName));
        assertThat(eventCache.getCreated(), is(clock.now()));
        assertThat(eventCache.getEventJson(), is(eventJson));
    }

    @Test
    public void shouldLogAndReturnEmptyIfTheEnvelopeCannotBeParsedToAString() throws Exception {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);

        final RuntimeException runtimeException = new RuntimeException("Ooops");

        when(jsonObjectEnvelopeConverter.asJsonString(jsonEnvelope)).thenThrow(runtimeException);

        assertThrows(RuntimeException.class, () -> eventConverter.convert(jsonEnvelope));
    }
}
