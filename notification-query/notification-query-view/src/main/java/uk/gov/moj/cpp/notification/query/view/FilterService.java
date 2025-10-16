package uk.gov.moj.cpp.notification.query.view;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.json.JsonParser;
import uk.gov.moj.cpp.notification.persistence.SubscriptionRepository;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class FilterService {

    public static final String SUBSCRIPTION_ID_PROPERTY_NAME = "subscriptionId";
    public static final String CLIENT_CORRELATION_ID_PROPERTY_NAME = "clientCorrelationId";

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Inject
    JsonParser jsonParser;

    @Inject
    StringToJsonObjectConverter converter;

    public Optional<Filter> findFilter(final UUID subscriptionId) {

        final Subscription subscription = subscriptionRepository.findBy(subscriptionId);

        if (null != subscription) {
            final String filterJson = subscription.getFilter();
            final Filter filter = jsonParser.toObject(filterJson, Filter.class);

            return Optional.of(filter);
        }

        return Optional.empty();
    }

    public Optional<JsonObject> findJsonFilter(final UUID subscriptionId) {

        final Subscription subscription = subscriptionRepository.findBy(subscriptionId);

        if (null != subscription) {
            final String filterJson = subscription.getFilter();

            return Optional.of(converter.convert(filterJson));
        }

        return Optional.empty();
    }
}
