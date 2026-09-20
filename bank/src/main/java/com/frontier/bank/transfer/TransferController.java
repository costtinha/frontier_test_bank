package com.frontier.bank.transfer;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frontier.bank.common.command.CommandBus;
import com.frontier.bank.common.error.TransferRejectedException;
import com.frontier.bank.transfer.command.TransferMoneyCommand;
import com.frontier.bank.transfer.command.TransferResult;
import com.frontier.bank.transfer.dto.TransferRequest;
import com.frontier.bank.transfer.dto.TransferView;

import jakarta.validation.Valid;

/**
 * Camada de apresentação da transferência: monta o comando, despacha pelo
 * {@link CommandBus} e traduz o desfecho em HTTP (rejeição de negócio → 422).
 * <p>
 * Exige o cabeçalho {@code Idempotency-Key}: repetir a requisição com a mesma
 * chave devolve o desfecho original em vez de transferir de novo.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

	private final CommandBus commandBus;

	public TransferController(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@PostMapping
	public TransferView transfer(@RequestHeader(name = IDEMPOTENCY_HEADER) String idempotencyKey,
			@Valid @RequestBody TransferRequest request) {

		TransferResult result = commandBus.dispatch(new TransferMoneyCommand(
				request.sourceUserId(), request.targetUserId(), request.amount(), idempotencyKey));

		if (result.isFailed()) {
			throw new TransferRejectedException(result.failureReason());
		}
		return TransferView.from(result);
	}

}
