package com.frontier.bank.common.event;

import java.time.Instant;
import java.util.UUID;

import com.frontier.bank.common.context.CorrelationContext;

/**
 * Envelope de um evento de domínio: um fato de negócio, no passado e imutável.
 * <p>
 * O envelope é o contrato de fio — carrega identidade (para idempotência),
 * roteamento (agregado), rastreabilidade (correlação/causa) e versão de schema.
 * O payload tipado fica em {@link EventPayload}.
 *
 * @param eventId       identidade única da mensagem (dedupe em consumidores)
 * @param eventType     nome canônico versionável (ex.: {@code bank.user.UserRegistered})
 * @param eventVersion  versão do schema do evento
 * @param aggregateType nome do agregado de origem
 * @param aggregateId   id do agregado (chave de particionamento → ordenação por agregado)
 * @param occurredAt    quando o fato aconteceu
 * @param correlationId rastreia a requisição ponta a ponta
 * @param causationId   qual comando causou o evento (preenchido quando os comandos tiverem id)
 * @param actorId       quem executou a ação (preenchido quando houver autenticação)
 * @param payload       corpo tipado do evento
 */
public record DomainEvent<T extends EventPayload>(
		UUID eventId,
		String eventType,
		int eventVersion,
		String aggregateType,
		UUID aggregateId,
		Instant occurredAt,
		String correlationId,
		String causationId,
		String actorId,
		T payload) {

	/**
	 * Monta o envelope a partir do payload — tipo, agregado e versão vêm do
	 * próprio evento, então é impossível publicar um evento com metadados errados.
	 */
	public static <T extends EventPayload> DomainEvent<T> of(T payload) {
		return new DomainEvent<>(
				UUID.randomUUID(),
				payload.eventType(),
				payload.version(),
				payload.aggregateType(),
				payload.aggregateId(),
				Instant.now(),
				CorrelationContext.getOrCreate(),
				null,
				null,
				payload);
	}

}
