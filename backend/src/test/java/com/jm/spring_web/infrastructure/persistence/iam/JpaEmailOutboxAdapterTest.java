package com.jm.spring_web.infrastructure.persistence.iam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jm.spring_web.application.email.EmailTemplateIds;
import com.jm.spring_web.application.email.model.EmailEnqueueCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaEmailOutboxAdapterTest {

    @Mock
    private IamEmailOutboxJpaRepository repository;

    @Test
    void enqueuePersistsPendingRow() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JpaEmailOutboxAdapter adapter = new JpaEmailOutboxAdapter(repository, mapper);
        adapter.enqueue(new EmailEnqueueCommand(
                EmailTemplateIds.GENERIC_NOTICE,
                "user@example.com",
                "Custom subject",
                Map.of("title", "Hi", "lead", "Test")));

        ArgumentCaptor<IamEmailOutboxJpaEntity> captor = ArgumentCaptor.forClass(IamEmailOutboxJpaEntity.class);
        verify(repository).save(captor.capture());
        IamEmailOutboxJpaEntity saved = captor.getValue();
        assertThat(saved.getTemplateId()).isEqualTo(EmailTemplateIds.GENERIC_NOTICE);
        assertThat(saved.getToAddress()).isEqualTo("user@example.com");
        assertThat(saved.getSubject()).isEqualTo("Custom subject");
        assertThat(saved.getStatus()).isEqualTo(IamEmailOutboxStatus.PENDING);
        assertThat(mapper.readTree(saved.getPayloadJson()).get("title").asText()).isEqualTo("Hi");
    }
}
