package dev.jotaeme.branchhub.entrypoints.rest.pagination;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.SortPolicy;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PageRequestParams;

/**
 * Maps HTTP {@link PageRequestParams} (+ resource {@link SortPolicy}) to application {@link PageQuery}.
 */
public final class PaginationBinding {

    private PaginationBinding() {}

    public static PageQuery toPageQuery(PageRequestParams params, SortPolicy sortPolicy) {
        return new PageQuery(params.getPage(), params.getSize(), sortPolicy.resolve(params.getSort()));
    }
}
