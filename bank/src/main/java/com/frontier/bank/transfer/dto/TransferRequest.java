package com.frontier.bank.transfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de transferência. A chave de idempotência vem no cabeçalho
 * {@code Idempotency-Key} (não faz parte do corpo da requisição).
 */
public record TransferRequest(
		@NotNull(message = "conta de origem é obrigatória")
		UUID sourceUserId,

		@NotNull(message = "conta de destino é obrigatória")
		UUID targetUserId,

		@NotNull(message = "valor é obrigatório")
		@DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
		@Digits(integer = 17, fraction = 2, message = "valor deve ter no máximo 17 dígitos inteiros e 2 casas decimais")
		BigDecimal amount) {
}
