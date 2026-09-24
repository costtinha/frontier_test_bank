package com.frontier.bank.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Fronteira da arquitetura híbrida: <b>Core</b> (toca o razão, ACID,
 * single-writer) × <b>Periféricos</b> (read models, projeções, suporte,
 * eventualmente consistentes).
 * <p>
 * As regras abaixo são executáveis — se alguém cruzar a fronteira, o build quebra.
 * O que se protege aqui não é estilo: é a garantia de que nenhum componente de
 * suporte consegue escrever no livro-razão nem acoplar o core a leitura.
 */
class CorePeripheralBoundaryTest {

	private static JavaClasses classes;

	@BeforeAll
	static void importClasses() {
		classes = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("com.frontier.bank");
	}

	/**
	 * O core não pode depender do lado de leitura: quem decide sobre dinheiro não
	 * pode esperar read model, DTO de API ou projeção.
	 * <p>
	 * Controllers ficam de fora: são a camada de apresentação e compõem as duas
	 * pontas (comando + consulta).
	 */
	@Test
	void coreWriteSideMustNotDependOnReadSide() {
		noClasses()
				.that().resideInAnyPackage(
						"com.frontier.bank.user", "com.frontier.bank.user.command..",
						"com.frontier.bank.balance", "com.frontier.bank.balance.command..",
						"com.frontier.bank.transfer", "com.frontier.bank.transfer.command..",
						"com.frontier.bank.transfer.saga..", "com.frontier.bank.ledger")
				.and().haveSimpleNameNotEndingWith("Controller")
				.should().dependOnClassesThat()
				// padrões ancorados no nosso pacote base: "..query.." sozinho casaria
				// com pacotes de bibliotecas (ex.: org.springframework.data.repository.query)
				.resideInAnyPackage("com.frontier.bank..query..",
						"com.frontier.bank..projection..",
						"com.frontier.bank..dto..")
				.check(classes);
	}

	/** O lado de leitura não pode depender do lado de comando (CQRS de verdade). */
	@Test
	void readSideMustNotDependOnCommandSide() {
		noClasses()
				.that().resideInAnyPackage("com.frontier.bank..query..", "com.frontier.bank..projection..")
				.should().dependOnClassesThat()
				.resideInAnyPackage("com.frontier.bank..command..")
				.check(classes);
	}

	/**
	 * Periférico nunca escreve no core: nenhuma projeção ou consulta usa os
	 * repositórios de escrita. Escrever no razão é privilégio do core — através de
	 * seus handlers de comando, com transação e lock.
	 */
	@Test
	void readSideMustNotUseWriteRepositories() {
		List<String> writeRepositories = List.of(
				"com.frontier.bank.user.UserRepository",
				"com.frontier.bank.balance.BalanceRepository",
				"com.frontier.bank.ledger.LedgerRepository",
				"com.frontier.bank.transfer.TransferRepository");

		for (String repository : writeRepositories) {
			noClasses()
					.that().resideInAnyPackage("com.frontier.bank..query..", "com.frontier.bank..projection..")
					.should().dependOnClassesThat().haveFullyQualifiedName(repository)
					.check(classes);
		}
	}

	/**
	 * Contratos de evento são infraestrutura compartilhada: não podem conhecer
	 * domínio, senão a outbox passa a depender de quem publica nela (e o core
	 * deixa de poder evoluir sozinho).
	 */
	@Test
	void eventContractsMustNotDependOnDomain() {
		noClasses()
				.that().resideInAPackage("com.frontier.bank.common.event..")
				.should().dependOnClassesThat()
				.resideInAnyPackage("com.frontier.bank.user..", "com.frontier.bank.balance..",
						"com.frontier.bank.transfer..", "com.frontier.bank.ledger..",
						"com.frontier.bank.saga..")
				.check(classes);
	}

}
