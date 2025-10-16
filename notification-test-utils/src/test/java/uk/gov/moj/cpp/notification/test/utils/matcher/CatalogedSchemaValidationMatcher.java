package uk.gov.moj.cpp.notification.test.utils.matcher;

import static uk.gov.justice.schema.catalog.test.utils.SchemaCatalogResolver.schemaCatalogResolver;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONObject;

public class CatalogedSchemaValidationMatcher {
    private static final Random random = new Random();
    private static final String JSON_SCHEMA_TEMPLATE = "json/schema/%s.json";
    private static final String RAML_JSON_SCHEMA_TEMPLATE = "raml/json/schema/%s.json";


    public CatalogedSchemaValidationMatcher() {
    }

    public static Matcher<String> isValidForSchema(final String pathToJsonSchema) {
        return new TypeSafeMatcher<String>() {

            private String pathToJsonFile;
            private ValidationException exception = null;

            protected boolean matchesSafely(final String pathToJsonFile) {
                this.pathToJsonFile = pathToJsonFile;

                try {
                    getJsonSchemaFor(pathToJsonSchema).validate(getJsonObjectFor(pathToJsonFile));
                    return true;
                } catch (ValidationException var3) {
                    this.exception = var3;
                    return false;
                } catch (IOException var4) {
                    throw new IllegalArgumentException(var4);
                }
            }

            public void describeTo(Description description) {
                description.appendText("json file ").appendValue(this.pathToJsonFile).appendText(" to validate against schema ").appendValue(pathToJsonSchema);
            }

            protected void describeMismatchSafely(String pathToJsonFile, Description mismatchDescription) {
                mismatchDescription.appendText("validation failed with message ").appendValue(this.exception.toJSON());
            }
        };
    }

    public static Matcher<JsonEnvelope> isValidJsonEnvelopeForSchema() {
        return new TypeSafeDiagnosingMatcher<JsonEnvelope>() {
            private ValidationException validationException = null;

            protected boolean matchesSafely(JsonEnvelope jsonEnvelope, Description description) {
                if (null == this.validationException) {
                    try {
                        String e = String.format(JSON_SCHEMA_TEMPLATE, new Object[]{jsonEnvelope.metadata().name()});
                        getJsonSchemaFor(e).validate(new JSONObject(jsonEnvelope.payloadAsJsonObject().toString()));
                    } catch (IOException | IllegalArgumentException var6) {
                        try {
                            String ioe = String.format(RAML_JSON_SCHEMA_TEMPLATE, new Object[]{jsonEnvelope.metadata().name()});
                            getJsonSchemaFor(ioe).validate(new JSONObject(jsonEnvelope.payloadAsJsonObject().toString()));
                        } catch (IOException var5) {
                            throw new IllegalArgumentException(var5);
                        }
                    } catch (ValidationException var7) {
                        this.validationException = var7;
                        return false;
                    }

                    return true;
                } else {
                    description.appendText("Schema validation failed with message: ").appendValue(this.validationException.getMessage());
                    return false;
                }
            }

            public void describeTo(Description description) {
                description.appendText("JsonEnvelope validated against schema found on classpath at \'raml/json/schema/\' ");
            }
        };
    }


    public static Matcher<String> failsValidationWithMessage(final String pathToJsonSchema, final String errorMessage) {
        return new TypeSafeMatcher<String>() {
            private String pathToJsonFile;
            private ValidationException exception = null;

            protected boolean matchesSafely(String pathToJsonFile) {
                this.pathToJsonFile = pathToJsonFile;

                try {
                    getJsonSchemaFor(pathToJsonSchema).validate(getJsonObjectFor(pathToJsonFile));
                    return false;
                } catch (ValidationException validationException) {
                    this.exception = validationException;
                    return validationException.getMessage().equals(errorMessage);
                } catch (IOException ioException) {
                    throw new IllegalArgumentException(ioException);
                }
            }

            public void describeTo(Description description) {
                if (this.exception == null) {
                    description.appendText("json file ").appendValue(this.pathToJsonFile).appendText(" to fail validation against schema ").appendValue(pathToJsonSchema);
                } else {
                    description.appendText("json file ").appendValue(this.pathToJsonFile).appendText(" to fail validation against schema ").appendValue(pathToJsonSchema).appendText(" with message ").appendValue(errorMessage);
                }

            }

            protected void describeMismatchSafely(String pathToJsonFile, Description mismatchDescription) {
                if (this.exception == null) {
                    mismatchDescription.appendText("validation passed");
                } else {
                    mismatchDescription.appendText("validation failed with message ").appendValue(this.exception.toJSON());
                }

            }
        };
    }


    public static Matcher<String> failsValidationForAnyMissingField(final String pathToJsonSchema) {
        return new TypeSafeMatcher<String>() {

            private String pathToJsonFile;

            protected boolean matchesSafely(String pathToJsonFile) {
                this.pathToJsonFile = pathToJsonFile;

                try {
                    getJsonSchemaFor(pathToJsonSchema).validate(getJsonObjectFor(pathToJsonFile));
                    return false;
                } catch (ValidationException var3) {
                    return true;
                } catch (IOException var4) {
                    throw new IllegalArgumentException(var4);
                }
            }

            public void describeTo(Description description) {
                description.appendText("json file ").appendValue(this.pathToJsonFile).appendText(" to fail validation against schema ").appendValue(pathToJsonSchema);
            }

            protected void describeMismatchSafely(String pathToJsonFile, Description mismatchDescription) {
                mismatchDescription.appendText("validation passed");
            }
        };
    }

    private static JSONObject getJsonObjectFor(String pathToJsonFile) throws IOException {
        return new JSONObject(getJsonContentFrom(pathToJsonFile));
    }

    private static Schema getJsonSchemaFor(String pathToJsonSchema) throws IOException {
        final String jsonSchema = getJsonContentFrom(pathToJsonSchema);
        return schemaCatalogResolver().loadSchema(jsonSchema);
    }

    private static String getJsonContentFrom(String pathToJsonSchema) throws IOException {
        String jsonContent;
        if (Paths.get(pathToJsonSchema, new String[0]).isAbsolute()) {
            jsonContent = Files.toString(new File(pathToJsonSchema), Charsets.UTF_8);
        } else {
            jsonContent = Resources.toString(Resources.getResource(pathToJsonSchema), Charsets.UTF_8);
        }

        return jsonContent;
    }

}
