package uk.gov.moj.cpp.notification.aggregate;

import uk.gov.moj.cpp.platform.test.serializable.AggregateSerializableChecker;

import org.junit.jupiter.api.Test;

public class AggregateSerializationTest {

    private AggregateSerializableChecker aggregateSerializableChecker = new AggregateSerializableChecker();

    @Test
    public void shouldCheckAggregatesAreSerializable() {
        final String packageName = "uk.gov.moj.cpp.notification.aggregate";

        aggregateSerializableChecker.checkAggregatesIn(packageName);
    }
}