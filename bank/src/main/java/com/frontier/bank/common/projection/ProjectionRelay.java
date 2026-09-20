package com.frontier.bank.common.projection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara a projeção periodicamente (catch-up e reprocessamento).
 * <p>
 * A projeção em tempo real acontece no {@link ProjectionEventListener}
 * (pós-commit, na mesma requisição); este relay garante que nada fique pendente
 * quando aquela aplicação falha ou quando a aplicação cai entre o commit e a
 * projeção.
 */
@Component
public class ProjectionRelay {

	private static final Logger log = LoggerFactory.getLogger(ProjectionRelay.class);

	private final ProjectionDispatcher dispatcher;
	private final int batchSize;

	public ProjectionRelay(ProjectionDispatcher dispatcher,
			@Value("${bank.projection.batch-size:50}") int batchSize) {
		this.dispatcher = dispatcher;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${bank.projection.poll-interval:2000}")
	public void projectPendingEvents() {
		try {
			int projected = dispatcher.processPending(batchSize);
			if (projected > 0) {
				log.debug("Projeção aplicou {} evento(s)", projected);
			}
		} catch (RuntimeException e) {
			// não deixa o scheduler morrer por falha inesperada (ex.: banco fora)
			log.error("Falha ao processar projeções: {}", e.getMessage(), e);
		}
	}

}
