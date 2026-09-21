package com.frontier.bank.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.user.UserRole;
import com.frontier.bank.user.event.UserRegistered;

@ExtendWith(MockitoExtension.class)
class AuditChainVerifierTest {

	@Mock
	private AuditRepository auditRepository;

	private AuditChainVerifier verifier;

	@BeforeEach
	void setUp() {
		verifier = new AuditChainVerifier(auditRepository);
	}

	@Test
	void shouldValidateIntactChain() {
		List<AuditRecord> chain = chainOf(3);
		when(auditRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(chain);

		AuditChainVerifier.Report report = verifier.verify();

		assertThat(report.valid()).isTrue();
		assertThat(report.records()).isEqualTo(3);
		assertThat(report.brokenAtId()).isNull();
	}

	@Test
	void shouldDetectTamperedContent() {
		List<AuditRecord> chain = chainOf(3);
		AuditRecord tampered = chain.get(1);
		ReflectionTestUtils.setField(tampered, "payload", "{\"adulterado\":true}");
		when(auditRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(chain);

		AuditChainVerifier.Report report = verifier.verify();

		assertThat(report.valid()).isFalse();
		assertThat(report.brokenAtId()).isEqualTo(tampered.getId());
		assertThat(report.reason()).contains("adulterado");
	}

	@Test
	void shouldDetectRemovedRecord() {
		List<AuditRecord> chain = chainOf(3);
		AuditRecord removed = chain.remove(1); // registro apagado da trilha
		when(auditRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(chain);

		AuditChainVerifier.Report report = verifier.verify();

		assertThat(report.valid()).isFalse();
		assertThat(report.brokenAtId()).isEqualTo(removed.getId() + 1); // o elo quebrado aparece no registro seguinte
		assertThat(report.reason()).contains("encadeamento");
	}

	/** Constrói uma cadeia íntegra de N registros, com ids sequenciais. */
	private List<AuditRecord> chainOf(int size) {
		java.util.ArrayList<AuditRecord> chain = new java.util.ArrayList<>();
		String previousHash = AuditHasher.GENESIS_HASH;

		for (int index = 1; index <= size; index++) {
			DomainEvent<?> event = DomainEvent.of(new UserRegistered(UUID.randomUUID(), "Cliente " + index,
					"cliente" + index + "@example.com", UserRole.USER));
			AuditRecord record = AuditRecord.of(event, "{\"indice\":" + index + "}", previousHash);
			ReflectionTestUtils.setField(record, "id", (long) index);
			chain.add(record);
			previousHash = record.getRecordHash();
		}
		return chain;
	}

}
