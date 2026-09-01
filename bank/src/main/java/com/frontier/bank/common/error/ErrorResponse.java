package com.frontier.bank.common.error;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Corpo padrão de erro da API.
 *
 * @param timestamp   momento em que o erro ocorreu
 * @param status      código HTTP
 * @param error       razão padrão do status HTTP
 * @param message     mensagem legível do erro
 * @param fieldErrors erros por campo (validação de bean), ou {@code null}
 */
public record ErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		Map<String, String> fieldErrors) {

	public static ErrorResponse of(HttpStatus status, String message) {
		return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, null);
	}

	public static ErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors) {
		return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
	}

}
