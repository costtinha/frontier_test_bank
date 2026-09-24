package com.frontier.bank.transfer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frontier.bank.common.command.CommandBus;
import com.frontier.bank.common.error.SagaInterventionRequiredException;
import com.frontier.bank.common.error.TransferRejectedException;
import com.frontier.bank.transfer.command.TransferMoneyCommand;
import com.frontier.bank.transfer.command.TransferResult;
import com.frontier.bank.transfer.dto.TransferRequest;
import com.frontier.bank.transfer.dto.TransferView;

import jakarta.validation.Valid;

/**
 * Camada de apresentação da transferência: monta o comando, despacha pelo
 * {@link CommandBus} e traduz o desfecho <b>da saga</b> em HTTP.
 * <ul>
 *   <li>concluída → <b>200</b> com o resultado final;</li>
 *   <li>ainda processando (retry agendado) → <b>202 Accepted</b>, mesmo contrato de
 *       leitura: a saga é durável e o sweeper retoma sozinho;</li>
 *   <li>rejeitada por regra de negócio (compensada sem efeito residual) → <b>422</b>;</li>
 *   <li>não concluiu nem compensou → <b>500</b> (requer intervenção operacional).</li>
 * </ul>
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
	public ResponseEntity<TransferView> transfer(@RequestHeader(name = IDEMPOTENCY_HEADER) String idempotencyKey,
			@Valid @RequestBody TransferRequest request) {

		TransferResult result = commandBus.dispatch(new TransferMoneyCommand(
				request.sourceUserId(), request.targetUserId(), request.amount(), idempotencyKey));

		if (result.requiresIntervention()) {
			throw new SagaInterventionRequiredException(result.failureReason());
		}
		if (result.isRejected()) {
			throw new TransferRejectedException(result.failureReason());
		}

		TransferView view = TransferView.from(result);
		return result.isPending()
				? ResponseEntity.accepted().body(view)
				: ResponseEntity.ok(view);
	}

}
