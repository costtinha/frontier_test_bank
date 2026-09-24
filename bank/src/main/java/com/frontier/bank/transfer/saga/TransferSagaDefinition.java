package com.frontier.bank.transfer.saga;

import java.util.List;
import java.util.function.BiFunction;

import org.springframework.stereotype.Component;

import com.frontier.bank.saga.SagaDefinition;
import com.frontier.bank.saga.SagaStep;
import com.frontier.bank.transfer.command.CreditTransferTargetCommand;
import com.frontier.bank.transfer.command.DebitTransferSourceCommand;
import com.frontier.bank.transfer.command.FailTransferCommand;
import com.frontier.bank.transfer.command.RefundTransferDebitCommand;
import com.frontier.bank.transfer.command.RegisterCompletedTransferCommand;

/**
 * Definição da saga de transferência (orquestração):
 * <pre>
 *   1. debitSource              (compensação: refundDebit)
 *   2. creditTarget             (sem compensação: o que desfazer é o débito anterior)
 *   3. registerCompletedTransfer (efetiva o registro e publica TransferCompleted)
 * </pre>
 * Se um passo for rejeitado, os anteriores são compensados em ordem reversa e a
 * saga publica {@code TransferFailed}.
 * <p>
 * Quando as contas estiverem em serviços separados, esta definição continua a
 * mesma — o que muda é o transporte dos comandos (Kafka/REST em vez de
 * {@code CommandBus} in-process): a orquestração já é durável e idempotente.
 */
@Component
public class TransferSagaDefinition implements SagaDefinition<TransferSagaContext> {

	public static final String SAGA_TYPE = "MoneyTransferSaga";

	@Override
	public String sagaType() {
		return SAGA_TYPE;
	}

	@Override
	public Class<TransferSagaContext> contextType() {
		return TransferSagaContext.class;
	}

	@Override
	public List<SagaStep<TransferSagaContext>> steps() {
		return List.of(
				new SagaStep<>("debitSource",
						context -> new DebitTransferSourceCommand(context.transferId(), context.sourceUserId(),
								context.amount()),
						context -> new RefundTransferDebitCommand(context.transferId(), context.sourceUserId(),
								context.amount())),

				new SagaStep<>("creditTarget",
						context -> new CreditTransferTargetCommand(context.transferId(), context.targetUserId(),
								context.amount()),
						null),

				new SagaStep<>("registerCompletedTransfer",
						context -> new RegisterCompletedTransferCommand(context.transferId(),
								context.sourceUserId(), context.targetUserId(), context.amount(),
								context.idempotencyKey()),
						null));
	}

	@Override
	public BiFunction<TransferSagaContext, String, Object> failureCommand() {
		return (context, reason) -> new FailTransferCommand(context.transferId(), context.sourceUserId(),
				context.targetUserId(), context.amount(), reason);
	}

}
