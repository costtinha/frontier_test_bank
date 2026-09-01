package com.frontier.bank.balance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.dto.BalanceResponse;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.UserRepository;

@Service
public class BalanceServiceImpl implements BalanceService {

	private final BalanceRepository balanceRepository;
	private final UserRepository userRepository;

	public BalanceServiceImpl(BalanceRepository balanceRepository, UserRepository userRepository) {
		this.balanceRepository = balanceRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public BalanceResponse getBalance(UUID userId) {
		ensureUserExists(userId);
		return toResponse(findOrCreate(userId));
	}

	@Override
	@Transactional
	public BalanceResponse deposit(UUID userId, BigDecimal amount) {
		ensureUserExists(userId);
		BigDecimal value = normalize(amount);
		Balance balance = lockOrCreate(userId);
		balance.deposit(value);
		return toResponse(balanceRepository.saveAndFlush(balance));
	}

	@Override
	@Transactional
	public BalanceResponse withdraw(UUID userId, BigDecimal amount) {
		ensureUserExists(userId);
		BigDecimal value = normalize(amount);
		Balance balance = lockOrCreate(userId);
		balance.withdraw(value);
		return toResponse(balanceRepository.saveAndFlush(balance));
	}

	/**
	 * Usuários criados antes deste recurso existir podem não ter saldo;
	 * cria-se então um saldo R$ 0,00 sob demanda. Novos usuários já nascem
	 * com saldo via {@code UserServiceImpl.create}.
	 */
	private Balance findOrCreate(UUID userId) {
		return balanceRepository.findByUserId(userId)
				.orElseGet(() -> createBalance(userId));
	}

	/**
	 * Bloqueia a linha do saldo para escrita ({@code FOR UPDATE}) antes de operar,
	 * tornando depósito/saque atômicos sob concorrência.
	 */
	private Balance lockOrCreate(UUID userId) {
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

	private void ensureUserExists(UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw new ResourceNotFoundException("usuário", userId);
		}
	}

	private BigDecimal normalize(BigDecimal amount) {
		if (amount == null || amount.signum() <= 0) {
			throw new IllegalArgumentException("Valor deve ser maior que zero");
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private BalanceResponse toResponse(Balance balance) {
		return new BalanceResponse(
				balance.getId(),
				balance.getUser().getId(),
				balance.getAmount(),
				balance.getCreatedAt(),
				balance.getUpdatedAt());
	}

}
