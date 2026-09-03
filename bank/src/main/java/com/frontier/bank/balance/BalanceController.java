package com.frontier.bank.balance;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frontier.bank.balance.command.DepositCommand;
import com.frontier.bank.balance.command.WithdrawCommand;
import com.frontier.bank.balance.dto.DepositRequest;
import com.frontier.bank.balance.dto.WithdrawRequest;
import com.frontier.bank.balance.query.BalanceQueryService;
import com.frontier.bank.balance.query.BalanceSnapshot;
import com.frontier.bank.common.command.CommandBus;

import jakarta.validation.Valid;

/**
 * Camada de apresentação do saldo sob CQRS:
 * <ul>
 *   <li><b>Escrita</b> (depósito/saque): monta comandos e despacha pelo
 *       {@link CommandBus} — nunca lê direto;</li>
 *   <li><b>Leitura</b> (GET): delega ao {@link BalanceQueryService} — nunca
 *       muta estado.</li>
 * </ul>
 * Após comandos, a representação é composta pelo lado de query
 * (read-your-writes), mantendo o contrato HTTP.
 */
@RestController
@RequestMapping("/api/users/{userId}/balance")
public class BalanceController {

	private final CommandBus commandBus;
	private final BalanceQueryService queryService;

	public BalanceController(CommandBus commandBus, BalanceQueryService queryService) {
		this.commandBus = commandBus;
		this.queryService = queryService;
	}

	@GetMapping
	public BalanceSnapshot getBalance(@PathVariable UUID userId) {
		return queryService.getBalance(userId);
	}

	@PostMapping("/deposit")
	public BalanceSnapshot deposit(@PathVariable UUID userId, @Valid @RequestBody DepositRequest request) {
		commandBus.dispatch(new DepositCommand(userId, request.amount()));
		return queryService.getBalance(userId);
	}

	@PostMapping("/withdraw")
	public BalanceSnapshot withdraw(@PathVariable UUID userId, @Valid @RequestBody WithdrawRequest request) {
		commandBus.dispatch(new WithdrawCommand(userId, request.amount()));
		return queryService.getBalance(userId);
	}

}
