package com.frontier.bank.balance.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de <b>escrita</b> do read model de saldo, usada exclusivamente pelo
 * projector. O lado de query lê por {@link BalanceQueryRepository}.
 */
public interface BalanceSnapshotViewRepository extends JpaRepository<BalanceSnapshotView, UUID> {

	Optional<BalanceSnapshotView> findByUserId(UUID userId);

}
