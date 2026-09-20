package com.frontier.bank.ledger;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.frontier.bank.common.pagination.CursorDirection;
import com.frontier.bank.common.pagination.CursorPage;
import com.frontier.bank.ledger.query.StatementEntry;
import com.frontier.bank.ledger.query.StatementQueryService;

/**
 * Extrato do cliente: leitura paginada do razão (fonte da verdade contábil).
 */
@RestController
@RequestMapping("/api/users/{userId}/statement")
public class LedgerController {

	private final StatementQueryService statementQueryService;

	public LedgerController(StatementQueryService statementQueryService) {
		this.statementQueryService = statementQueryService;
	}

	@GetMapping
	public CursorPage<StatementEntry> statement(
			@PathVariable UUID userId,
			@RequestParam(defaultValue = "20") int limit,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "FORWARD") CursorDirection direction) {
		return statementQueryService.findStatement(userId, limit, cursor, direction);
	}

}
