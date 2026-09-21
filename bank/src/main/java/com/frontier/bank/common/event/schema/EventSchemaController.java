package com.frontier.bank.common.event.schema;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato público dos eventos: consumidores (outros serviços) podem consultar
 * tipo, versão e campos de cada evento publicado pelo banco.
 */
@RestController
@RequestMapping("/api/events/schemas")
public class EventSchemaController {

	private final EventSchemaRegistry registry;

	public EventSchemaController(EventSchemaRegistry registry) {
		this.registry = registry;
	}

	@GetMapping
	public List<EventSchema> schemas() {
		return registry.schemas();
	}

}
