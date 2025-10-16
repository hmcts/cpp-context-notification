package uk.gov.moj.cpp.notification.event.processor;

import static java.lang.Long.parseLong;

import uk.gov.justice.services.common.configuration.Value;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

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
