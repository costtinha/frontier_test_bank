package com.frontier.bank.common.event.schema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import com.frontier.bank.common.error.EventSchemaViolationException;
import com.frontier.bank.common.event.EventPayload;
import com.frontier.bank.common.support.Hashing;

/**
 * Registra no arranque todos os contratos de evento existentes no código.
 * <p>
 * A descoberta é feita varrendo o classpath por implementações de
 * {@link EventPayload} — adicionar um evento novo é só criar o record. Se algum
 * contrato tiver evoluído de forma incompatível sem incrementar a versão, a
 * aplicação <b>não sobe</b>: o erro aparece no deploy, não em produção.
 */
@Component
public class EventSchemaRegistrar implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(EventSchemaRegistrar.class);
	private static final String BASE_PACKAGE = "com.frontier.bank";

	private final EventSchemaRegistry registry;

	public EventSchemaRegistrar(EventSchemaRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void run(ApplicationArguments args) {
		registry.loadPersistedSchemas();

		List<Class<? extends EventPayload>> payloadTypes = scanPayloadTypes();
		payloadTypes.forEach(type -> registry.register(EventSchemaDeriver.derive(type)));

		log.info("Schema registry pronto: {} tipo(s) de evento registrado(s) [fingerprint global {}]",
				payloadTypes.size(), globalFingerprint(payloadTypes));
	}

	private String globalFingerprint(List<Class<? extends EventPayload>> payloadTypes) {
		String canonical = payloadTypes.stream()
				.map(EventSchemaDeriver::derive)
				.map(schema -> schema.eventType() + ":" + schema.version() + ":" + schema.fingerprint())
				.sorted()
				.reduce("", (left, right) -> left + "|" + right);
		return Hashing.sha256(canonical).substring(0, 12);
	}

	private List<Class<? extends EventPayload>> scanPayloadTypes() {
		ClassPathScanningCandidateComponentProvider scanner =
				new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AssignableTypeFilter(EventPayload.class));

		List<Class<? extends EventPayload>> types = new ArrayList<>();
		for (BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
			Class<?> loaded = load(candidate.getBeanClassName());
			if (loaded != null && loaded.isRecord() && EventPayload.class.isAssignableFrom(loaded)) {
				types.add(loaded.asSubclass(EventPayload.class));
			}
		}
		types.sort(Comparator.comparing(Class::getName));

		if (types.isEmpty()) {
			throw new EventSchemaViolationException(
					"Nenhum evento encontrado no classpath em " + BASE_PACKAGE + " — verifique o empacotamento");
		}
		return types;
	}

	private Class<?> load(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			log.warn("Classe de evento não carregada: {}", className);
			return null;
		}
	}

}
