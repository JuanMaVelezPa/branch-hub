package dev.jotaeme.branchhub.infrastructure.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.SortDirection;

/**
 * Maps application {@link PageQuery} to Spring Data {@link PageRequest} (sort field = JPA attribute name).
 */
public final class SpringPageRequests {

    private SpringPageRequests() {}

    public static PageRequest from(PageQuery query) {
        Sort.Direction dir =
                query.sort().direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(new Sort.Order(dir, query.sort().property()));
        return PageRequest.of(query.page(), query.size(), sort);
    }
}
