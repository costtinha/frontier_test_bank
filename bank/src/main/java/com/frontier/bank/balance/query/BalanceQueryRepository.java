package com.frontier.bank.balance.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de LEITURA do saldo: expõe apenas consultas, sem nenhuma operação
 * de escrita.
 * <p>
 * Desde a Fase 2 lê o <b>read model</b> ({@link BalanceSnapshotView}), mantido
 * pelo projector a partir dos eventos — não toca mais na tabela de escrita. É por
 * aqui que o lado de leitura pode ser movido para uma réplica ou outro banco.
 */
public interface BalanceQueryRepository extends Repository<BalanceSnapshotView, UUID> {

	@Query("""
			SELECT new com.frontier.bank.balance.query.BalanceSnapshot(
				v.balanceId, v.userId, v.amount, v.createdAt, v.updatedAt)
			FROM BalanceSnapshotView v
			WHERE v.userId = :userId
			""")
	Optional<BalanceSnapshot> findSnapshotByUserId(@Param("userId") UUID userId);

}
