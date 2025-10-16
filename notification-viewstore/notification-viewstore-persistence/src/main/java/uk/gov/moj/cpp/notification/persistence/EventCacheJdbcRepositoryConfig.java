package uk.gov.moj.cpp.notification.persistence;

import static java.lang.Integer.parseInt;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

public class EventCacheJdbcRepositoryConfig {

    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    @Inject
    @Value(key = "batchSize", defaultValue = "100")
    private String batchSize;

    public int getBatchSize() {
        return parseInt(batchSize);
    }
}
