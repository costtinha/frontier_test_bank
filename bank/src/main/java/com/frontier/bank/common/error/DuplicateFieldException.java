package com.frontier.bank.common.error;

/**
 * Lançada quando uma tentativa de gravação viola uma restrição de unicidade
 * de negócio (ex.: email ou CPF já cadastrados) — HTTP 409.
 */
public class DuplicateFieldException extends RuntimeException {

	public DuplicateFieldException(String resource, String field, String value) {
		super("Já existe %s com %s '%s'".formatted(resource, field, value));
	}

}
