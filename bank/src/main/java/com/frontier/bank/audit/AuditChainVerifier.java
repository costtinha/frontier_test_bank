package com.frontier.bank.audit;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica a integridade da trilha de auditoria recalculando a cadeia de hashes.
 * <p>
 * A verificação detecta dois tipos de adulteração: <b>remoção</b> de registros
 * (o encadeamento deixa de casar) e <b>alteração</b> de conteúdo (o hash
 * recalculado difere do armazenado).
 */
@Component
public class AuditChainVerifier {

	private final AuditRepository auditRepository;

	public AuditChainVerifier(AuditRepository auditRepository) {
		this.auditRepository = auditRepository;
	}

	@Transactional(readOnly = true)
	public Report verify() {
		List<AuditRecord> records = auditRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
		String expectedPrevious = AuditHasher.GENESIS_HASH;

		for (AuditRecord record : records) {
			if (!expectedPrevious.equals(record.getPreviousHash())) {
				return Report.broken(records.size(), record.getId(), "encadeamento quebrado: registro fora de sequência");
			}
			if (!AuditHasher.hash(record.getPreviousHash(), record).equals(record.getRecordHash())) {
				return Report.broken(records.size(), record.getId(), "conteúdo adulterado: hash não confere");
			}
			expectedPrevious = record.getRecordHash();
		}
		return Report.valid(records.size());
	}

	/**
	 * @param valid      {@code true} quando a cadeia inteira confere
	 * @param records    quantidade de registros verificados
	 * @param brokenAtId id do primeiro registro inconsistente, se houver
	 * @param reason     motivo da inconsistência
	 */
	public record Report(boolean valid, long records, Long brokenAtId, String reason, Instant verifiedAt) {

		static Report valid(long records) {
			return new Report(true, records, null, null, Instant.now());
		}

		static Report broken(long records, Long brokenAtId, String reason) {
			return new Report(false, records, brokenAtId, reason, Instant.now());
		}
	}

}
