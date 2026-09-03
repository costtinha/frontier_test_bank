package com.frontier.bank.user.command;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: atualização parcial do usuário.
 */
@Component
@Transactional
public class UpdateUserHandler implements CommandHandler<UpdateUserCommand, UUID> {

	private final UserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final UserUniquenessGuard uniqueness;

	public UpdateUserHandler(UserRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.uniqueness = new UserUniquenessGuard(repository);
	}

	@Override
	public UUID handle(UpdateUserCommand command) {
		User user = repository.findById(command.id())
				.orElseThrow(() -> new ResourceNotFoundException("usuário", command.id()));

		if (command.name() != null) {
			user.setName(command.name().trim());
		}
		if (command.email() != null) {
			String email = uniqueness.normalizeEmail(command.email());
			uniqueness.ensureEmailAvailable(email, command.id());
			user.setEmail(email);
		}
		if (command.password() != null) {
			user.setPassword(passwordEncoder.encode(command.password()));
		}
		if (command.role() != null) {
			user.setRole(command.role());
		}
		if (command.active() != null) {
			user.setActive(command.active());
		}
		// saveAndFlush garante que @UpdateTimestamp reflita updated_at
		repository.saveAndFlush(user);
		return user.getId();
	}

}
