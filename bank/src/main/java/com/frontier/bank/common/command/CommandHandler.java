package com.frontier.bank.common.command;

/**
 * Handler de um comando (lado de escrita do CQRS).
 * <p>
 * Cada comando tem exatamente um handler; o {@link CommandBus} roteia a
 * instância do comando para o handler registrado. Handlers executam em sua
 * própria transação e são o ponto único de mutação do agregado — prontos para
 * serem acionados por mensageria (ex.: Kafka) quando os domain events chegarem.
 *
 * @param <C> tipo do comando
 * @param <R> tipo do resultado (ex.: {@code UUID} do agregado, {@code Void})
 */
@FunctionalInterface
public interface CommandHandler<C, R> {

	R handle(C command);

}
