package com.frontier.bank.user.query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.frontier.bank.user.User;

/**
 * Repositório de LEITURA do usuário: expõe apenas consultas (projeções),
 * sem nenhuma operação de escrita. É a única porta usada pelo lado de query —
 * pode ser apontado futuramente para uma réplica de leitura ou para um modelo
 * materializado alimentado por domain events, sem tocar nos writers.
 */
public interface UserQueryRepository extends Repository<User, UUID> {

	@Query("""
			SELECT new com.frontier.bank.user.query.UserDetails(
				u.id, u.name, u.email, u.cpf, u.role, u.active, u.createdAt, u.updatedAt)
			FROM User u
			WHERE u.id = :id
			""")
	Optional<UserDetails> findDetailsById(@Param("id") UUID id);

	/** Usado pelo extrato para distinguir cliente inexistente de extrato vazio. */
	boolean existsById(UUID id);

	@Query("""
			SELECT new com.frontier.bank.user.query.UserSummary(
				u.id, u.name, u.email, u.role, u.active, u.createdAt)
			FROM User u
			ORDER BY u.createdAt ASC, u.id ASC
			""")
	List<UserSummary> findFirstPage(Pageable pageable);

	@Query("""
			SELECT new com.frontier.bank.user.query.UserSummary(
				u.id, u.name, u.email, u.role, u.active, u.createdAt)
			FROM User u
			WHERE u.createdAt > :createdAt
			   OR (u.createdAt = :createdAt AND u.id > :id)
			ORDER BY u.createdAt ASC, u.id ASC
			""")
	List<UserSummary> findAfter(@Param("createdAt") Instant createdAt, @Param("id") UUID id, Pageable pageable);

	@Query("""
			SELECT new com.frontier.bank.user.query.UserSummary(
				u.id, u.name, u.email, u.role, u.active, u.createdAt)
			FROM User u
			WHERE u.createdAt < :createdAt
			   OR (u.createdAt = :createdAt AND u.id < :id)
			ORDER BY u.createdAt DESC, u.id DESC
			""")
	List<UserSummary> findBefore(@Param("createdAt") Instant createdAt, @Param("id") UUID id, Pageable pageable);

	@Query("""
			SELECT new com.frontier.bank.user.query.UserSummary(
				u.id, u.name, u.email, u.role, u.active, u.createdAt)
			FROM User u
			ORDER BY u.createdAt DESC, u.id DESC
			""")
	List<UserSummary> findLastPage(Pageable pageable);

}
