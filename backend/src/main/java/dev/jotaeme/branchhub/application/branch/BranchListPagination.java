package dev.jotaeme.branchhub.application.branch;

import java.util.Set;

import dev.jotaeme.branchhub.application.common.pagination.SortOrder;
import dev.jotaeme.branchhub.application.common.pagination.SortPolicy;

/**
 * Pagination/sort contract for branch listing. Reuse the same pattern for new aggregates with their own
 * {@link SortPolicy} and allowed field set (aligned with JPA property names).
 */
public final class BranchListPagination {

    public static final SortPolicy SORT_POLICY =
            new SortPolicy(SortOrder.asc("code"), Set.of("code", "name", "city", "active"));

    private BranchListPagination() {}
}
