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

import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.user.dto.CreateUserRequest;
import com.frontier.bank.user.dto.UpdateUserRequest;
import com.frontier.bank.user.dto.UserResponse;

import jakarta.validation.Valid;

/**
 * Camada de apresentação: apenas mapeia HTTP → serviço.
 * Toda a lógica (CRUD e paginação) está no {@link UserService}.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
		return userService.create(request);
	}

	@GetMapping("/{id}")
	public UserResponse findById(@PathVariable UUID id) {
		return userService.findById(id);
	}

	@GetMapping
	public CursorPage<UserResponse> findAll(
			@RequestParam(defaultValue = "20") int limit,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "FORWARD") CursorDirection direction) {
		return userService.findAll(limit, cursor, direction);
	}

	@PatchMapping("/{id}")
	public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
		return userService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		userService.delete(id);
	}

}
