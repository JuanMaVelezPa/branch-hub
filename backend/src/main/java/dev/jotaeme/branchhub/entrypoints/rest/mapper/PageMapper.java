package dev.jotaeme.branchhub.entrypoints.rest.mapper;

import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PagedResponse;

import java.util.function.Function;

public final class PageMapper {
    private PageMapper() {
    }

    public static <I, O> PagedResponse<O> toPagedResponse(PageResult<I> page, Function<I, O> itemMapper) {
        return new PagedResponse<>(
                page.content().stream().map(itemMapper).toList(),
                page.totalElements(),
                page.page(),
                page.size(),
                page.totalPages());
    }
}
