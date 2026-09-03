package com.frontier.bank.balance.command;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.UserRepository;

/**
 * Pré-condições e acesso de escrita compartilhados pelos handlers de
 * depósito/saque: validação de valor, existência do usuário e lock pessimista
 * com criação defensiva. Uso exclusivo do pacote de comandos.
 */
final class BalanceWriteGuard {

	private final BalanceRepository balanceRepository;
	private final UserRepository userRepository;

	BalanceWriteGuard(BalanceRepository balanceRepository, UserRepository userRepository) {
		this.balanceRepository = balanceRepository;
		this.userRepository = userRepository;
	}

	void ensureUserExists(UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw new ResourceNotFoundException("usuário", userId);
		}
	}

	BigDecimal normalize(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("Valor deve ser maior que zero");
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * Bloqueia a linha do saldo para escrita ({@code FOR UPDATE}) antes de
	 * operar. Se o registro não existir (anomalia de dados), cria-se R$ 0,00
	 * defensivamente — o normal é todo usuário já nascer com saldo.
	 */
	Balance lockOrCreate(UUID userId) {
		return balanceRepository.findByUserIdForUpdate(userId)
				.orElseGet(() -> createBalance(userId));
	}

	private Balance createBalance(UUID userId) {
		try {
			return balanceRepository.saveAndFlush(new Balance(userRepository.getReferenceById(userId)));
		} catch (DataIntegrityViolationException e) {
			// corrida de criação: outro fluxo criou o saldo primeiro — relê com lock
			return balanceRepository.findByUserIdForUpdate(userId)
					.orElseThrow(() -> e);
		}
	}

}
