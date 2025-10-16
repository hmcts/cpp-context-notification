package uk.gov.moj.cpp.notification.query.view;

import static java.lang.String.valueOf;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.util.List;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class ExpiredSubscriptionsListConverter implements Converter<List<Subscription>, JsonObject> {

    final static String EXPIRED_SUBSCRIPTIONS = "expiredSubscriptions";
    final static String SUBSCRIPTION_ID = "subscriptionId";

    @Override
    public JsonObject convert(final List<Subscription> subscriptions) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        subscriptions.forEach(subscription -> {
            jsonArrayBuilder.add(createObjectBuilder().add(SUBSCRIPTION_ID, valueOf(subscription.getId())).build());
        });

        return createObjectBuilder()
                .add(EXPIRED_SUBSCRIPTIONS, jsonArrayBuilder.build())
                .build();
    }

}
