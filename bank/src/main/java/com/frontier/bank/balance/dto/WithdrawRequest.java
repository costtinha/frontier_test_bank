package com.frontier.bank.balance.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record WithdrawRequest(
		@NotNull(message = "valor é obrigatório")
		@DecimalMin(value = "0.01", message = "valor deve ser maior que zero")
		@Digits(integer = 17, fraction = 2, message = "valor deve ter no máximo 17 dígitos inteiros e 2 casas decimais")
		BigDecimal amount) {
}
