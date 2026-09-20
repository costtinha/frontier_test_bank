package com.frontier.bank.common.event;

/**
 * Falha ao serializar/transportar um evento. O relay trata como falha
 * recuperável: a linha permanece pendente para nova tentativa.
 */
public class EventSerializationException extends RuntimeException {

	public EventSerializationException(String message, Throwable cause) {
		super(message, cause);
	}

}
