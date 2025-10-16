package uk.gov.moj.cpp.notification.integration.test.dataaccess;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.nio.charset.Charset;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for setting stubs.
 */
public class WireMockStubUtils {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int HTTP_STATUS_OK = 200;
    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockStubUtils.class);

    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void setupUserAsSystemUser(String userId) {
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/" + userId + "/groups"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user.json"))));
    }

    public static void stubUserWithPermission(final String userId) {
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, userId)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(getPayload("stub-data/usersgroups.user-permissions.json"))));
    }

    public static void stubUserWithNoPermission(final String userId) {
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, userId)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(getPayload("stub-data/usersgroups.user-no-permissions.json"))));
    }

    public static void stubAA(final String userId) {
        ///subscriptions/{subscriptionId}/events
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/logged-in-user/permissions"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, userId)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(getPayload("stub-data/usersgroups.user-no-permissions.json"))));
    }

    public static void setupUserAsNormalUser(String userId) {
        InternalEndpointMockUtils.stubPingFor("usersgroups-service");
        stubFor(get(urlPathEqualTo("/usersgroups-service/query/api/rest/usersgroups/users/" + userId + "/groups"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(getPayload("stub-data/usersgroups.get-groups-by-user-for-normal-user.json"))));
    }

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (Exception e) {
            LOGGER.error(format("Error consuming file from location {}", path), e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }
}
