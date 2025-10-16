package uk.gov.moj.cpp.notification.query.view;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static uk.gov.moj.cpp.notification.common.FilterType.FIELD;

import uk.gov.moj.cpp.notification.persistence.EventCacheJdbcRepository;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class FilterParser {

    private static final String SPACE = " ";
    private static final String CLAUSE_PATTERN = "%s = '%s'";

    private FilterParser() {
    }

    /**
     * Returns a SQL query 'WHERE' clause representing the filter
     *
     * @param filter - the filter as specified as a JsonObject
     * @return a SQL WHERE clause representing the filter for the {@link EventCacheJdbcRepository}
     */
    public static String parse(final JsonObject filter) {

        final String filterType = filter.getString("type");

        if (FIELD.name().equals(filterType)) {
            return format(CLAUSE_PATTERN, filter.getString("name"), filter.getString("value"));
        } else {
            final JsonArray filterArray = filter.getJsonArray("value");

            final String parsedFilters = filterArray.stream()
                    .map(jsonValue -> parse((JsonObject) jsonValue))
                    .collect(joining(SPACE + filterType + SPACE));

            return "(" + parsedFilters + ")";
        }
    }
}
