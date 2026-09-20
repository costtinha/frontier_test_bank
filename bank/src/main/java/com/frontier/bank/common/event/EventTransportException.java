package com.frontier.bank.common.event;

/**
 * Falha ao entregar um evento ao transporte (broker indisponível, timeout…).
 */
public class EventTransportException extends RuntimeException {

	public EventTransportException(String message, Throwable cause) {
		super(message, cause);
	}

}
