package com.frontier.bank.user;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.frontier.bank.common.command.CommandBus;
import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.user.command.CreateUserCommand;
import com.frontier.bank.user.command.DeleteUserCommand;
import com.frontier.bank.user.command.UpdateUserCommand;
import com.frontier.bank.user.dto.CreateUserRequest;
import com.frontier.bank.user.dto.UpdateUserRequest;
import com.frontier.bank.user.query.UserDetails;
import com.frontier.bank.user.query.UserQueryService;
import com.frontier.bank.user.query.UserSummary;

import jakarta.validation.Valid;

/**
 * Camada de apresentação do usuário sob CQRS:
 * <ul>
 *   <li><b>Escrita</b> (POST/PATCH/DELETE): monta comandos e despacha pelo
 *       {@link CommandBus} — nunca lê/escreve direto;</li>
 *   <li><b>Leitura</b> (GET): delega ao {@link UserQueryService} — nunca
 *       muta estado.</li>
 * </ul>
 * Após comandos, a representação de resposta é composta pelo lado de query
 * (read-your-writes), mantendo o contrato HTTP.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final CommandBus commandBus;
	private final UserQueryService queryService;

	public UserController(CommandBus commandBus, UserQueryService queryService) {
		this.commandBus = commandBus;
		this.queryService = queryService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserDetails create(@Valid @RequestBody CreateUserRequest request) {
		UUID id = commandBus.dispatch(new CreateUserCommand(
				request.name(), request.email(), request.cpf(), request.password(), request.role()));
		return queryService.findById(id);
	}

	@GetMapping("/{id}")
	public UserDetails findById(@PathVariable UUID id) {
		return queryService.findById(id);
	}

	@GetMapping
	public CursorPage<UserSummary> findAll(
			@RequestParam(defaultValue = "20") int limit,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "FORWARD") CursorDirection direction) {
		return queryService.findAll(limit, cursor, direction);
	}

	@PatchMapping("/{id}")
	public UserDetails update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
		commandBus.dispatch(new UpdateUserCommand(
				id, request.name(), request.email(), request.password(), request.role(), request.active()));
		return queryService.findById(id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		commandBus.dispatch(new DeleteUserCommand(id));
	}

}
