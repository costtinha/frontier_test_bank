package com.frontier.bank.ledger;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do razão: usado apenas para <b>inserir</b> entradas (nunca
 * atualizar/remover) pelos handlers de comando. A leitura de extrato vive em
 * {@code com.frontier.bank.ledger.query.LedgerQueryRepository}.
 */
public interface LedgerRepository extends JpaRepository<LedgerEntry, UUID> {

}
