package uk.gov.moj.cpp.notification.event.processor;

import static java.lang.Long.parseLong;

import uk.gov.justice.services.common.configuration.Value;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class SubscriptionCleanerScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCleanerScheduler.class);
    private static final String TIMER_TIMEOUT_INFO = "SubscriptionCleanerScheduler timer triggered.";

    @Inject
    @Value(key = "subscriptionCleanerScheduleInitialDelayInMillis", defaultValue = "600000")
    private String subscriptionCleanerScheduleInitialDelayInMillis;

    @Inject
    @Value(key = "subscriptionCleanerScheduleIntervalInMillis", defaultValue = "600000")
    private String subscriptionCleanerScheduleIntervalInMillis;

    @Inject
    private SubscriptionCleanerService subscriptionCleanerService;

    @Resource
    private TimerService timerService;

    @PostConstruct
    public void init() {
        timerService.createIntervalTimer(parseLong(subscriptionCleanerScheduleInitialDelayInMillis),
                parseLong(subscriptionCleanerScheduleIntervalInMillis), new TimerConfig(TIMER_TIMEOUT_INFO, false));
    }

    @Timeout
    @AccessTimeout(value = 5, unit = TimeUnit.MINUTES)
    public void unsubscribeExpiredSubscriptions() {
        LOGGER.info("Started un-subscribe expired subscriptions");
        subscriptionCleanerService.unsubscribeExpiredSubscriptions();
        LOGGER.info("Finished cleaning expired EventCaches");
    }
}
