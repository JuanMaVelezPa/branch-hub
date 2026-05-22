package com.jm.spring_web.infrastructure.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxJpaEntity;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxJpaRepository;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class IamEmailOutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(IamEmailOutboxRelayWorker.class);

    private final IamEmailOutboxJpaRepository repository;
    private final IamEmailProperties properties;
    private final IamEmailTemplateRenderer renderer;
    private final ObjectMapper objectMapper;

    public IamEmailOutboxRelayWorker(
            IamEmailOutboxJpaRepository repository,
            IamEmailProperties properties,
            IamEmailTemplateRenderer renderer,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.properties = properties;
        this.renderer = renderer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(JavaMailSender mailSender, UUID rowId) {
        IamEmailOutboxJpaEntity managed = repository.findById(rowId).orElse(null);
        if (managed == null || managed.getStatus() != IamEmailOutboxStatus.PENDING) {
            return;
        }
        try {
            IamEmailTemplateId template = IamEmailTemplateId.fromTemplateId(managed.getTemplateId());
            Map<String, Object> vars =
                    objectMapper.readValue(managed.getPayloadJson(), new TypeReference<>() {});
            String html = renderer.renderHtml(template, vars);
            String subject = managed.getSubject() != null && !managed.getSubject().isBlank()
                    ? managed.getSubject()
                    : template.defaultSubject();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.fromAddress());
            helper.setTo(managed.getToAddress());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);

            managed.setStatus(IamEmailOutboxStatus.SENT);
            managed.setSentAt(Instant.now());
            managed.setLastError(null);
            repository.save(managed);
            log.info("IAM email outbox sent id={} template={} to={}", managed.getId(), managed.getTemplateId(), managed.getToAddress());
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            fail(managed, ex.getMessage());
        } catch (MailException | MessagingException ex) {
            retryOrFail(managed, ex);
        } catch (Exception ex) {
            retryOrFail(managed, ex);
        }
    }

    private void fail(IamEmailOutboxJpaEntity managed, String message) {
        managed.setStatus(IamEmailOutboxStatus.FAILED);
        managed.setLastError(message);
        repository.save(managed);
        log.warn("IAM email outbox failed permanently id={} reason={}", managed.getId(), message);
    }

    private void retryOrFail(IamEmailOutboxJpaEntity managed, Exception ex) {
        int next = managed.getAttempts() + 1;
        managed.setAttempts(next);
        managed.setLastError(ex.getMessage());
        if (next >= properties.maxAttempts()) {
            managed.setStatus(IamEmailOutboxStatus.FAILED);
            log.warn("IAM email outbox failed after {} attempts id={}", next, managed.getId(), ex);
        } else {
            log.warn("IAM email outbox send error attempt {}/{} id={}", next, properties.maxAttempts(), managed.getId(), ex);
        }
        repository.save(managed);
    }
}
