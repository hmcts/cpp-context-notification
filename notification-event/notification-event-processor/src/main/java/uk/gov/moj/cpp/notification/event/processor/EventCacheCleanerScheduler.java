package uk.gov.moj.cpp.notification.event.processor;

import static java.lang.Long.parseLong;

import uk.gov.justice.services.common.configuration.Value;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.inject.Inject;

import org.slf4j.Logger;

@Singleton
@Startup
public class EventCacheCleanerScheduler {

    static final String TIMER_TIMEOUT_INFO = "EventCacheCleanerScheduler timer triggered.";

    @Inject
    @Value(key = "eventCacheCleanerSchedulerInitialDelayMillis", defaultValue = "30000")
    String eventCacheCleanerSchedulerInitialDelayMillis;

    @Inject
    @Value(key = "eventCacheCleanerSchedulerIntervalMillis", defaultValue = "120000")
    String eventCacheCleanerSchedulerIntervalMillis;

    @Inject
    EventCacheCleaner eventCacheCleaner;

    @Inject
    Logger logger;

    @Resource
    TimerService timerService;

    @PostConstruct
    public void init() {

        timerService.getTimers()
                .stream()
                .filter(timer -> timer.getInfo().equals(TIMER_TIMEOUT_INFO) && timer.isPersistent())
                .forEach(Timer::cancel);

        timerService.createIntervalTimer(parseLong(eventCacheCleanerSchedulerInitialDelayMillis),
                parseLong(eventCacheCleanerSchedulerIntervalMillis), new TimerConfig(TIMER_TIMEOUT_INFO, false));
    }

    @Timeout
    public void removeExpiredEventCaches() {
        logger.info("Started cleaning expired EventCaches");
        eventCacheCleaner.removeExpiredEventCaches();
        logger.info("Finished cleaning expired EventCaches");
    }
}
