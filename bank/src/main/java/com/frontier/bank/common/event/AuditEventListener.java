package com.frontier.bank.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reação in-process a eventos de domínio, no mesmo contexto.
 * <p>
 * Executa somente <b>após o commit</b>: consumidores locais nunca enxergam estado
 * que não foi persistido. Hoje registra a trilha em log — é o ponto de extensão
 * para o store de auditoria imutável (Fase 4) e para projeções locais (Fase 2).
 */
@Component
public class AuditEventListener {

	private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onDomainEvent(DomainEvent<?> event) {
		log.info("AUDITORIA evento={} aggregate={}/{} eventId={} correlationId={} occurredAt={}",
				event.eventType(), event.aggregateType(), event.aggregateId(),
				event.eventId(), event.correlationId(), event.occurredAt());
	}

}
