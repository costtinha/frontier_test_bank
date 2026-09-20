package com.frontier.bank.common.event;

/**
 * Publica eventos de domínio.
 * <p>
 * A implementação grava o evento na outbox <b>dentro da transação corrente</b>
 * (gravação atômica com a mudança de estado) e notifica listeners in-process
 * após o commit. A entrega a sistemas externos é responsabilidade do relay.
 */
public interface EventPublisher {

	/**
	 * @throws org.springframework.transaction.IllegalTransactionStateException
	 *         quando chamado fora de uma transação — eventos só existem acoplados
	 *         a uma mudança de estado commitada
	 */
	void publish(DomainEvent<?> event);

}
