package com.frontier.bank.audit;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface AuditChainGuardRepository extends JpaRepository<AuditChainGuard, Short> {

	/** Trava a linha de guarda: serializa a escrita da cadeia entre instâncias. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AuditChainGuard> findById(Short id);

}
