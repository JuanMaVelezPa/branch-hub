package dev.jotaeme.branchhub.application.branch.usecase;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.application.branch.model.CreateBranchCommand;
import dev.jotaeme.branchhub.application.branch.port.BranchRepositoryPort;
import dev.jotaeme.branchhub.application.common.exception.ConflictException;
import dev.jotaeme.branchhub.application.notification.model.BranchCreatedNotificationEvent;
import dev.jotaeme.branchhub.application.notification.model.OutboxEventCommand;
import dev.jotaeme.branchhub.application.notification.port.OutboxEventRepositoryPort;
import dev.jotaeme.branchhub.domain.branch.Branch;

public class CreateBranchUseCase {
    private static final Logger logger = LoggerFactory.getLogger(CreateBranchUseCase.class);

    private final BranchRepositoryPort branchRepositoryPort;
    private final OutboxEventRepositoryPort outboxEventRepositoryPort;

    public CreateBranchUseCase(
            BranchRepositoryPort branchRepositoryPort,
            OutboxEventRepositoryPort outboxEventRepositoryPort
    ) {
        this.branchRepositoryPort = Objects.requireNonNull(branchRepositoryPort);
        this.outboxEventRepositoryPort = Objects.requireNonNull(outboxEventRepositoryPort);
    }

    @Transactional
    public BranchResult execute(CreateBranchCommand command) {
        if (branchRepositoryPort.existsByCode(command.code().trim())) {
            throw new ConflictException("Branch code already exists");
        }
        Branch saved = branchRepositoryPort.save(Branch.createNew(command.code(), command.name(), command.city()));
        UUID eventId = resolveEventIdFromTransactionContext();
        BranchCreatedNotificationEvent event = new BranchCreatedNotificationEvent(
                eventId,
                saved.id(),
                saved.code(),
                saved.name(),
                saved.city(),
                saved.createdAt()
        );
        outboxEventRepositoryPort.savePending(new OutboxEventCommand(
                eventId,
                "BRANCH",
                saved.id(),
                "BRANCH_CREATED",
                toJson(event),
                saved.createdAt()
        ));
        logger.info(
                "Branch created and outbox enqueued code={} branchId={} eventId={}",
                saved.code(),
                saved.id(),
                eventId
        );
        return BranchMapper.toResult(saved);
    }

    private String toJson(BranchCreatedNotificationEvent event) {
        return String.format(
                "{\"eventId\":\"%s\",\"branchId\":\"%s\",\"code\":\"%s\",\"name\":\"%s\",\"city\":\"%s\",\"createdAt\":\"%s\"}",
                event.eventId(),
                event.branchId(),
                escapeJson(event.code()),
                escapeJson(event.name()),
                escapeJson(event.city()),
                event.createdAt()
        );
    }

    private String escapeJson(String value) {
        return value.replace("\"", "\\\"");
    }

    private UUID resolveEventIdFromTransactionContext() {
        String transactionId = MDC.get("transactionId");
        if (transactionId == null || transactionId.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(transactionId);
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid transactionId in MDC. Falling back to random event id.");
            return UUID.randomUUID();
        }
    }
}
