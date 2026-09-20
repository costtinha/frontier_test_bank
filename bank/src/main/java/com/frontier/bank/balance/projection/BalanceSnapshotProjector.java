package com.frontier.bank.balance.projection;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.frontier.bank.balance.event.BalanceOpened;
import com.frontier.bank.balance.event.MoneyDeposited;
import com.frontier.bank.balance.event.MoneyMovement;
import com.frontier.bank.balance.event.MoneyWithdrawn;
import com.frontier.bank.balance.query.BalanceSnapshotView;
import com.frontier.bank.balance.query.BalanceSnapshotViewRepository;
import com.frontier.bank.common.event.EventMessage;
import com.frontier.bank.common.projection.ProjectionHandler;

import tools.jackson.databind.ObjectMapper;

/**
 * Projeta o read model de saldo a partir dos eventos de conta.
 * <p>
 * Regras que garantem projeção correta sob entrega at-least-once e reordenação:
 * <ul>
 *   <li>o saldo é sempre <b>absoluto</b> ({@code resultingBalance} do evento), nunca uma soma;</li>
 *   <li>evento mais antigo que o estado atual é ignorado (mantém o mais recente);</li>
 *   <li>movimentação sem linha no read model cria a linha (auto-recuperação).</li>
 * </ul>
 */
@Component
public class BalanceSnapshotProjector implements ProjectionHandler {

	public static final String CONSUMER = "balance-snapshot";

	private final BalanceSnapshotViewRepository repository;
	private final ObjectMapper objectMapper;

	public BalanceSnapshotProjector(BalanceSnapshotViewRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@Override
	public String consumerName() {
		return CONSUMER;
	}

	@Override
	public Set<String> supportedEventTypes() {
		return Set.of(BalanceOpened.TYPE, MoneyDeposited.TYPE, MoneyWithdrawn.TYPE);
	}

	@Override
	public void apply(EventMessage event) {
		switch (event.eventType()) {
			case BalanceOpened.TYPE -> open(read(event, BalanceOpened.class), event);
			case MoneyDeposited.TYPE -> move(read(event, MoneyDeposited.class), event);
			case MoneyWithdrawn.TYPE -> move(read(event, MoneyWithdrawn.class), event);
			default -> throw new IllegalArgumentException("Evento não suportado: " + event.eventType());
		}
	}

	private void open(BalanceOpened opened, EventMessage event) {
		repository.findByUserId(opened.userId()).ifPresentOrElse(
				existing -> existing.apply(opened.amount(), event.occurredAt(), event.eventId()),
				() -> repository.save(BalanceSnapshotView.open(opened.balanceId(), opened.userId(),
						opened.amount(), event.occurredAt(), event.eventId())));
	}

	private void move(MoneyMovement movement, EventMessage event) {
		Optional<BalanceSnapshotView> existing = repository.findByUserId(movement.userId());
		if (existing.isPresent() && existing.get().getUpdatedAt().isAfter(event.occurredAt())) {
			// evento fora de ordem: o read model já reflete um estado mais recente
			return;
		}

		BalanceSnapshotView view = existing
				.map(current -> {
					current.apply(movement.resultingBalance(), event.occurredAt(), event.eventId());
					return current;
				})
				.orElseGet(() -> BalanceSnapshotView.open(movement.balanceId(), movement.userId(),
						movement.resultingBalance(), event.occurredAt(), event.eventId()));

		repository.save(view);
	}

	private <T> T read(EventMessage event, Class<T> type) {
		return objectMapper.treeToValue(event.payload(), type);
	}

}
