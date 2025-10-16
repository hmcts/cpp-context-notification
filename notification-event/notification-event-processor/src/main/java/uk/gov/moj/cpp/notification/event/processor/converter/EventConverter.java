package uk.gov.moj.cpp.notification.event.processor.converter;

import static java.util.UUID.randomUUID;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class EventConverter implements Converter<JsonEnvelope, EventCache> {

    @Inject
    JsonObjectEnvelopeConverter jsonObjectEnvelopeConverter;

    @Inject
    Clock clock;

    @Override
    public EventCache convert(final JsonEnvelope event) {

        final String eventJson = jsonObjectEnvelopeConverter.asJsonString(event);
        final UUID id = randomUUID();
        final Metadata metadata = event.metadata();
        final UUID userId = metadata.userId()
                .map(UUID::fromString).orElse(null);
        final UUID sessionId = metadata.sessionId()
                .map(UUID::fromString).orElse(null);
        final String clientCorrelationId = metadata.clientCorrelationId()
                .orElse(null);
        final UUID streamId = metadata.streamId().orElse(null);
        final ZonedDateTime createdAt = clock.now();
        final String eventName = event.metadata().name();

        return new EventCache(
                id,
                userId,
                sessionId,
                clientCorrelationId,
                streamId,
                eventJson,
                createdAt,
                eventName);
    }
}
