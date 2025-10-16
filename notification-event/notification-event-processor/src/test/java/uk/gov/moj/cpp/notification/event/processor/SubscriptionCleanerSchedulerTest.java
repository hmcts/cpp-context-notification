package uk.gov.moj.cpp.notification.event.processor;

import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
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

@ExtendWith(MockitoExtension.class)
public class SubscriptionCleanerSchedulerTest {

    private static final String TIMER_TIMEOUT_INFO = "SubscriptionCleanerScheduler timer triggered.";

    @Mock
    private SubscriptionCleanerService subscriptionCleanerService;

    @Mock
    private TimerService timerService;

    @Mock
    private TimerImpl timer;

    @InjectMocks
    private SubscriptionCleanerScheduler subscriptionCleanerScheduler;

    @Captor
    private ArgumentCaptor<TimerConfig> timerConfigArgumentCaptor;

    @Test
    public void shouldCleanSubscriptions() {
        assertMethodWithAnnotation(SubscriptionCleanerScheduler.class.getDeclaredMethods(), "unsubscribeExpiredSubscriptions", Timeout.class);

        subscriptionCleanerScheduler.unsubscribeExpiredSubscriptions();

        verify(subscriptionCleanerService).unsubscribeExpiredSubscriptions();
    }

    @Test
    public void shouldCreateTimerOnInit() throws IllegalAccessException {
        final long initialDelayInMillis = 1000L;
        final long intervalInMillis = 5000L;
        ReflectionUtil.setField(subscriptionCleanerScheduler, "subscriptionCleanerScheduleInitialDelayInMillis", valueOf(initialDelayInMillis));
        ReflectionUtil.setField(subscriptionCleanerScheduler, "subscriptionCleanerScheduleIntervalInMillis", valueOf(intervalInMillis));

        assertMethodWithAnnotation(SubscriptionCleanerScheduler.class.getDeclaredMethods(), "init", PostConstruct.class);

        subscriptionCleanerScheduler.init();
        verify(timerService).createIntervalTimer(eq(initialDelayInMillis), eq(intervalInMillis), timerConfigArgumentCaptor.capture());
        assertThat(timerConfigArgumentCaptor.getValue().isPersistent(), is(false));
        assertThat(timerConfigArgumentCaptor.getValue().getInfo(), is(TIMER_TIMEOUT_INFO));
    }

    @Test
    public void shouldHaveRequiredAnnotations() {
        final Annotation[] classLevelAnnotations = SubscriptionCleanerScheduler.class.getDeclaredAnnotations();
        assertAnnotation(classLevelAnnotations, Singleton.class);
        assertAnnotation(classLevelAnnotations, Startup.class);

        final Field[] declaredFields = SubscriptionCleanerScheduler.class.getDeclaredFields();
        assertFieldWithAnnotation(declaredFields, TimerService.class, Resource.class);
    }

    private void assertFieldWithAnnotation(final Field[] declaredFields, final Class fieldClass, final Class annotationClass) {
        boolean matched = false;
        for (int i = 0; i < declaredFields.length; i++) {
            final Field field = declaredFields[i];
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
        for (int i = 0; i < declaredMethods.length; i++) {
            final Method method = declaredMethods[i];
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
        for (int i = 0; i < annotations.length; i++) {
            final Annotation annotation = annotations[i];
            if (annotation.annotationType().equals(annotationClass)) {
                matched = true;
                break;
            }
        }
        assertTrue(annotations.toString(), matched);
    }
}
