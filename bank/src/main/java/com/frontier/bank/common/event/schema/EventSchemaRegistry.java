package com.frontier.bank.common.event.schema;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.error.EventSchemaViolationException;
import com.frontier.bank.common.event.DomainEvent;

/**
 * Registro de contratos de eventos.
 * <p>
 * Funciona como um <i>schema registry</i> embarcado: guarda o schema vigente de
 * cada tipo, valida a evolução (aditiva é permitida; remoção/troca de tipo exige
 * nova versão) e confere cada evento no momento da publicação — um evento fora
 * do contrato derruba a transação em vez de ir para a outbox.
 */
@Component
public class EventSchemaRegistry {

	private static final Logger log = LoggerFactory.getLogger(EventSchemaRegistry.class);

	private final EventSchemaRepository repository;
	private final Map<String, EventSchema> schemas = new ConcurrentHashMap<>();

	public EventSchemaRegistry(EventSchemaRepository repository) {
		this.repository = repository;
	}

	/** Carrega os schemas já persistidos (executado no arranque). */
	@Transactional(readOnly = true)
	public void loadPersistedSchemas() {
		repository.findAll().forEach(record -> schemas.put(record.getEventType(), record.toSchema()));
	}

	@Transactional
	public void register(EventSchema schema) {
		Optional<EventSchemaRecord> stored = repository.findById(schema.eventType());

		if (stored.isEmpty()) {
			repository.save(EventSchemaRecord.of(schema));
			schemas.put(schema.eventType(), schema);
			log.info("Schema registrado: {} v{} ({} campo(s))",
					schema.eventType(), schema.version(), schema.fields().size());
			return;
		}

		EventSchema previous = stored.get().toSchema();
		boolean breaking = EventSchemaCompatibility.compare(previous, schema)
				== EventSchemaCompatibility.Verdict.BREAKING;

		if (breaking && schema.version() <= previous.version()) {
			throw new EventSchemaViolationException(
					("Evolução incompatível do evento %s: campo removido ou com tipo alterado exige incrementar a "
							+ "versão (atual v%d, declarada v%d). Evoluções compatíveis são apenas aditivas.")
							.formatted(schema.eventType(), previous.version(), schema.version()));
		}

		if (schema.version() != previous.version() || !schema.fingerprint().equals(previous.fingerprint())) {
			EventSchemaRecord record = stored.get();
			record.updateTo(schema);
			repository.save(record);
			log.info("Schema atualizado: {} v{} → v{}",
					schema.eventType(), previous.version(), schema.version());
		}
		schemas.put(schema.eventType(), schema);
	}

	/**
	 * Valida o evento contra o contrato registrado.
	 *
	 * @throws EventSchemaViolationException se o tipo não está registrado, a versão
	 *         não confere ou os campos do payload divergem do schema
	 */
	public void validate(DomainEvent<?> event) {
		EventSchema schema = schemas.get(event.eventType());
		if (schema == null) {
			throw new EventSchemaViolationException(
					"Evento não registrado no schema registry: " + event.eventType());
		}
		if (schema.version() != event.eventVersion()) {
			throw new EventSchemaViolationException(
					"Versão divergente do schema de %s: registrada v%d, evento v%d".formatted(
							event.eventType(), schema.version(), event.eventVersion()));
		}

		List<String> payloadFields = EventSchemaDeriver.fieldNamesOf(event.payload().getClass());
		if (!schema.fieldNames().equals(new java.util.LinkedHashSet<>(payloadFields))) {
			throw new EventSchemaViolationException(
					"Payload de %s difere do schema registrado v%d: esperado %s, recebido %s".formatted(
							event.eventType(), schema.version(), schema.fieldNames(), payloadFields));
		}
	}

	public List<EventSchema> schemas() {
		return schemas.values().stream()
				.sorted(Comparator.comparing(EventSchema::eventType))
				.toList();
	}

}
