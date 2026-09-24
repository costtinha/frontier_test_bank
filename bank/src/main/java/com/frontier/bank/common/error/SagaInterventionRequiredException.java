package com.frontier.bank.common.error;

/**
 * Lançada quando uma saga não conclui e também não consegue compensar — pode
 * haver efeito residual (dinheiro debitado sem crédito, por exemplo). Não é erro
 * do cliente: exige intervenção operacional — HTTP 500.
 */
public class SagaInterventionRequiredException extends RuntimeException {

	public SagaInterventionRequiredException(String reason) {
		super("Saga requer intervenção operacional: " + reason);
	}

}
