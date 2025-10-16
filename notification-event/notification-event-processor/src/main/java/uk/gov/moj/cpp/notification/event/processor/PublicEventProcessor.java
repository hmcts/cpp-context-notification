package uk.gov.moj.cpp.notification.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.notification.persistence.EventCacheRepository;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings("squid:S1312")
@ServiceComponent(EVENT_PROCESSOR)
public class PublicEventProcessor {

    @Inject
    Converter<JsonEnvelope, EventCache> converter;

    @Inject
    Logger logger;

    @Inject
    EventCacheRepository eventCacheRepository;

    /**
     * Handles all events on the public.event topic and stores in the database.
     *
     * @param event - the envelope containing the public event.
     */
    @Handles("*")
    public void handle(final JsonEnvelope event) {
        try {
            final EventCache eventCache = converter.convert(event);
            eventCacheRepository.save(converter.convert(event));

            if(Objects.nonNull(eventCache.getClientCorrelationId()) && logger.isWarnEnabled()) {
                logger.warn("saving correlationId: {}" , eventCache.getClientCorrelationId());
            }
        } catch (RuntimeException e) {
            // TODO: Replace RuntimeException with specific exception when framework is updated
            logger.error("Could not save public event to event cache: " + event, e);
        }
    }
}
