package com.frontier.bank.user.command;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.frontier.bank.common.command.CommandHandler;
import com.frontier.bank.common.error.ResourceNotFoundException;
import com.frontier.bank.user.User;
import com.frontier.bank.user.UserRepository;

/**
 * Handler de escrita: exclusão física do usuário. O saldo vinculado é
 * removido em cascata pelo banco (FK ON DELETE CASCADE).
 */
@Component
@Transactional
public class DeleteUserHandler implements CommandHandler<DeleteUserCommand, Void> {

	private final UserRepository repository;

	public DeleteUserHandler(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	public Void handle(DeleteUserCommand command) {
		User user = repository.findById(command.id())
				.orElseThrow(() -> new ResourceNotFoundException("usuário", command.id()));
		repository.delete(user);
		return null;
	}

}
