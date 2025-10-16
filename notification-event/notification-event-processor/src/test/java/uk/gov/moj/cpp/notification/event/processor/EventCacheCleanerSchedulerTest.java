package uk.gov.moj.cpp.notification.event.processor;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.notification.event.processor.EventCacheCleanerScheduler.TIMER_TIMEOUT_INFO;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import org.apache.openejb.core.timer.TimerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class EventCacheCleanerSchedulerTest {

    @Mock
    private EventCacheCleaner eventCacheCleaner;

    @Mock
    private TimerService timerService;

    @Mock
    private Logger logger;

    @Mock
    private TimerImpl timer;

    @InjectMocks
    private EventCacheCleanerScheduler eventCacheCleanerScheduler;

    @Captor
    private ArgumentCaptor<TimerConfig> timerConfigArgumentCaptor;

    @Test
    public void shouldCleanEventCaches() throws InterruptedException {

        assertMethodWithAnnotation(EventCacheCleanerScheduler.class.getDeclaredMethods(), "removeExpiredEventCaches", Timeout.class);
        eventCacheCleanerScheduler.removeExpiredEventCaches();
        verify(eventCacheCleaner).removeExpiredEventCaches();
    }

    @Test
    public void shouldCreateTimerOnInit() {

        final long initialDelay = 1000L;
        final long interval = 5000L;
        eventCacheCleanerScheduler.eventCacheCleanerSchedulerInitialDelayMillis = valueOf(initialDelay);
        eventCacheCleanerScheduler.eventCacheCleanerSchedulerIntervalMillis = valueOf(interval);

        assertMethodWithAnnotation(EventCacheCleanerScheduler.class.getDeclaredMethods(), "init", PostConstruct.class);

        eventCacheCleanerScheduler.init();
        verify(timerService).createIntervalTimer(eq(initialDelay), eq(interval), timerConfigArgumentCaptor.capture());
        assertThat(timerConfigArgumentCaptor.getValue().isPersistent(), is(false));
        assertThat(timerConfigArgumentCaptor.getValue().getInfo(), is(TIMER_TIMEOUT_INFO));
    }

    @Test
    public void shouldRemoveAnyCurrentPersistentTimersOfTheSameType() {

        final long initialDelay = 1000L;
        final long interval = 5000L;
        final Timer timer_1 = mock(Timer.class);
        final Timer timer_2 = mock(Timer.class);
        final Timer timer_3 = mock(Timer.class);

        when(timerService.getTimers()).thenReturn(asList(timer_1, timer_2));
        when(timer_1.getInfo()).thenReturn("EventCacheCleanerScheduler timer triggered.");
        when(timer_1.isPersistent()).thenReturn(false);
        when(timer_2.getInfo()).thenReturn("EventCacheCleanerScheduler timer triggered.");
        when(timer_2.isPersistent()).thenReturn(true);

        eventCacheCleanerScheduler.eventCacheCleanerSchedulerInitialDelayMillis = valueOf(initialDelay);
        eventCacheCleanerScheduler.eventCacheCleanerSchedulerIntervalMillis = valueOf(interval);

        eventCacheCleanerScheduler.init();

        verify(timer_2).cancel();
    }

    @Test
    public void shouldHaveRequiredAnnotations() {

        final Annotation[] classLevelAnnotations = EventCacheCleanerScheduler.class.getDeclaredAnnotations();
        assertAnnotation(classLevelAnnotations, Singleton.class);
        assertAnnotation(classLevelAnnotations, Startup.class);

        final Field[] declaredFields = EventCacheCleanerScheduler.class.getDeclaredFields();
        assertFieldWithAnnotation(declaredFields, TimerService.class, Resource.class);
    }

    private void assertFieldWithAnnotation(final Field[] declaredFields, final Class fieldClass, final Class annotationClass) {
        boolean matched = false;
        for (final Field field : declaredFields) {
            if (field.getType().equals(fieldClass)) {
                assertAnnotation(field.getAnnotations(), annotationClass);
                matched = true;
                break;
            }
        }
        assertTrue(matched);
    }

    private void assertMethodWithAnnotation(final Method[] declaredMethods, final String methodName, final Class annotationClass) {
        boolean matched = false;
        for (final Method method : declaredMethods) {
            if (method.getName().equals(methodName)) {
                assertAnnotation(method.getDeclaredAnnotations(), annotationClass);
                matched = true;
                break;
            }
        }
        assertTrue(matched);
    }

    private void assertAnnotation(final Annotation[] annotations, final Class annotationClass) {
        boolean matched = false;
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType().equals(annotationClass)) {
                matched = true;
                break;
            }
        }
        assertTrue(Arrays.toString(annotations), matched);
    }

}
