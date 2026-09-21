package com.frontier.bank.common.event.schema;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.frontier.bank.common.support.Hashing;

/**
 * Schema declarado de um tipo de evento: versão e campos (nome + tipo), na ordem
 * em que aparecem no payload.
 *
 * @param eventType nome canônico (ex.: {@code bank.user.UserRegistered})
 * @param version   versão do contrato
 * @param fields    campos do payload
 */
public record EventSchema(String eventType, int version, List<Field> fields) {

	public EventSchema {
		fields = List.copyOf(fields);
	}

	public record Field(String name, String type) {
	}

	/** Muda quando qualquer campo é adicionado, removido ou tem o tipo alterado. */
	public String fingerprint() {
		return Hashing.sha256(serializedFields());
	}

	public Set<String> fieldNames() {
		return fields.stream()
				.map(Field::name)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** Formato de persistência: {@code nome:tipo,nome:tipo}. */
	public String serializedFields() {
		return fields.stream()
				.map(field -> field.name() + ":" + field.type())
				.collect(Collectors.joining(","));
	}

	public static EventSchema fromSerialized(String eventType, int version, String serializedFields) {
		List<Field> parsed = serializedFields.isBlank()
				? List.of()
				: java.util.Arrays.stream(serializedFields.split(","))
						.map(entry -> entry.split(":", 2))
						.map(parts -> new Field(parts[0], parts.length > 1 ? parts[1] : "unknown"))
						.toList();
		return new EventSchema(eventType, version, parsed);
	}

}
