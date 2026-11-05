package dev.jotaeme.branchhub.application.common.pagination;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        long totalElements,
        int page,
        int size,
        int totalPages) {

    public static <T> PageResult<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil(totalElements / (double) size);
        return new PageResult<>(content, totalElements, page, size, totalPages);
    }
}
