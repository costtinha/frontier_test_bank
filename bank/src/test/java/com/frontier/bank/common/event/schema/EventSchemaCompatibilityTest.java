package com.frontier.bank.common.event.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.user.event.UserRegistered;

class EventSchemaCompatibilityTest {

	private static final EventSchema.Field CLIENTE = new EventSchema.Field("clienteId", "UUID");
	private static final EventSchema.Field VALOR = new EventSchema.Field("valor", "BigDecimal");

	@Test
	void shouldAllowAdditiveChange() {
		EventSchema previous = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE, VALOR));
		EventSchema current = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE, VALOR,
				new EventSchema.Field("descricao", "String")));

		assertThat(EventSchemaCompatibility.compare(previous, current))
				.isEqualTo(EventSchemaCompatibility.Verdict.COMPATIBLE);
	}

	@Test
	void shouldRejectRemovedField() {
		EventSchema previous = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE, VALOR));
		EventSchema current = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE));

		assertThat(EventSchemaCompatibility.compare(previous, current))
				.isEqualTo(EventSchemaCompatibility.Verdict.BREAKING);
	}

	@Test
	void shouldRejectChangedFieldType() {
		EventSchema previous = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE, VALOR));
		EventSchema current = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE,
				new EventSchema.Field("valor", "String")));

		assertThat(EventSchemaCompatibility.compare(previous, current))
				.isEqualTo(EventSchemaCompatibility.Verdict.BREAKING);
	}

	@Test
	void shouldDeriveSchemaFromPayloadRecords() {
		EventSchema userSchema = EventSchemaDeriver.derive(UserRegistered.class);

		assertThat(userSchema.eventType()).isEqualTo(UserRegistered.TYPE);
		assertThat(userSchema.version()).isEqualTo(1);
		assertThat(userSchema.fieldNames()).containsExactly("userId", "name", "email", "role");

		EventSchema balanceSchema = EventSchemaDeriver.derive(MoneyDeposited.class);

		assertThat(balanceSchema.eventType()).isEqualTo(MoneyDeposited.TYPE);
		assertThat(balanceSchema.fields()).extracting(EventSchema.Field::type)
				.contains("BigDecimal", "UUID");
	}

	@Test
	void shouldChangeFingerprintWhenFieldsChange() {
		EventSchema schema = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE, VALOR));
		EventSchema other = new EventSchema("bank.x.Evento", 1, List.of(CLIENTE));

		assertThat(schema.fingerprint()).hasSize(64).isNotEqualTo(other.fingerprint());
		assertThat(schema.fieldNames()).hasSize(2);
		assertThat(UUID.randomUUID()).isNotNull();
	}

}
