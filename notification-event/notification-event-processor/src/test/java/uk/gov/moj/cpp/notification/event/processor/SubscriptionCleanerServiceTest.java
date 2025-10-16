package uk.gov.moj.cpp.notification.event.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.notification.event.processor.SubscriptionCleanerService.SUBSCRIPTION_QUERY;
import static uk.gov.moj.cpp.notification.event.processor.SubscriptionCleanerService.UNSUBSCRIBE_COMMAND;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class SubscriptionCleanerServiceTest {

    @Mock
    Logger logger;

    @Mock
    Requester requester;

    @Mock
    Sender sender;

    @Mock
    JsonEnvelope expiredSubscriptionsQuery;

    @InjectMocks
    @Spy
    private SubscriptionCleanerService subscriptionCleanerService;

    @Test
    public void shouldUnSubscribeExpiredSubscriptions() {
        final JsonEnvelope expiredSubscriptionsQuery = envelope()
                .with(metadataWithRandomUUID(SUBSCRIPTION_QUERY))
                .build();

        when(subscriptionCleanerService.getExpiredSubscriptionEnvelope()).thenReturn(expiredSubscriptionsQuery);

        final UUID subscriptionId1 = randomUUID();

        final UUID subscriptionId2 = randomUUID();

        final JsonObjectBuilder json1 = Json.createObjectBuilder();
        json1.add("subscriptionId", subscriptionId1.toString());

        final JsonObjectBuilder json2 = Json.createObjectBuilder();
        json2.add("subscriptionId", subscriptionId2.toString());

        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        jsonArrayBuilder.add(json1);
        jsonArrayBuilder.add(json2);

        final JsonEnvelope expiredSubscriptionsResponse = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("notification.query.findExpired"),
                Json.createObjectBuilder()
                        .add("expiredSubscriptions", jsonArrayBuilder.build())
                        .build());

        when(requester.requestAsAdmin(expiredSubscriptionsQuery)).thenReturn(expiredSubscriptionsResponse);

        final JsonEnvelope unSubscribeCommand1 = envelope()
                .with(metadataWithRandomUUID(UNSUBSCRIBE_COMMAND))
                .withPayloadOf(subscriptionId1, "subscriptionId")
                .build();

        final JsonEnvelope unSubscribeCommand2 = envelope()
                .with(metadataWithRandomUUID(UNSUBSCRIBE_COMMAND))
                .withPayloadOf(subscriptionId2, "subscriptionId")
                .build();

        when(subscriptionCleanerService.getUnsubscribeCommand(subscriptionId1)).thenReturn(unSubscribeCommand1);
        when(subscriptionCleanerService.getUnsubscribeCommand(subscriptionId2)).thenReturn(unSubscribeCommand2);

        subscriptionCleanerService.unsubscribeExpiredSubscriptions();

        verify(requester).requestAsAdmin(expiredSubscriptionsQuery);

        verify(sender).sendAsAdmin(unSubscribeCommand1);
        verify(sender).sendAsAdmin(unSubscribeCommand2);
    }
}