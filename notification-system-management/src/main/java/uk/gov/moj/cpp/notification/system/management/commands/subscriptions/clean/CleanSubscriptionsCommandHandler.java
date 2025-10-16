package uk.gov.moj.cpp.notification.system.management.commands.subscriptions.clean;

import static java.lang.String.format;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_COMPLETE;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_FAILED;
import static uk.gov.justice.services.jmx.api.domain.CommandState.COMMAND_IN_PROGRESS;
import static uk.gov.moj.cpp.notification.system.management.commands.subscriptions.clean.CleanSubscriptionsCommand.CLEAN_SUBSCRIPTIONS;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.jmx.api.parameters.JmxCommandRuntimeParameters;
import uk.gov.justice.services.jmx.command.HandlesSystemCommand;
import uk.gov.justice.services.jmx.state.events.SystemCommandStateChangedEvent;
import uk.gov.moj.cpp.notification.event.processor.SubscriptionCleanerService;

import java.util.UUID;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;

public class CleanSubscriptionsCommandHandler {

    @Inject
    private SubscriptionCleanerService subscriptionCleanerService;

    @Inject
    private Event<SystemCommandStateChangedEvent> systemCommandStateChangedEventFirer;

    @Inject
    private UtcClock clock;

    @SuppressWarnings("squid:S1312") //Correct logger is being injected, thus allowing testing to occur
    @Inject
    private Logger logger;

    @SuppressWarnings("squid:S2221") //Exception needs to be caught so that the command can fail gracefully
    @HandlesSystemCommand(CLEAN_SUBSCRIPTIONS)
    public void onCleanSubscriptionsRequested(final CleanSubscriptionsCommand cleanSubscriptionsCommand, final UUID commandId, final JmxCommandRuntimeParameters jsxCmdRuntimeParams) {

        final String startMessage = format("%s started", cleanSubscriptionsCommand.getName());
        logger.info(startMessage);
        systemCommandStateChangedEventFirer.fire(new SystemCommandStateChangedEvent(
                commandId,
                cleanSubscriptionsCommand,
                COMMAND_IN_PROGRESS,
                clock.now(),
                startMessage
        ));

        try {
            subscriptionCleanerService.unsubscribeExpiredSubscriptions();
            final String completeMessage = format("%s complete", cleanSubscriptionsCommand.getName());
            logger.info(completeMessage);
            systemCommandStateChangedEventFirer.fire(new SystemCommandStateChangedEvent(
                    commandId,
                    cleanSubscriptionsCommand,
                    COMMAND_COMPLETE,
                    clock.now(),
                    completeMessage
            ));


        } catch (final Exception e) {
            final String failureMessage = format("%s failed: %s: %s", cleanSubscriptionsCommand.getName(), e.getClass().getSimpleName(), e.getMessage());
            logger.error(failureMessage, e);
            systemCommandStateChangedEventFirer.fire(new SystemCommandStateChangedEvent(
                    commandId,
                    cleanSubscriptionsCommand,
                    COMMAND_FAILED,
                    clock.now(),
                    failureMessage
            ));
        }
    }
}
