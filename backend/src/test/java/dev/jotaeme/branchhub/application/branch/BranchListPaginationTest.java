package dev.jotaeme.branchhub.application.branch;

import org.junit.jupiter.api.Test;

import dev.jotaeme.branchhub.application.common.pagination.SortOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BranchListPaginationTest {

    @Test
    void policyDefaultMatchesCodeAsc() {
        assertEquals(SortOrder.asc("code"), BranchListPagination.SORT_POLICY.resolve(null));
    }
}
