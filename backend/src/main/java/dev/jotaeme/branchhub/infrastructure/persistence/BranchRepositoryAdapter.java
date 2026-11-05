package dev.jotaeme.branchhub.infrastructure.persistence;

import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageSlice;
import dev.jotaeme.branchhub.domain.branch.Branch;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class BranchRepositoryAdapter implements BranchRepositoryPort {
    private final SpringDataBranchRepository repository;

    public BranchRepositoryAdapter(SpringDataBranchRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public Branch save(Branch branch) {
        BranchJpaEntity entity = repository.save(toEntity(branch));
        return toDomain(entity);
    }

    @Override
    public Optional<Branch> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public PageSlice<Branch> findAll(PageQuery query) {
        Page<BranchJpaEntity> result = repository.findAll(SpringPageRequests.from(query));
        return new PageSlice<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements());
    }

    private BranchJpaEntity toEntity(Branch branch) {
        return BranchJpaEntity.builder()
                .id(branch.id())
                .code(branch.code())
                .name(branch.name())
                .city(branch.city())
                .active(branch.active())
                .createdAt(branch.createdAt())
                .updatedAt(branch.updatedAt())
                .build();
    }

    private Branch toDomain(BranchJpaEntity entity) {
        return Branch.restore(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCity(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
