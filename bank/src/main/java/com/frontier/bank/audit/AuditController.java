package com.frontier.bank.audit;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints administrativos da trilha de auditoria.
 * <p>
 * <b>ATENÇÃO:</b> hoje o {@code SecurityConfig} libera todas as rotas (ambiente
 * de desenvolvimento). Ao entrar autenticação, {@code /api/admin/**} deve ser
 * restrito ao papel {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/admin/audit")
public class AuditController {

	private static final int MAX_PAGE_SIZE = 200;

	private final AuditRepository auditRepository;
	private final AuditChainVerifier verifier;

	public AuditController(AuditRepository auditRepository, AuditChainVerifier verifier) {
		this.auditRepository = auditRepository;
		this.verifier = verifier;
	}

	/** Recalcula a cadeia e reporta a integridade da trilha. */
	@GetMapping("/verify")
	public AuditChainVerifier.Report verify() {
		return verifier.verify();
	}

	/** Últimos registros de auditoria (mais recentes primeiro). */
	@GetMapping
	public List<AuditRecord> latest(@RequestParam(defaultValue = "50") int limit) {
		int pageSize = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
		return auditRepository.findAllByOrderByIdDesc(PageRequest.ofSize(pageSize));
	}

}
