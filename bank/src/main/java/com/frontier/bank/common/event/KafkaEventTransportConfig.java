package com.frontier.bank.common.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import tools.jackson.databind.ObjectMapper;

/**
 * Ativa o transporte Kafka apenas quando {@code bank.events.transport=kafka}.
 * Em desenvolvimento (padrão) nada disso é carregado — a aplicação sobe sem broker.
 */
@Configuration
@ConditionalOnProperty(name = "bank.events.transport", havingValue = "kafka")
public class KafkaEventTransportConfig {

	@Bean
	EventTransport kafkaEventTransport(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
			@Value("${bank.events.topic:bank.events}") String topic,
			@Value("${bank.events.kafka.timeout-seconds:10}") long timeoutSeconds) {
		return new KafkaEventTransport(kafkaTemplate, objectMapper, topic, timeoutSeconds);
	}

	/**
	 * Cria o tópico no arranque (idempotente). 3 partições dão paralelismo de
	 * consumo mantendo a ordem por agregado (mesma chave → mesma partição).
	 */
	@Bean
	NewTopic bankEventsTopic(@Value("${bank.events.topic:bank.events}") String topic) {
		return TopicBuilder.name(topic).partitions(3).replicas(1).build();
	}

}
