package com.frontier.bank.balance;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frontier.bank.balance.dto.BalanceResponse;
import com.frontier.bank.balance.dto.DepositRequest;
import com.frontier.bank.balance.dto.WithdrawRequest;

import jakarta.validation.Valid;

/**
 * Camada de apresentação do saldo: apenas mapeia HTTP → {@link BalanceService}.
 * Rotas aninhadas em /users/{userId} reforçam a relação 1:1 com o cliente.
 */
@RestController
@RequestMapping("/api/users/{userId}/balance")
public class BalanceController {

	private final BalanceService balanceService;

	public BalanceController(BalanceService balanceService) {
		this.balanceService = balanceService;
	}

	@GetMapping
	public BalanceResponse getBalance(@PathVariable UUID userId) {
		return balanceService.getBalance(userId);
	}

	@PostMapping("/deposit")
	public BalanceResponse deposit(@PathVariable UUID userId, @Valid @RequestBody DepositRequest request) {
		return balanceService.deposit(userId, request.amount());
	}

	@PostMapping("/withdraw")
	public BalanceResponse withdraw(@PathVariable UUID userId, @Valid @RequestBody WithdrawRequest request) {
		return balanceService.withdraw(userId, request.amount());
	}

}
