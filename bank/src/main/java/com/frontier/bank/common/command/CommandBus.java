package com.frontier.bank.common.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

/**
 * Roteia comandos para seus handlers registrados (in-process).
 * <p>
 * No arranque, coleta todos os beans {@link CommandHandler} e indexa pelo tipo
 * de comando declarado na interface genérica — independentemente de o bean
 * estar envolvido em proxy AOP (ex.: {@code @Transactional}).
 */
@Component
public class CommandBus {

	private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();

	public CommandBus(List<CommandHandler<?, ?>> handlerBeans) {
		for (CommandHandler<?, ?> handler : handlerBeans) {
			Class<?> targetClass = AopUtils.getTargetClass(handler);
			Class<?> commandType = ResolvableType.forClass(targetClass)
					.as(CommandHandler.class)
					.getGeneric(0)
					.resolve();
			if (commandType == null) {
				throw new IllegalStateException(
						"Não foi possível determinar o tipo de comando do handler " + targetClass.getName());
			}
			CommandHandler<?, ?> previous = handlers.putIfAbsent(commandType, handler);
			if (previous != null) {
				throw new IllegalStateException("Handler duplicado para o comando " + commandType.getName());
			}
		}
	}

	/**
	 * Executa o handler do comando na mesma thread (chamada síncrona).
	 *
	 * @throws IllegalArgumentException se não houver handler registrado
	 */
	@SuppressWarnings("unchecked")
	public <C, R> R dispatch(C command) {
		CommandHandler<C, R> handler = (CommandHandler<C, R>) handlers.get(command.getClass());
		if (handler == null) {
			throw new IllegalArgumentException(
					"Nenhum handler registrado para o comando " + command.getClass().getSimpleName());
		}
		return handler.handle(command);
	}

}
