package com.frontier.bank.saga;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.frontier.bank.common.error.ResourceNotFoundException;

/**
 * Registro das definições de saga do sistema. Um tipo de saga é identificado
 * pelo nome ({@code sagaType}) e resolvido para a definição correspondente.
 */
@Component
public class SagaRegistry {

	private final Map<String, SagaDefinition<?>> definitions;

	public SagaRegistry(List<SagaDefinition<?>> definitions) {
		this.definitions = definitions.stream()
				.collect(Collectors.toMap(SagaDefinition::sagaType, Function.identity(), (first, second) -> {
					throw new IllegalStateException("Saga duplicada registrada: " + first.sagaType());
				}, LinkedHashMap::new));
	}

	public SagaDefinition<?> definition(String sagaType) {
		SagaDefinition<?> definition = definitions.get(sagaType);
		if (definition == null) {
			throw new ResourceNotFoundException("definição de saga", sagaType);
		}
		return definition;
	}

	public Set<String> sagaTypes() {
		return definitions.keySet();
	}

	public int totalSteps(String sagaType) {
		return definition(sagaType).steps().size();
	}

}
