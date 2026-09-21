package com.frontier.bank.audit;

import com.frontier.bank.common.support.Hashing;

/**
 * Cálculo do encadeamento da trilha de auditoria.
 * <p>
 * O hash de um registro é o SHA-256 do hash anterior concatenado ao conteúdo
 * canônico do registro. Como cada hash depende do anterior, qualquer alteração
 * (ou remoção) de um registro invalida a cadeia a partir dele — é isso que dá a
 * propriedade <i>tamper-evident</i>.
 */
public final class AuditHasher {

	/** Hash inicial da cadeia (antes do primeiro registro). */
	public static final String GENESIS_HASH = "0".repeat(64);

	private AuditHasher() {
	}

	public static String hash(String previousHash, AuditRecord record) {
		return Hashing.sha256(previousHash + "|" + canonical(record));
	}

	/**
	 * Conteúdo canônico: ordem fixa e separador explícito, para que o mesmo
	 * registro produza sempre o mesmo texto (e portanto o mesmo hash).
	 */
	public static String canonical(AuditRecord record) {
		return String.join("|",
				record.getEventId().toString(),
				record.getEventType(),
				String.valueOf(record.getEventVersion()),
				record.getAggregateType(),
				record.getAggregateId().toString(),
				nullSafe(record.getActorId()),
				nullSafe(record.getCorrelationId()),
				nullSafe(record.getCausationId()),
				record.getOccurredAt().toString(),
				record.getRecordedAt().toString(),
				record.getPayload());
	}

	private static String nullSafe(String value) {
		return value == null ? "" : value;
	}

}
