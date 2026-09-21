package com.frontier.bank.common.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

	/**
	 * Seleciona e bloqueia um lote de eventos pendentes.
	 * <p>
	 * {@code FOR UPDATE SKIP LOCKED} (PostgreSQL) faz com que várias instâncias da
	 * aplicação publiquem em paralelo sem duplicar linhas nem ficarem travadas
	 * esperando umas às outras — cada instância pega um lote diferente.
	 */
	@Query(value = """
			SELECT * FROM outbox_events
			WHERE published_at IS NULL
			ORDER BY occurred_at ASC
			LIMIT :batchSize
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OutboxEvent> lockPendingBatch(@Param("batchSize") int batchSize);

	/**
	 * Lote do log de eventos a partir de uma posição (checkpoint).
	 * <p>
	 * A comparação é <b>inclusiva</b> de propósito: o evento do checkpoint é
	 * relido a cada ciclo e descartado pelo dedupe ({@code processed_events}).
	 * Isso é o que permite reexecutar um evento específico (dead letter) apenas
	 * posicionando o checkpoint nele.
	 * <p>
	 * A outbox funciona como log de eventos local: consumidores de read model leem
	 * por posição, em ordem estável {@code (occurred_at, id)}, sem depender do
	 * estado de publicação para o broker.
	 */
	@Query(value = """
			SELECT * FROM outbox_events
			WHERE (occurred_at, id) >= (:occurredAt, :eventId)
			ORDER BY occurred_at ASC, id ASC
			LIMIT :batchSize
			""", nativeQuery = true)
	List<OutboxEvent> findAfter(@Param("occurredAt") Instant occurredAt,
			@Param("eventId") UUID eventId, @Param("batchSize") int batchSize);

	/** Quantos eventos ainda estão pendentes de publicação (backlog do outbox). */
	long countByPublishedAtIsNull();

	/** Quantos eventos do log estão à frente de uma posição (pendentes de projeção). */
	@Query(value = """
			SELECT count(*) FROM outbox_events
			WHERE (occurred_at, id) > (:occurredAt, :eventId)
			""", nativeQuery = true)
	long countAfter(@Param("occurredAt") Instant occurredAt, @Param("eventId") UUID eventId);

	Optional<OutboxEvent> findByEventId(UUID eventId);

}
