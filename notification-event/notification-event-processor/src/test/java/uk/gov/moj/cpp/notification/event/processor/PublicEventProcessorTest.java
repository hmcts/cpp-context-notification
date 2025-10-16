package uk.gov.moj.cpp.notification.event.processor;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.notification.event.processor.converter.EventConverter;
import uk.gov.moj.cpp.notification.persistence.EventCacheRepository;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class PublicEventProcessorTest {

    @Mock
    private EventConverter converter;

    @Mock
    private JsonEnvelope event;

    @Mock
    private EventCacheRepository publicEventRepository;

    @Mock
    private EventCache eventCache;

    @Mock
    private Logger logger;

    @InjectMocks
    private PublicEventProcessor publicEventProcessor;

    @BeforeEach
    public void setUp() throws Exception {
        eventCache = new EventCache(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"eventname",UUID.randomUUID(),"eventname", ZonedDateTime.now(),"name");
    }

    @Test
    public void shouldPersistNewPublicEvent() {

        when(converter.convert(event)).thenReturn(eventCache);

        publicEventProcessor.handle(event);

        verify(publicEventRepository).save(eventCache);
    }

    @Test
    public void shouldNotPersistNewPublicEvent() {

        final Throwable runtimeException = new RuntimeException();
        when(converter.convert(event)).thenThrow(runtimeException);
        when(event.toString()).thenReturn("envelope-json");

        publicEventProcessor.handle(event);

        verify(publicEventRepository, times(0)).save(eventCache);
        verify(logger).error("Could not save public event to event cache: envelope-json", runtimeException);
    }

}
