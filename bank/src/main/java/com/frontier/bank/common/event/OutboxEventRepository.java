package com.frontier.bank.common.event;

import java.time.Instant;
import java.util.List;
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
	 * Lote seguinte do log de eventos a partir de uma posição (checkpoint).
	 * <p>
	 * A outbox funciona como log de eventos local: consumidores de read model
	 * leem por posição, em ordem estável {@code (occurred_at, id)}, sem depender
	 * do estado de publicação para o broker — projeção e entrega externa são
	 * independentes (um broker fora do ar não atrasa o read model).
	 */
	@Query(value = """
			SELECT * FROM outbox_events
			WHERE (occurred_at, id) > (:occurredAt, :eventId)
			ORDER BY occurred_at ASC, id ASC
			LIMIT :batchSize
			""", nativeQuery = true)
	List<OutboxEvent> findAfter(@Param("occurredAt") Instant occurredAt,
			@Param("eventId") UUID eventId, @Param("batchSize") int batchSize);

}
