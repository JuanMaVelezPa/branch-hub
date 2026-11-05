package dev.jotaeme.branchhub.application.common.pagination;

import java.util.Set;

/**
 * Whitelist + default sort for a given REST list resource. Reuse per aggregate (e.g. branches, products).
 */
public record SortPolicy(SortOrder defaultSort, Set<String> allowedFields) {

    public SortPolicy {
        if (allowedFields == null || allowedFields.isEmpty()) {
            throw new IllegalArgumentException("allowedFields must not be empty");
        }
        if (!allowedFields.contains(defaultSort.property())) {
            throw new IllegalArgumentException("default sort field must be in allowedFields");
        }
    }

    /**
     * @param rawSort query param {@code sort} (nullable)
     */
    public SortOrder resolve(String rawSort) {
        return SortOrder.parse(rawSort, allowedFields, defaultSort);
    }
}
