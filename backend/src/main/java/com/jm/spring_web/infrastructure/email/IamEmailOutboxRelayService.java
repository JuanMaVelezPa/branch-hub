package com.jm.spring_web.infrastructure.email;

import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxJpaEntity;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxJpaRepository;
import com.jm.spring_web.infrastructure.persistence.iam.IamEmailOutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.iam.email", name = "enabled", havingValue = "true")
public class IamEmailOutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(IamEmailOutboxRelayService.class);

    private final IamEmailOutboxJpaRepository repository;
    private final IamEmailProperties properties;
    private final IamEmailOutboxRelayWorker worker;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public IamEmailOutboxRelayService(
            IamEmailOutboxJpaRepository repository,
            IamEmailProperties properties,
            IamEmailOutboxRelayWorker worker,
            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.repository = repository;
        this.properties = properties;
        this.worker = worker;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Scheduled(fixedDelayString = "${app.iam.email.relay-delay-ms:5000}")
    public void relayPending() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("app.iam.email.enabled=true but JavaMailSender is not configured (set spring.mail.host, etc.)");
            return;
        }
        int batch = Math.min(50, Math.max(1, properties.batchSize()));
        List<IamEmailOutboxJpaEntity> pending = repository.findAllByStatusOrderByCreatedAtAsc(
                IamEmailOutboxStatus.PENDING, PageRequest.of(0, batch));
        for (IamEmailOutboxJpaEntity row : pending) {
            worker.process(mailSender, row.getId());
        }
    }
}
