package com.frontier.bank.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.error.DuplicateFieldException;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.common.pagination.CursorCodec;
import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.user.dto.CreateUserRequest;
import com.frontier.bank.user.dto.UpdateUserRequest;
import com.frontier.bank.user.dto.UserResponse;

@Service
public class UserServiceImpl implements UserService {

	private static final int MAX_PAGE_SIZE = 100;

	private final UserRepository repository;
	private final PasswordEncoder passwordEncoder;

	public UserServiceImpl(UserRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public UserResponse create(CreateUserRequest request) {
		ensureEmailAvailable(request.email(), null);
		ensureCpfAvailable(request.cpf(), null);

		User user = new User(
				request.name().trim(),
				normalizeEmail(request.email()),
				request.cpf(),
				passwordEncoder.encode(request.password()));
		if (request.role() != null) {
			user.setRole(request.role());
		}
		return toResponse(repository.save(user));
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse findById(UUID id) {
		return toResponse(findEntity(id));
	}

	@Override
	@Transactional
	public UserResponse update(UUID id, UpdateUserRequest request) {
		User user = findEntity(id);

		if (request.name() != null) {
			user.setName(request.name().trim());
		}
		if (request.email() != null) {
			String email = normalizeEmail(request.email());
			ensureEmailAvailable(email, id);
			user.setEmail(email);
		}
		if (request.password() != null) {
			user.setPassword(passwordEncoder.encode(request.password()));
		}
		if (request.role() != null) {
			user.setRole(request.role());
		}
		if (request.active() != null) {
			user.setActive(request.active());
		}
		// saveAndFlush garante que @UpdateTimestamp reflita updated_at antes da resposta
		return toResponse(repository.saveAndFlush(user));
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		repository.delete(findEntity(id));
	}

	@Override
	@Transactional(readOnly = true)
	public CursorPage<UserResponse> findAll(int limit, String cursor, CursorDirection direction) {
		int pageSize = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
		// Busca um item extra apenas para detectar se existe próxima/anterior página.
		Pageable pageable = Pageable.ofSize(pageSize + 1);

		return switch (direction) {
			case FORWARD -> findForward(pageable, pageSize, cursor);
			case BACKWARD -> findBackward(pageable, pageSize, cursor);
		};
	}

	/**
	 * Avança a partir do cursor (ou retorna a primeira página quando não há cursor).
	 * Ordem de consulta: {@code created_at ASC, id ASC}.
	 */
	private CursorPage<UserResponse> findForward(Pageable pageable, int pageSize, String cursor) {
		CursorCodec.Cursor decoded = decode(cursor);
		List<User> rows;
		boolean hasPrevious;

		if (decoded == null) {
			rows = repository.findFirstPage(pageable);
			hasPrevious = false;
		} else {
			rows = repository.findAfter(decoded.createdAt(), decoded.id(), pageable);
			hasPrevious = true;
		}

		boolean hasNext = rows.size() > pageSize;
		List<User> items = hasNext ? rows.subList(0, pageSize) : rows;
		return toPage(items, hasNext, hasPrevious);
	}

	/**
	 * Volta a partir do cursor (ou retorna a última página quando não há cursor).
	 * Consulta em ordem descendente e reverte para devolver a página na ordem
	 * de exibição (ascendente).
	 */
	private CursorPage<UserResponse> findBackward(Pageable pageable, int pageSize, String cursor) {
		CursorCodec.Cursor decoded = decode(cursor);
		List<User> rows;
		boolean hasNext;

		if (decoded == null) {
			rows = repository.findLastPage(pageable);
			hasNext = false;
		} else {
			rows = repository.findBefore(decoded.createdAt(), decoded.id(), pageable);
			hasNext = true;
		}

		boolean hasPrevious = rows.size() > pageSize;
		List<User> items = hasPrevious ? rows.subList(0, pageSize) : rows;
		List<User> display = new ArrayList<>(items);
		java.util.Collections.reverse(display);
		return toPage(display, hasNext, hasPrevious);
	}

	private CursorCodec.Cursor decode(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		return CursorCodec.decode(cursor);
	}

	/**
	 * Monta a resposta com os cursores de navegação:
	 * {@code nextCursor} aponta para o último item da página e
	 * {@code previousCursor} para o primeiro.
	 */
	private CursorPage<UserResponse> toPage(List<User> items, boolean hasNext, boolean hasPrevious) {
		String nextCursor = null;
		String previousCursor = null;

		if (hasNext && !items.isEmpty()) {
			User last = items.get(items.size() - 1);
			nextCursor = CursorCodec.encode(last.getCreatedAt(), last.getId());
		}
		if (hasPrevious && !items.isEmpty()) {
			User first = items.get(0);
			previousCursor = CursorCodec.encode(first.getCreatedAt(), first.getId());
		}

		List<UserResponse> responses = items.stream().map(this::toResponse).toList();
		return CursorPage.of(responses, nextCursor, previousCursor, hasNext, hasPrevious);
	}

	private User findEntity(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("usuário", id));
	}

	private void ensureEmailAvailable(String email, UUID currentId) {
		String normalized = normalizeEmail(email);
		repository.findByEmail(normalized)
				.filter(existing -> currentId == null || !existing.getId().equals(currentId))
				.ifPresent(existing -> {
					throw new DuplicateFieldException("usuário", "email", normalized);
				});
	}

	private void ensureCpfAvailable(String cpf, UUID currentId) {
		repository.findByCpf(cpf)
				.filter(existing -> currentId == null || !existing.getId().equals(currentId))
				.ifPresent(existing -> {
					throw new DuplicateFieldException("usuário", "cpf", cpf);
				});
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getCpf(),
				user.getRole(),
				user.isActive(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

}
