package com.frontier.bank.common.event.schema;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regra de evolução do contrato de eventos.
 * <p>
 * <b>Aditivo é compatível</b> (consumidores ignoram campos novos); <b>remover
 * campo ou trocar o tipo é incompatível</b> e exige incrementar a versão do
 * evento — o registro falha no arranque caso isso seja violado.
 */
public final class EventSchemaCompatibility {

	public enum Verdict {
		COMPATIBLE,
		BREAKING
	}

	private EventSchemaCompatibility() {
	}

	public static Verdict compare(EventSchema previous, EventSchema current) {
		Map<String, String> currentTypes = current.fields().stream()
				.collect(Collectors.toMap(EventSchema.Field::name, EventSchema.Field::type));

		for (EventSchema.Field field : previous.fields()) {
			String currentType = currentTypes.get(field.name());
			if (currentType == null || !currentType.equals(field.type())) {
				return Verdict.BREAKING;
			}
		}
		return Verdict.COMPATIBLE;
	}

}
