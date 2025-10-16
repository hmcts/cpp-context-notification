package uk.gov.moj.cpp.notification.system.management.commands.subscriptions.clean;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_COMPLETE;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_FAILED;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_IN_PROGRESS;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jmx.state.events.SystemCommandStateChangedEvent;
import uk.gov.moj.cpp.notification.event.processor.SubscriptionCleanerService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.enterprise.event.Event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class CleanSubscriptionsCommandHandlerTest {

    @Mock
    private SubscriptionCleanerService subscriptionCleanerService;

    @Mock
    private Event<SystemCommandStateChangedEvent> systemCommandStateChangedEventFirer;

    @Mock
    private UtcClock clock;

    @Mock
    private Logger logger;

    @Captor
    private ArgumentCaptor<SystemCommandStateChangedEvent> systemCommandStateChangedEventCaptor;
    
    @InjectMocks
    private CleanSubscriptionsCommandHandler cleanSubscriptionsCommandHandler;

    @Test
    public void shouldRunCleanSubscriptionsAndFireLifecycleEvents() throws Exception {

        final UUID commandId = randomUUID();
        final ZonedDateTime startTime = new UtcClock().now();
        final ZonedDateTime endTime = startTime.plusSeconds(2);
        final CleanSubscriptionsCommand cleanSubscriptionsCommand = new CleanSubscriptionsCommand();

        when(clock.now()).thenReturn(startTime, endTime);

        cleanSubscriptionsCommandHandler.onCleanSubscriptionsRequested(cleanSubscriptionsCommand, commandId, null);

        final InOrder inOrder = inOrder(logger, systemCommandStateChangedEventFirer, subscriptionCleanerService);

        inOrder.verify(logger).info("CLEAN_SUBSCRIPTIONS started");
        inOrder.verify(systemCommandStateChangedEventFirer).fire(systemCommandStateChangedEventCaptor.capture());
        inOrder.verify(subscriptionCleanerService).unsubscribeExpiredSubscriptions();
        inOrder.verify(logger).info("CLEAN_SUBSCRIPTIONS complete");
        inOrder.verify(systemCommandStateChangedEventFirer).fire(systemCommandStateChangedEventCaptor.capture());

        final List<SystemCommandStateChangedEvent> events = systemCommandStateChangedEventCaptor.getAllValues();

        assertThat(events.size(), is(2));

        assertThat(events.get(0).getCommandId(), is(commandId));
        assertThat(events.get(0).getSystemCommand(), is(cleanSubscriptionsCommand));
        assertThat(events.get(0).getCommandState(), is(COMMAND_IN_PROGRESS));
        assertThat(events.get(0).getStatusChangedAt(), is(startTime));
        assertThat(events.get(0).getMessage(), is("CLEAN_SUBSCRIPTIONS started"));

        assertThat(events.get(1).getCommandId(), is(commandId));
        assertThat(events.get(1).getSystemCommand(), is(cleanSubscriptionsCommand));
        assertThat(events.get(1).getCommandState(), is(COMMAND_COMPLETE));
        assertThat(events.get(1).getStatusChangedAt(), is(endTime));
        assertThat(events.get(1).getMessage(), is("CLEAN_SUBSCRIPTIONS complete"));
    }

    @Test
    public void shouldFireTheCorrectFailureEventIfCleaningSubscriptionsFails() throws Exception {

        final NullPointerException nullPointerException = new NullPointerException("Oops");

        final UUID commandId = randomUUID();
        final ZonedDateTime startTime = new UtcClock().now();
        final ZonedDateTime endTime = startTime.plusSeconds(2);
        final CleanSubscriptionsCommand cleanSubscriptionsCommand = new CleanSubscriptionsCommand();

        when(clock.now()).thenReturn(startTime, endTime);

        doThrow(nullPointerException).when(subscriptionCleanerService).unsubscribeExpiredSubscriptions();

        cleanSubscriptionsCommandHandler.onCleanSubscriptionsRequested(cleanSubscriptionsCommand, commandId, null);

        final InOrder inOrder = inOrder(logger, systemCommandStateChangedEventFirer, subscriptionCleanerService);

        inOrder.verify(logger).info("CLEAN_SUBSCRIPTIONS started");
        inOrder.verify(systemCommandStateChangedEventFirer).fire(systemCommandStateChangedEventCaptor.capture());
        inOrder.verify(subscriptionCleanerService).unsubscribeExpiredSubscriptions();
        inOrder.verify(logger).error("CLEAN_SUBSCRIPTIONS failed: NullPointerException: Oops", nullPointerException);
        inOrder.verify(systemCommandStateChangedEventFirer).fire(systemCommandStateChangedEventCaptor.capture());

        final List<SystemCommandStateChangedEvent> events = systemCommandStateChangedEventCaptor.getAllValues();

        assertThat(events.size(), is(2));

        assertThat(events.get(0).getCommandId(), is(commandId));
        assertThat(events.get(0).getSystemCommand(), is(cleanSubscriptionsCommand));
        assertThat(events.get(0).getCommandState(), is(COMMAND_IN_PROGRESS));
        assertThat(events.get(0).getStatusChangedAt(), is(startTime));
        assertThat(events.get(0).getMessage(), is("CLEAN_SUBSCRIPTIONS started"));

        assertThat(events.get(1).getCommandId(), is(commandId));
        assertThat(events.get(1).getSystemCommand(), is(cleanSubscriptionsCommand));
        assertThat(events.get(1).getCommandState(), is(COMMAND_FAILED));
        assertThat(events.get(1).getStatusChangedAt(), is(endTime));
        assertThat(events.get(1).getMessage(), is("CLEAN_SUBSCRIPTIONS failed: NullPointerException: Oops"));
    }
}
