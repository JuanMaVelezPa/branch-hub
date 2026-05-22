package com.jm.spring_web.infrastructure.persistence.iam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jm.spring_web.application.email.model.EmailEnqueueCommand;
import com.jm.spring_web.application.email.port.EmailOutboxPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaEmailOutboxAdapter implements EmailOutboxPort {

    private final IamEmailOutboxJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaEmailOutboxAdapter(IamEmailOutboxJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void enqueue(EmailEnqueueCommand command) {
        IamEmailOutboxJpaEntity row = new IamEmailOutboxJpaEntity();
        row.setId(UUID.randomUUID());
        row.setTemplateId(command.templateId());
        row.setToAddress(command.toAddress().trim());
        row.setSubject(command.subjectOverride());
        try {
            row.setPayloadJson(objectMapper.writeValueAsString(command.templateVariables()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Email template variables must be JSON-serializable", e);
        }
        row.setStatus(IamEmailOutboxStatus.PENDING);
        row.setAttempts(0);
        row.setCreatedAt(Instant.now());
        repository.save(row);
    }
}
