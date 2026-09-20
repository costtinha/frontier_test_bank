package com.frontier.bank.user.event;

import java.util.UUID;

import com.frontier.bank.common.event.EventPayload;
import com.frontier.bank.user.UserRole;

/**
 * Fato: novo cliente cadastrado no banco.
 * <p>
 * Minimização de dados (LGPD): o <b>CPF não trafega</b> no evento — consumidores
 * que precisarem dele devem consultar a API de query ou um evento com finalidade
 * específica. Nome e email vão no payload por serem necessários ao consumidor de
 * notificação (boas-vindas).
 */
public record UserRegistered(
		UUID userId,
		String name,
		String email,
		UserRole role) implements EventPayload {

	public static final String TYPE = "bank.user.UserRegistered";
	public static final String AGGREGATE = "User";

	@Override
	public UUID aggregateId() {
		return userId;
	}

	@Override
	public String eventType() {
		return TYPE;
	}

	@Override
	public String aggregateType() {
		return AGGREGATE;
	}

}
