package com.frontier.bank.balance.query;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.error.ResourceNotFoundException;

/**
 * Implementação do lado de leitura do saldo: leitura pura (read-only).
 * Graças à migração V3 (backfill) e à criação eager no cadastro do usuário,
 * todo usuário possui registro de saldo — o GET nunca escreve.
 */
@Service
public class BalanceQueryServiceImpl implements BalanceQueryService {

	private final BalanceQueryRepository queryRepository;

	public BalanceQueryServiceImpl(BalanceQueryRepository queryRepository) {
		this.queryRepository = queryRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public BalanceSnapshot getBalance(UUID userId) {
		return queryRepository.findSnapshotByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("saldo", userId));
	}

}
