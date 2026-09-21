package com.frontier.bank.common.context;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * Contexto de correlação da requisição/serviço atual (thread-local + MDC).
 * <p>
 * Populado pelo {@link CorrelationIdFilter} e consumido pelos eventos de domínio,
 * permitindo rastrear uma requisição até os consumidores assíncronos. Além do
 * thread-local, o id é publicado no MDC para aparecer em <b>toda</b> linha de log.
 */
public final class CorrelationContext {

	/** Chave do id de correlação no MDC (usada no pattern de log). */
	public static final String MDC_KEY = "correlationId";

	private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

	private CorrelationContext() {
	}

	/** @return o id de correlação atual, ou {@code null} se não houver */
	public static String get() {
		return CURRENT.get();
	}

	public static void set(String correlationId) {
		if (correlationId == null || correlationId.isBlank()) {
			clear();
			return;
		}
		CURRENT.set(correlationId);
		MDC.put(MDC_KEY, correlationId);
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
		MDC.remove(MDC_KEY);
	}

}
