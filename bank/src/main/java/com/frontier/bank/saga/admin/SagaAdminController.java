package com.frontier.bank.saga.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.frontier.bank.saga.SagaState;

/**
 * Operação das sagas em execução.
 * <p>
 * <b>ATENÇÃO:</b> rotas administrativas — hoje liberadas pelo {@code SecurityConfig}
 * de desenvolvimento; ao entrar autenticação, {@code /api/admin/**} deve ser
 * restrito ao papel {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/admin/sagas")
public class SagaAdminController {

	private final SagaAdminService service;

	public SagaAdminController(SagaAdminService service) {
		this.service = service;
	}

	@GetMapping
	public List<SagaStatus> list(@RequestParam(required = false) SagaState state,
			@RequestParam(defaultValue = "50") int limit) {
		return service.list(state, limit);
	}

	/** Retoma a saga agora (útil quando a próxima tentativa está agendada para depois). */
	@PostMapping("/{sagaId}/retry")
	public SagaStatus retry(@PathVariable UUID sagaId) {
		return service.retry(sagaId);
	}

}
