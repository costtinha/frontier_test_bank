package com.frontier.bank.common.event.schema;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.frontier.bank.common.error.EventSchemaViolationException;
import com.frontier.bank.common.event.EventPayload;

/**
 * Deriva o schema de um evento a partir do próprio payload: os campos vêm dos
 * componentes do record e a identidade vem das constantes
 * {@code public static final String TYPE} (obrigatória) e
 * {@code public static final int VERSION} (opcional, padrão 1).
 * <p>
 * Assim o contrato acompanha o código — não há schema escrito à mão para ficar
 * desatualizado.
 */
public final class EventSchemaDeriver {

	private static final String TYPE_CONSTANT = "TYPE";
	private static final String VERSION_CONSTANT = "VERSION";

	private EventSchemaDeriver() {
	}

	public static EventSchema derive(Class<? extends EventPayload> payloadType) {
		String eventType = stringConstant(payloadType, TYPE_CONSTANT).orElseThrow(() -> new EventSchemaViolationException(
				"Evento %s deve declarar 'public static final String TYPE'".formatted(payloadType.getName())));

		return new EventSchema(eventType, intConstant(payloadType, VERSION_CONSTANT).orElse(1),
				fieldsOf(payloadType));
	}

	public static List<EventSchema.Field> fieldsOf(Class<?> payloadType) {
		RecordComponent[] components = payloadType.getRecordComponents();
		if (components == null) {
			throw new EventSchemaViolationException(
					"Payload de evento deve ser um record: " + payloadType.getName());
		}
		return Arrays.stream(components)
				.map(component -> new EventSchema.Field(component.getName(), component.getType().getSimpleName()))
				.toList();
	}

	public static List<String> fieldNamesOf(Class<?> payloadType) {
		return fieldsOf(payloadType).stream().map(EventSchema.Field::name).toList();
	}

	private static Optional<String> stringConstant(Class<?> type, String name) {
		return constant(type, name).filter(String.class::isInstance).map(String.class::cast);
	}

	private static Optional<Integer> intConstant(Class<?> type, String name) {
		return constant(type, name).filter(Integer.class::isInstance).map(Integer.class::cast);
	}

	private static Optional<Object> constant(Class<?> type, String name) {
		try {
			Field field = type.getField(name);
			return Optional.ofNullable(field.get(null));
		} catch (NoSuchFieldException e) {
			return Optional.empty();
		} catch (IllegalAccessException e) {
			throw new EventSchemaViolationException(
					"Constante %s de %s não é acessível".formatted(name, type.getName()));
		}
	}

}
