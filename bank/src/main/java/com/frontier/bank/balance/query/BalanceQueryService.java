package com.frontier.bank.balance.query;

import java.util.UUID;

/**
 * Contrato do lado de LEITURA do saldo (CQRS): consultas dedicadas, somente
 * leitura, sem efeitos colaterais.
 */
public interface BalanceQueryService {

	BalanceSnapshot getBalance(UUID userId);

}
