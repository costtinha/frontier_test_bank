package com.frontier.bank.common.event;

/**
 * Transporte de saída dos eventos de integração.
 * <p>
 * Implementações: {@link LoggingEventTransport} (desenvolvimento, sem broker) e
 * {@code KafkaEventTransport} (produção). Troca-se via {@code bank.events.transport}.
 */
public interface EventTransport {

	/**
	 * Entrega o evento ao destino. Deve lançar {@link EventTransportException}
	 * em falha — o relay mantém a linha pendente para nova tentativa.
	 */
	void publish(EventMessage message);

}
