package com.frontier.bank.user.command;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.balance.Balance;
import com.frontier.bank.balance.BalanceRepository;
import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: criação de usuário. Executa em uma única transação e
 * preserva o invariante de que todo cliente nasce com saldo R$ 0,00.
 */
@Component
@Transactional
public class CreateUserHandler implements CommandHandler<CreateUserCommand, UUID> {

	private final UserRepository repository;
	private final BalanceRepository balanceRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserUniquenessGuard uniqueness;

	public CreateUserHandler(UserRepository repository, BalanceRepository balanceRepository,
			PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.balanceRepository = balanceRepository;
		this.passwordEncoder = passwordEncoder;
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
		balanceRepository.save(new Balance(user));
		return user.getId();
	}

}
