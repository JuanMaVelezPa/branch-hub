package dev.jotaeme.branchhub.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableKafkaRetryTopic
@EnableScheduling
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOutboxConfig {

    @Bean("kafkaTemplate")
    @ConditionalOnMissingBean(name = "kafkaTemplate")
    KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    NewTopic hrNewBranchNotificationsTopic(
            @Value("${app.kafka.topics.hr-notifications:hr-new-branch-notifications}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
