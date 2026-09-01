package com.frontier.bank.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	Optional<User> findByCpf(String cpf);

	boolean existsByEmail(String email);

	boolean existsByCpf(String cpf);

	// --- Consultas de paginação por cursor (ordem estável: created_at, id) ---

	@Query("""
			SELECT u FROM User u
			ORDER BY u.createdAt ASC, u.id ASC
			""")
	List<User> findFirstPage(Pageable pageable);

	@Query("""
			SELECT u FROM User u
			WHERE u.createdAt > :createdAt
			   OR (u.createdAt = :createdAt AND u.id > :id)
			ORDER BY u.createdAt ASC, u.id ASC
			""")
	List<User> findAfter(@Param("createdAt") Instant createdAt, @Param("id") UUID id, Pageable pageable);

	@Query("""
			SELECT u FROM User u
			WHERE u.createdAt < :createdAt
			   OR (u.createdAt = :createdAt AND u.id < :id)
			ORDER BY u.createdAt DESC, u.id DESC
			""")
	List<User> findBefore(@Param("createdAt") Instant createdAt, @Param("id") UUID id, Pageable pageable);

	@Query("""
			SELECT u FROM User u
			ORDER BY u.createdAt DESC, u.id DESC
			""")
	List<User> findLastPage(Pageable pageable);

}
