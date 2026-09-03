package com.frontier.bank.user.query;

import java.util.UUID;

import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;

/**
 * Contrato do lado de LEITURA (CQRS): consultas dedicadas, somente leitura,
 * sem efeitos colaterais. Evolui independentemente dos writers.
 */
public interface UserQueryService {

	UserDetails findById(UUID id);

	/**
	 * @param limit     tamanho máximo da página (ajustado para o intervalo 1..100)
	 * @param cursor    cursor opaco retornado pela página anterior; {@code null} na primeira/última página
	 * @param direction direção da navegação
	 */
	CursorPage<UserSummary> findAll(int limit, String cursor, CursorDirection direction);

}
