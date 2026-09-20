package com.frontier.bank.common.event;

import java.util.UUID;

/**
 * Corpo de um evento de domínio. Cada evento declara seu próprio tipo, agregado
 * e versão — evitando strings soltas nos call sites e tornando o contrato
 * explícito e versionável.
 */
public interface EventPayload {

	/** Identificador do agregado dono do fato (ex.: userId, balanceId). */
	UUID aggregateId();

	/** Nome canônico do evento (ex.: {@code bank.user.UserRegistered}). */
	String eventType();

	/** Nome do agregado (ex.: {@code User}, {@code Balance}). */
	String aggregateType();

	/**
	 * Versão do schema do evento. Só incremente em mudanças incompatíveis:
	 * evoluções devem ser aditivas (nunca renomear/remover campos).
	 */
	default int version() {
		return 1;
	}

}
