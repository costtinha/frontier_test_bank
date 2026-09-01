package com.frontier.bank.balance;

import java.math.BigDecimal;
import java.util.UUID;

import com.frontier.bank.balance.dto.BalanceResponse;

/**
 * Contrato de serviço do saldo. Toda a regra de negócio (validação de valor,
 * lock de concorrência, criação sob demanda) vive na implementação; o
 * controller apenas delega.
 */
public interface BalanceService {

	BalanceResponse getBalance(UUID userId);

	BalanceResponse deposit(UUID userId, BigDecimal amount);

	BalanceResponse withdraw(UUID userId, BigDecimal amount);

}
