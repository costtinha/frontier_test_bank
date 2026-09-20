package com.frontier.bank.balance;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/**
 * Repositório de ESCRITA do agregado Balance — usado exclusivamente pelos
 * handlers de comando (depósito, saque e transferência). Consultas de
 * apresentação vivem em {@code BalanceQueryRepository} (lado de leitura do CQRS).
 */
public interface BalanceRepository extends JpaRepository<Balance, UUID> {

	/**
	 * Lock pessimista de escrita ({@code SELECT ... FOR UPDATE}) para
	 * depósitos/saques atômicos — evita atualizações perdidas em concorrência.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT b FROM Balance b WHERE b.user.id = :userId")
	Optional<Balance> findByUserIdForUpdate(@Param("userId") UUID userId);

	/**
	 * Leitura sem lock usada pela transferência para descobrir os ids das contas
	 * <b>antes</b> de travá-las — é o que permite travar sempre na mesma ordem e
	 * evitar deadlock em transferências cruzadas.
	 */
	List<Balance> findAllByUserIdIn(Collection<UUID> userIds);

}
