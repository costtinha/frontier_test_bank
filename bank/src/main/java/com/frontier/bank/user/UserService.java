package com.frontier.bank.user;

import java.util.UUID;

import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.user.dto.CreateUserRequest;
import com.frontier.bank.user.dto.UpdateUserRequest;
import com.frontier.bank.user.dto.UserResponse;

/**
 * Contrato de serviço do usuário. Toda a regra de negócio (validações,
 * unicidade, criptografia de senha e paginação) vive aqui; o controller
 * apenas delega.
 */
public interface UserService {

	UserResponse create(CreateUserRequest request);

	UserResponse findById(UUID id);

	UserResponse update(UUID id, UpdateUserRequest request);

	void delete(UUID id);

	/**
	 * @param limit     tamanho máximo da página (ajustado para o intervalo 1..100)
	 * @param cursor    cursor opaco retornado pela página anterior; {@code null} na primeira/última página
	 * @param direction direção da navegação
	 */
	CursorPage<UserResponse> findAll(int limit, String cursor, CursorDirection direction);

}
