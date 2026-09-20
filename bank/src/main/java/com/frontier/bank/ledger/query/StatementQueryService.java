package com.frontier.bank.ledger.query;

import java.util.UUID;

import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;

/**
 * Contrato do lado de LEITURA do extrato: consultas somente leitura sobre o razão.
 */
public interface StatementQueryService {

	/**
	 * @param limit     tamanho máximo da página (ajustado para o intervalo 1..100)
	 * @param cursor    cursor opaco retornado pela página anterior; {@code null} na primeira/última página
	 * @param direction direção da navegação
	 */
	CursorPage<StatementEntry> findStatement(UUID userId, int limit, String cursor, CursorDirection direction);

}
