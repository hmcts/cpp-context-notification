package uk.gov.moj.cpp.notification.integration.test;


import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.notification.integration.test.dataaccess.EventCachePoller;
import uk.gov.moj.cpp.notification.persistence.entity.EventCache;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PublicEventProcessorIntegrationTest extends BaseIT {

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private final EventCachePoller eventCachePoller = new EventCachePoller();

    @BeforeEach
    public void cleanTheDatabase() throws Exception {
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "event_cache");
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanEventLogTable(CONTEXT_NAME);
    }

    @Test
    public void shouldProcessPublicEventAndRaiseANotificationAddedEventInEventStore() throws Exception {
        final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();
        final String clientCorrelationId = STRING.next();
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();
        final UUID streamId = randomUUID();
        final String eventName = "public.listing.hearing-changes-saved";

        final JsonObject payload = createObjectBuilder()
                .add("exampleField", "Example Value")
                .build();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID(eventName)
                        .withClientCorrelationId(clientCorrelationId)
                        .withSessionId(sessionId.toString())
                        .withUserId(userId.toString())
                        .withStreamId(streamId)
                        .build(),
                payload
        );

        publicMessageProducerClient.sendMessage(eventName, event);

        final List<EventCache> eventCaches = eventCachePoller.pollByUserIdUntilFound(userId);

        assertThat(eventCaches.size(), is(1));
        assertThat(eventCaches.get(0).getUserId(), is(userId));
        assertThat(eventCaches.get(0).getSessionId(), is(sessionId));
        assertThat(eventCaches.get(0).getClientCorrelationId(), is(clientCorrelationId));
        assertThat(eventCaches.get(0).getStreamId(), is(streamId));
        assertThat(eventCaches.get(0).getName(), is(eventName));

        final String eventJson = eventCaches.get(0).getEventJson();

        with(eventJson)
                .assertThat("$.exampleField", is("Example Value"));
    }
}
