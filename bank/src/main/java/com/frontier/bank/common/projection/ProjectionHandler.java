package com.frontier.bank.common.projection;

import java.util.Set;

import com.frontier.bank.common.event.EventMessage;

/**
 * Construtor de um read model (consumidor de eventos).
 * <p>
 * Implementações devem ser <b>idempotentes</b> por natureza (aplicar o mesmo
 * evento duas vezes produz o mesmo estado) e tolerantes a eventos fora de ordem —
 * o controle de duplicidade e de posição fica a cargo da infraestrutura de
 * projeção ({@link ProjectionApplier}).
 */
public interface ProjectionHandler {

	/** Nome único do consumidor (usado em checkpoint e dedupe). */
	String consumerName();

	/** Tipos de evento relevantes para este read model. */
	Set<String> supportedEventTypes();

	/** Aplica o evento ao read model. */
	void apply(EventMessage event);

}
