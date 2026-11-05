package dev.jotaeme.branchhub.application.common.pagination;

import java.util.Locale;
import java.util.Set;

/**
 * Single-column sort (matches query shape {@code field,asc|desc}).
 */
public record SortOrder(String property, SortDirection direction) {

    public static SortOrder asc(String property) {
        return new SortOrder(property, SortDirection.ASC);
    }

    public static SortOrder desc(String property) {
        return new SortOrder(property, SortDirection.DESC);
    }

    public boolean ascending() {
        return direction == SortDirection.ASC;
    }

    /**
     * Parses {@code raw} as {@code property,direction}; blank uses {@code defaultOrder}.
     *
     * @param allowed lowercase JPA/property names allowed for this resource
     * @throws IllegalArgumentException when malformed or field not allowed (HTTP 400)
     */
    public static SortOrder parse(String raw, Set<String> allowed, SortOrder defaultOrder) {
        if (raw == null || raw.isBlank()) {
            return defaultOrder;
        }
        String[] parts = raw.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("sort must be field,asc or field,desc");
        }
        String requestedField = parts[0].trim();
        String dir = parts[1].trim().toLowerCase(Locale.ROOT);
        String field = allowed.stream()
                .filter(candidate -> candidate.equalsIgnoreCase(requestedField))
                .findFirst()
                .orElse(null);
        if (field == null) {
            throw new IllegalArgumentException("Invalid sort field");
        }
        SortDirection direction;
        if ("asc".equals(dir)) {
            direction = SortDirection.ASC;
        } else if ("desc".equals(dir)) {
            direction = SortDirection.DESC;
        } else {
            throw new IllegalArgumentException("Invalid sort direction");
        }
        return new SortOrder(field, direction);
    }
}
