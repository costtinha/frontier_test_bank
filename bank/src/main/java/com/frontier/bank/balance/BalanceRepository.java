package com.frontier.bank.balance;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

	Optional<Balance> findByUserId(UUID userId);

	/**
	 * Lock pessimista de escrita ({@code SELECT ... FOR UPDATE}) para
	 * depósitos/saques atômicos — evita atualizações perdidas em concorrência.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT b FROM Balance b WHERE b.user.id = :userId")
	Optional<Balance> findByUserIdForUpdate(@Param("userId") UUID userId);

}
