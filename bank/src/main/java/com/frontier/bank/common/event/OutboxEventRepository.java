package com.frontier.bank.common.event;

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

}
