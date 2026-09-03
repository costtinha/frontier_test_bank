package com.frontier.bank.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de ESCRITA do agregado User — usado exclusivamente pelos
 * handlers de comando. Consultas de apresentação vivem em
 * {@code UserQueryRepository} (lado de leitura do CQRS).
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	Optional<User> findByCpf(String cpf);

	boolean existsByEmail(String email);

	boolean existsByCpf(String cpf);

}
