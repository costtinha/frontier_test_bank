package com.frontier.bank.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventSerializationException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Grava a trilha de auditoria (WORM) de cada evento de domínio.
 * <p>
 * O listener é <b>síncrono</b> e roda dentro da própria transação que publicou o
 * evento: a auditoria é commitada junto com o estado e com a outbox — não existe
 * mudança de estado sem registro de auditoria, nem auditoria de algo que não
 * commitou.
 * <p>
 * A escrita da cadeia é serializada pela trava na linha de guarda, garantindo
 * encadeamento linear mesmo com várias instâncias da aplicação.
 */
@Component
public class AuditTrailRecorder {

	private static final Logger log = LoggerFactory.getLogger(AuditTrailRecorder.class);

	private final AuditRepository auditRepository;
	private final AuditChainGuardRepository chainGuardRepository;
	private final ObjectMapper objectMapper;

	public AuditTrailRecorder(AuditRepository auditRepository, AuditChainGuardRepository chainGuardRepository,
			ObjectMapper objectMapper) {
		this.auditRepository = auditRepository;
		this.chainGuardRepository = chainGuardRepository;
		this.objectMapper = objectMapper;
	}

	@EventListener
	public void onDomainEvent(DomainEvent<?> event) {
		chainGuardRepository.findById(AuditChainGuard.SINGLE_ROW_ID)
				.orElseThrow(() -> new IllegalStateException("linha de guarda da cadeia de auditoria ausente"));

		String previousHash = auditRepository.findFirstByOrderByIdDesc()
				.map(AuditRecord::getRecordHash)
				.orElse(AuditHasher.GENESIS_HASH);

		AuditRecord record = auditRepository.save(AuditRecord.of(event, serialize(event), previousHash));

		log.debug("Auditoria registrada id={} tipo={} correlationId={}",
				record.getId(), record.getEventType(), record.getCorrelationId());
	}

	private String serialize(DomainEvent<?> event) {
		try {
			return objectMapper.writeValueAsString(event.payload());
		} catch (JacksonException e) {
			throw new EventSerializationException("Falha ao serializar o payload para auditoria: " + event.eventType(), e);
		}
	}

}
