package com.frontier.bank.common.projection.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operação dos read models: status, replay e reexecução de dead letters.
 * <p>
 * <b>ATENÇÃO:</b> são rotas administrativas — hoje liberadas pelo
 * {@code SecurityConfig} de desenvolvimento; ao entrar autenticação,
 * {@code /api/admin/**} deve ser restrito ao papel {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/admin/projections")
public class ProjectionAdminController {

	private final ProjectionAdminService service;

	public ProjectionAdminController(ProjectionAdminService service) {
		this.service = service;
	}

	@GetMapping
	public List<ProjectionStatus> status() {
		return service.status();
	}

	/** Rebobina o consumidor e reprocessa o log (a projeção é idempotente). */
	@PostMapping("/{consumer}/replay")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ProjectionStatus replay(@PathVariable String consumer,
			@RequestParam(required = false) Instant from) {
		return service.replay(consumer, from);
	}

	/** Reenfileira um evento que está na dead letter. */
	@PostMapping("/{consumer}/dead-letters/{eventId}/retry")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ProjectionStatus retry(@PathVariable String consumer, @PathVariable UUID eventId) {
		return service.retryDeadLetter(consumer, eventId);
	}

}
