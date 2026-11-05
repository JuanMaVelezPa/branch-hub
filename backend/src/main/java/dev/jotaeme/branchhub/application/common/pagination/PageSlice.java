package dev.jotaeme.branchhub.application.common.pagination;

import java.util.List;

public record PageSlice<T>(List<T> content, long totalElements) {
}
