package uk.gov.moj.cpp.notification.query.view;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;
import static uk.gov.moj.cpp.notification.common.OperationType.EQUALS;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.json.DefaultJsonParser;
import uk.gov.justice.services.common.json.JsonParser;
import uk.gov.moj.cpp.notification.persistence.SubscriptionRepository;
import uk.gov.moj.cpp.notification.persistence.entity.Subscription;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class FilterServiceTest {

    private static final String FILTER_JSON =
            "{\n" +
                    "    \"type\": \"FIELD\",\n" +
                    "    \"name\": \"USER_ID\",\n" +
                    "    \"value\": \"d3690064-b74d-41b9-bcd2-1da279752a22\",\n" +
                    "    \"operation\": \"EQUALS\"\n" +
                    "}\n";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Spy
    private JsonParser jsonParser = new DefaultJsonParser();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private FilterService filterService;

    @Test
    public void shouldExtractTheSubscriptionIdFromTheEnvelopeAndUseItToFindTheFilter() throws Exception {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();
        final ZonedDateTime created = now();

        final Subscription subscription = new Subscription(subscriptionId, userId, FILTER_JSON, created);

        when(subscriptionRepository.findBy(subscriptionId)).thenReturn(subscription);

        final Optional<Filter> filter = filterService.findFilter(subscriptionId);

        assertThat(filter.get().getName(), is("USER_ID"));
        assertThat(filter.get().getValue(), is("d3690064-b74d-41b9-bcd2-1da279752a22"));
        assertThat(filter.get().getType(), is(FIELD));
        assertThat(filter.get().getOperation(), is(EQUALS));
    }

    @Test
    public void shouldReturnEmptyIfNoSubscriptionFound() throws Exception {

        final UUID subscriptionId = randomUUID();

        final Subscription subscription = null;

        when(subscriptionRepository.findBy(subscriptionId)).thenReturn(subscription);

        final Optional<Filter> filter = filterService.findFilter(subscriptionId);

        assertThat(filter.isPresent(), is(false));
    }

    @Test
    public void shouldExtractTheSubscriptionIdFromTheEnvelopeAndUseItToFindTheJsonFilter() throws Exception {

        final UUID subscriptionId = randomUUID();
        final UUID userId = randomUUID();
        final ZonedDateTime created = now();

        final Subscription subscription = new Subscription(subscriptionId, userId, FILTER_JSON, created);

        when(subscriptionRepository.findBy(subscriptionId)).thenReturn(subscription);

        final Optional<JsonObject> filter = filterService.findJsonFilter(subscriptionId);

        assertThat(filter.get().getString("name"), is("USER_ID"));
        assertThat(filter.get().getString("value"), is("d3690064-b74d-41b9-bcd2-1da279752a22"));
        assertThat(filter.get().getString("type"), is(FIELD.name()));
        assertThat(filter.get().getString("operation"), is(EQUALS.name()));
    }

}
