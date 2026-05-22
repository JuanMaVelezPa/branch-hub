package com.jm.spring_web.infrastructure.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxJpaEntity;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxJpaRepository;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IamEmailOutboxRelayWorkerTest {

    @Mock
    private IamEmailOutboxJpaRepository repository;

    @Mock
    private IamEmailTemplateRenderer renderer;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @Test
    void shouldMarkSentAfterSuccessfulSend() throws Exception {
        UUID id = UUID.randomUUID();
        IamEmailOutboxJpaEntity row = pendingRow(id);
        when(repository.findById(id)).thenReturn(Optional.of(row));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(renderer.renderHtml(any(), any())).thenReturn("<html/>");

        IamEmailProperties props = new IamEmailProperties(true, "from@test", 5000, 50, 5);
        IamEmailOutboxRelayWorker worker =
                new IamEmailOutboxRelayWorker(repository, props, renderer, new ObjectMapper());

        worker.process(mailSender, id);

        ArgumentCaptor<IamEmailOutboxJpaEntity> captor = ArgumentCaptor.forClass(IamEmailOutboxJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IamEmailOutboxStatus.SENT);
        assertThat(captor.getValue().getSentAt()).isNotNull();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void shouldIncrementAttemptsOnMailFailure() throws Exception {
        UUID id = UUID.randomUUID();
        IamEmailOutboxJpaEntity row = pendingRow(id);
        when(repository.findById(id)).thenReturn(Optional.of(row));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(renderer.renderHtml(any(), any())).thenReturn("<html/>");
        org.mockito.Mockito.doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        IamEmailProperties props = new IamEmailProperties(true, "from@test", 5000, 50, 5);
        IamEmailOutboxRelayWorker worker =
                new IamEmailOutboxRelayWorker(repository, props, renderer, new ObjectMapper());

        worker.process(mailSender, id);

        ArgumentCaptor<IamEmailOutboxJpaEntity> captor = ArgumentCaptor.forClass(IamEmailOutboxJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAttempts()).isEqualTo(1);
        assertThat(captor.getValue().getStatus()).isEqualTo(IamEmailOutboxStatus.PENDING);
    }

    private static IamEmailOutboxJpaEntity pendingRow(UUID id) throws Exception {
        IamEmailOutboxJpaEntity row = new IamEmailOutboxJpaEntity();
        row.setId(id);
        row.setTemplateId(IamEmailTemplateId.GENERIC_NOTICE.templateId());
        row.setToAddress("to@test");
        row.setSubject(null);
        row.setPayloadJson(new ObjectMapper().writeValueAsString(Map.of("title", "T", "lead", "L")));
        row.setStatus(IamEmailOutboxStatus.PENDING);
        row.setAttempts(0);
        row.setCreatedAt(Instant.now());
        return row;
    }
}
