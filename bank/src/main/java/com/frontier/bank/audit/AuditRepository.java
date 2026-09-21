package com.frontier.bank.audit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

	/** Último registro da cadeia — base para encadear o próximo. */
	Optional<AuditRecord> findFirstByOrderByIdDesc();

	List<AuditRecord> findAllByOrderByIdDesc(Pageable pageable);

	Optional<AuditRecord> findByEventId(UUID eventId);

}
