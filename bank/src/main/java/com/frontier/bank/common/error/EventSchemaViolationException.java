package com.frontier.bank.common.error;

/**
 * Violação do contrato de eventos: evento não registrado, payload divergente do
 * schema ou evolução incompatível sem incremento de versão.
 * <p>
 * Não é erro de cliente: indica defeito/drift de contrato e deve falhar rápido,
 * derrubando a transação em vez de publicar um evento fora do contrato.
 */
public class EventSchemaViolationException extends RuntimeException {

	public EventSchemaViolationException(String message) {
		super(message);
	}

}
