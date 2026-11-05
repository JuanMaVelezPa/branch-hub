package dev.jotaeme.branchhub.entrypoints.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageRequestParams {

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;

    /**
     * Optional sort for list endpoints: {@code property,asc} or {@code property,desc}. Allowed fields are
     * defined per resource (see that resource's {@link dev.jotaeme.branchhub.application.common.pagination.SortPolicy}).
     */
    private String sort;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
