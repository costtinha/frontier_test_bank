package com.frontier.bank.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.user.UserRole;
import com.frontier.bank.user.event.UserRegistered;

class AuditHasherTest {

	@Test
	void shouldProduceDeterministicHashForSameContent() {
		DomainEvent<?> event = DomainEvent.of(new UserRegistered(UUID.randomUUID(), "João",
				"joao@example.com", UserRole.USER));

		AuditRecord first = AuditRecord.of(event, "{\"a\":1}", AuditHasher.GENESIS_HASH);
		AuditRecord second = AuditRecord.of(event, "{\"a\":1}", AuditHasher.GENESIS_HASH);
		ReflectionTestUtils.setField(second, "recordedAt", first.getRecordedAt());

		assertThat(second.getRecordHash()).isEqualTo(first.getRecordHash());
	}

	@Test
	void shouldChainHashToPrevious() {
		DomainEvent<?> event = DomainEvent.of(new UserRegistered(UUID.randomUUID(), "João",
				"joao@example.com", UserRole.USER));

		AuditRecord first = AuditRecord.of(event, "{\"a\":1}", AuditHasher.GENESIS_HASH);
		AuditRecord second = AuditRecord.of(event, "{\"a\":1}", first.getRecordHash());

		assertThat(second.getPreviousHash()).isEqualTo(first.getRecordHash());
		assertThat(second.getRecordHash()).isNotEqualTo(first.getRecordHash());
		assertThat(second.getRecordHash())
				.isEqualTo(AuditHasher.hash(first.getRecordHash(), second));
	}

	@Test
	void shouldChangeHashWhenContentChanges() {
		DomainEvent<?> event = DomainEvent.of(new UserRegistered(UUID.randomUUID(), "João",
				"joao@example.com", UserRole.USER));

		AuditRecord record = AuditRecord.of(event, "{\"a\":1}", AuditHasher.GENESIS_HASH);
		String originalHash = record.getRecordHash();

		// simula adulteração do conteúdo armazenado
		ReflectionTestUtils.setField(record, "payload", "{\"a\":2}");

		assertThat(AuditHasher.hash(record.getPreviousHash(), record)).isNotEqualTo(originalHash);
	}

	@Test
	void shouldCopyEventMetadataIntoRecord() {
		DomainEvent<?> event = DomainEvent.of(new UserRegistered(UUID.randomUUID(), "João",
				"joao@example.com", UserRole.USER));

		AuditRecord record = AuditRecord.of(event, "{}", AuditHasher.GENESIS_HASH);

		assertThat(record.getEventId()).isEqualTo(event.eventId());
		assertThat(record.getEventType()).isEqualTo(event.eventType());
		assertThat(record.getCorrelationId()).isEqualTo(event.correlationId());
		assertThat(record.getPreviousHash()).isEqualTo(AuditHasher.GENESIS_HASH);
		assertThat(record.getRecordHash()).isNotBlank();
	}

}
