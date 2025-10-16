package uk.gov.moj.cpp.notification.common.accesscontrol;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.moj.cpp.accesscontrol.providers.Provider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Provider
@ApplicationScoped
public class SubscriptionProvider extends AbstractSubscriptionProvider {

    @Inject
    @FrameworkComponent("COMMAND_API")
    Requester requester;

    @Override
    public Requester getRequester() {
        return requester;
    }
}
