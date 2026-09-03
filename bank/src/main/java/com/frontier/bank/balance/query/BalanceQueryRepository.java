package com.frontier.bank.balance.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.frontier.bank.balance.Balance;

/**
 * Repositório de LEITURA do saldo: expõe apenas consultas (projeções), sem
 * nenhuma operação de escrita. Pode ser apontado futuramente para uma réplica
 * de leitura ou para um read model materializado por domain events.
 */
public interface BalanceQueryRepository extends Repository<Balance, UUID> {

	@Query("""
			SELECT new com.frontier.bank.balance.query.BalanceSnapshot(
				b.id, b.user.id, b.amount, b.createdAt, b.updatedAt)
			FROM Balance b
			WHERE b.user.id = :userId
			""")
	Optional<BalanceSnapshot> findSnapshotByUserId(@Param("userId") UUID userId);

}
