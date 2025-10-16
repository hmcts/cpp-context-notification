package uk.gov.moj.cpp.notification.query.api.accesscontrol;

import static javax.json.Json.createObjectBuilder;

public class RuleConstants {

    static final String ACTION = "action";
    static final String OBJECT = "object";
    static final String NON_CPS_DOCUMENT = "NonCpsDocument";
    static final String VIEW = "View";
    static final String UPLOAD = "Upload";
    static final String DOWNLOAD = "Download";

    private RuleConstants() {
    }

    public static String[] getNonCpsDocumentPermission() {
        return new String[]{
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, VIEW).build().toString(),
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, UPLOAD).build().toString(),
                createObjectBuilder().add(OBJECT, NON_CPS_DOCUMENT).add(ACTION, DOWNLOAD).build().toString()};
    }

}
