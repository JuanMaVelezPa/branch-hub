package dev.jotaeme.branchhub.entrypoints.rest;

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

import dev.jotaeme.branchhub.application.branch.BranchListPagination;
import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.application.branch.model.CreateBranchCommand;
import dev.jotaeme.branchhub.application.branch.model.UpdateBranchCommand;
import dev.jotaeme.branchhub.application.branch.usecase.CreateBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.DeactivateBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.GetBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.ListBranchesUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.UpdateBranchUseCase;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.entrypoints.rest.dto.BranchResponse;
import dev.jotaeme.branchhub.entrypoints.rest.dto.CreateBranchRequest;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PageRequestParams;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PagedResponse;
import dev.jotaeme.branchhub.entrypoints.rest.dto.UpdateBranchRequest;
import dev.jotaeme.branchhub.entrypoints.rest.mapper.PageMapper;
import dev.jotaeme.branchhub.entrypoints.rest.pagination.PaginationBinding;
import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import io.micrometer.core.instrument.Timer;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;

@Validated
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
