package com.frontier.bank.common.error;

/**
 * Lançada quando um recurso solicitado não existe (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resource, Object id) {
		super("%s não encontrado(a): %s".formatted(resource, id));
	}

}
