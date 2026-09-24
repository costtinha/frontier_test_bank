package com.frontier.bank.common.error;

/**
 * Lançada quando um recurso solicitado não existe (HTTP 404).
 * <p>
 * Também é uma {@link BusinessRejection}: em uma saga, recurso ausente não se
 * resolve com retry — o passo é tratado como rejeição de negócio.
 */
public class ResourceNotFoundException extends RuntimeException implements BusinessRejection {

	public ResourceNotFoundException(String resource, Object id) {
		super("%s não encontrado(a): %s".formatted(resource, id));
	}

}
