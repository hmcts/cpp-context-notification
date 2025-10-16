package uk.gov.moj.cpp.notification.query.view;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.UuidStringMatcher.isAUuid;
import static uk.gov.moj.cpp.notification.query.view.ExpiredSubscriptionsListConverter.EXPIRED_SUBSCRIPTIONS;
import static uk.gov.moj.cpp.notification.query.view.ExpiredSubscriptionsListConverter.SUBSCRIPTION_ID;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ExpiredSubscriptionsListConverterTest {

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private ExpiredSubscriptionsListConverter expiredSubscriptionsListConverter;

    @Test
    public void shouldConvertTheListOfEventCachesToAJsonArray() throws Exception {
        int seed = 100;

        final JsonObject jsonObject = expiredSubscriptionsListConverter.convert(generateSubscriptions(seed));

        final JsonArray expiredSubscriptions = jsonObject.getJsonArray(EXPIRED_SUBSCRIPTIONS);

        assertThat(expiredSubscriptions.size(), is(seed));

        for (int i=0; i<seed; i++) {
            String subscriptionId = expiredSubscriptions.getJsonObject(i).getJsonString(SUBSCRIPTION_ID).getString();
            assertThat(subscriptionId, isAUuid());
        }
    }

    private Subscription createSubscription(int seed) {
        return new Subscription(
                randomUUID(),
                randomUUID(),
                "" + seed,
                ZonedDateTime.now()
        );
    }

    private List<Subscription> generateSubscriptions(int seed) {
        List<Subscription> subscriptions = new ArrayList<>();
        for (int i = 0; i < seed; i++) {
            subscriptions.add(createSubscription(i));
        }
        return subscriptions;
    }
}
