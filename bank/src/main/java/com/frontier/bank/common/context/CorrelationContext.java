package com.frontier.bank.common.context;

import java.util.UUID;

/**
 * Contexto de correlação da requisição/serviço atual (thread-local).
 * Populado pelo {@link CorrelationIdFilter} e consumido pelos eventos de
 * domínio, permitindo rastrear uma requisição até os consumidores assíncronos.
 */
public final class CorrelationContext {

	private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

	private CorrelationContext() {
	}

	/** @return o id de correlação atual, ou {@code null} se não houver */
	public static String get() {
		return CURRENT.get();
	}

	public static void set(String correlationId) {
		CURRENT.set(correlationId);
	}

	/** @return o id atual; gera um novo quando não houver (ex.: jobs agendados) */
	public static String getOrCreate() {
		String current = CURRENT.get();
		if (current == null || current.isBlank()) {
			return UUID.randomUUID().toString();
		}
		return current;
	}

	public static void clear() {
		CURRENT.remove();
	}

}
