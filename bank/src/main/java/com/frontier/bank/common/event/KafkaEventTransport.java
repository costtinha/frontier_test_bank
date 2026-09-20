package com.frontier.bank.common.event;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Transporte de produção: publica no Kafka com a chave = id do agregado, o que
 * garante <b>ordenação por agregado</b> (dois saques do mesmo cliente nunca são
 * consumidos fora de ordem).
 * <p>
 * O envio é aguardado (com timeout) antes de marcar o evento como publicado:
 * preferimos atrasar a marcação a publicar "às cegas" e perder o evento.
 */
public class KafkaEventTransport implements EventTransport {

	private static final Logger log = LoggerFactory.getLogger(KafkaEventTransport.class);

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topic;
	private final long timeoutSeconds;

	public KafkaEventTransport(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
			String topic, long timeoutSeconds) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topic = topic;
		this.timeoutSeconds = timeoutSeconds;
	}

	@Override
	public void publish(EventMessage message) {
		try {
			String json = objectMapper.writeValueAsString(message);
			kafkaTemplate.send(topic, message.aggregateId().toString(), json)
					.get(timeoutSeconds, TimeUnit.SECONDS);
			log.debug("Evento {} ({}) publicado no tópico {}", message.eventId(), message.eventType(), topic);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new EventTransportException("Publicação interrompida do evento " + message.eventId(), e);
		} catch (ExecutionException | TimeoutException | JacksonException e) {
			throw new EventTransportException("Falha ao publicar evento " + message.eventId()
					+ " no tópico " + topic, e);
		}
	}

}
