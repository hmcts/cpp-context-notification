package uk.gov.moj.cpp.notification.system.management.commands.subscriptions.clean;

import uk.gov.justice.services.jmx.api.command.BaseSystemCommand;

public class CleanSubscriptionsCommand extends BaseSystemCommand {

    public static final String CLEAN_SUBSCRIPTIONS = "CLEAN_SUBSCRIPTIONS";
    private static final String DESCRIPTION = "Unsubscribe all expired subscriptions.";

    public CleanSubscriptionsCommand() {
        super(CLEAN_SUBSCRIPTIONS, DESCRIPTION);
    }
}
