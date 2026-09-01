package com.frontier.bank.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private UserRepository userRepository;

	@Test
	void shouldPersistAndFindUserByEmail() {
		User user = new User("João da Silva", "joao@example.com", "12345678901", "hash-da-senha");

		User saved = userRepository.save(user);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(saved.isActive()).isTrue();
		assertThat(saved.getRole()).isEqualTo(UserRole.USER);

		Optional<User> found = userRepository.findByEmail("joao@example.com");
		assertThat(found).isPresent();
		assertThat(found.get().getId()).isEqualTo(saved.getId());
		assertThat(found.get().getCpf()).isEqualTo("12345678901");
	}

	@Test
	void shouldRejectDuplicateEmail() {
		userRepository.save(new User("João", "duplicado@example.com", "12345678901", "hash"));

		User duplicate = new User("Maria", "duplicado@example.com", "10987654321", "hash");

		assertThat(userRepository.existsByEmail(duplicate.getEmail())).isTrue();
	}

}
