package com.frontier.bank.user.command;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.balance.event.BalanceOpened;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.event.DomainEvent;
import com.frontier.bank.common.event.EventPublisher;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;
import com.frontier.bank.user.event.UserRegistered;

/**
 * Handler de escrita: criação de usuário. Executa em uma única transação e
 * preserva o invariante de que todo cliente nasce com saldo R$ 0,00.
 * <p>
 * Publica dois eventos na outbox, na <b>mesma transação</b> da criação (sem
 * evento fantasma nem evento perdido): {@link UserRegistered} e
 * {@link BalanceOpened} — este último dá origem à linha do read model de saldo,
 * de modo que um cliente sem movimentação também aparece no lado de leitura.
 */
@Component
@Transactional
public class CreateUserHandler implements CommandHandler<CreateUserCommand, UUID> {

	private final UserRepository repository;
	private final BalanceRepository balanceRepository;
	private final PasswordEncoder passwordEncoder;
	private final EventPublisher eventPublisher;
	private final UserUniquenessGuard uniqueness;

	public CreateUserHandler(UserRepository repository, BalanceRepository balanceRepository,
			PasswordEncoder passwordEncoder, EventPublisher eventPublisher) {
		this.repository = repository;
		this.balanceRepository = balanceRepository;
		this.passwordEncoder = passwordEncoder;
		this.eventPublisher = eventPublisher;
		this.uniqueness = new UserUniquenessGuard(repository);
	}

	@Override
	public UUID handle(CreateUserCommand command) {
		uniqueness.ensureEmailAvailable(command.email(), null);
		uniqueness.ensureCpfAvailable(command.cpf(), null);

		User user = new User(
				command.name().trim(),
				uniqueness.normalizeEmail(command.email()),
				command.cpf(),
				passwordEncoder.encode(command.password()));
		if (command.role() != null) {
			user.setRole(command.role());
		}
		user = repository.save(user);
		Balance balance = balanceRepository.save(new Balance(user));

		eventPublisher.publish(DomainEvent.of(new UserRegistered(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole())));

		eventPublisher.publish(DomainEvent.of(new BalanceOpened(
				balance.getId(),
				user.getId(),
				balance.getAmount())));

		return user.getId();
	}

}
