package com.jm.spring_web.entrypoints.rest;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jm.spring_web.application.branch.BranchListPagination;
import com.jm.spring_web.application.branch.model.BranchResult;
import com.jm.spring_web.application.branch.model.CreateBranchCommand;
import com.jm.spring_web.application.branch.model.UpdateBranchCommand;
import com.jm.spring_web.application.branch.usecase.CreateBranchUseCase;
import com.jm.spring_web.application.branch.usecase.DeactivateBranchUseCase;
import com.jm.spring_web.application.branch.usecase.GetBranchUseCase;
import com.jm.spring_web.application.branch.usecase.ListBranchesUseCase;
import com.jm.spring_web.application.branch.usecase.UpdateBranchUseCase;
import com.jm.spring_web.application.common.pagination.PageQuery;
import com.jm.spring_web.entrypoints.rest.dto.BranchResponse;
import com.jm.spring_web.entrypoints.rest.dto.CreateBranchRequest;
import com.jm.spring_web.entrypoints.rest.dto.PageRequestParams;
import com.jm.spring_web.entrypoints.rest.dto.PagedResponse;
import com.jm.spring_web.entrypoints.rest.dto.UpdateBranchRequest;
import com.jm.spring_web.entrypoints.rest.mapper.PageMapper;
import com.jm.spring_web.entrypoints.rest.pagination.PaginationBinding;
import com.jm.spring_web.infrastructure.observability.AppMetrics;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;

@Validated
@Tag(name = "Branches")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {
    private final CreateBranchUseCase createBranchUseCase;
    private final GetBranchUseCase getBranchUseCase;
    private final ListBranchesUseCase listBranchesUseCase;
    private final UpdateBranchUseCase updateBranchUseCase;
    private final DeactivateBranchUseCase deactivateBranchUseCase;
    private final AppMetrics appMetrics;

    public BranchController(
            CreateBranchUseCase createBranchUseCase,
            GetBranchUseCase getBranchUseCase,
            ListBranchesUseCase listBranchesUseCase,
            UpdateBranchUseCase updateBranchUseCase,
            DeactivateBranchUseCase deactivateBranchUseCase,
            AppMetrics appMetrics) {
        this.createBranchUseCase = createBranchUseCase;
        this.getBranchUseCase = getBranchUseCase;
        this.listBranchesUseCase = listBranchesUseCase;
        this.updateBranchUseCase = updateBranchUseCase;
        this.deactivateBranchUseCase = deactivateBranchUseCase;
        this.appMetrics = appMetrics;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(@Valid @RequestBody CreateBranchRequest request) {
        BranchResult result = executeMeasured("create", () -> createBranchUseCase
                .execute(new CreateBranchCommand(request.code(), request.name(), request.city())));
        return toResponse(result);
    }

    @GetMapping("/{id}")
    public BranchResponse getById(@PathVariable UUID id) {
        return toResponse(executeMeasured("get_by_id", () -> getBranchUseCase.execute(id)));
    }

    @GetMapping
    public PagedResponse<BranchResponse> list(@Valid @ParameterObject PageRequestParams pagination) {
        PageQuery query = PaginationBinding.toPageQuery(pagination, BranchListPagination.SORT_POLICY);
        return executeMeasured(
                "list",
                () -> PageMapper.toPagedResponse(listBranchesUseCase.execute(query), this::toResponse));
    }

    @PutMapping("/{id}")
    public BranchResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBranchRequest request) {
        BranchResult result = executeMeasured("update", () ->
                updateBranchUseCase.execute(id, new UpdateBranchCommand(request.name(), request.city())));
        return toResponse(result);
    }

    @PatchMapping("/{id}/deactivate")
    public BranchResponse deactivate(@PathVariable UUID id) {
        return toResponse(executeMeasured("deactivate", () -> deactivateBranchUseCase.execute(id)));
    }

    private <T> T executeMeasured(String operation, Supplier<T> action) {
        Timer.Sample sample = appMetrics.startSample();
        try {
            T result = action.get();
            appMetrics.incrementBranchCommand(operation, "success");
            appMetrics.recordUseCaseDuration(sample, "branch_" + operation, "success");
            return result;
        } catch (RuntimeException exception) {
            appMetrics.incrementBranchCommand(operation, "failure");
            appMetrics.recordUseCaseDuration(sample, "branch_" + operation, "failure");
            throw exception;
        }
    }

    private BranchResponse toResponse(BranchResult result) {
        return new BranchResponse(
                result.id(),
                result.code(),
                result.name(),
                result.city(),
                result.isActive(),
                result.createdAt(),
                result.updatedAt());
    }
}
