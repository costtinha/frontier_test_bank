-- ============================================================================
-- Fase 3 — Ledger (livro-razão append-only) e transferências
--
-- O ledger é a fonte da verdade contábil: toda movimentação de saldo grava uma
-- entrada na MESMA transação da mudança de estado. Com isso o extrato passa a
-- ser confiável e é possível reconciliar (saldo da conta x soma do razão).
-- ============================================================================

CREATE TABLE ledger_entries (
    id             UUID                     NOT NULL,
    transaction_id UUID                     NOT NULL,
    balance_id     UUID                     NOT NULL,
    user_id        UUID                     NOT NULL,
    entry_type     VARCHAR(30)              NOT NULL,
    amount         NUMERIC(19, 2)           NOT NULL,
    balance_before NUMERIC(19, 2)           NOT NULL,
    balance_after  NUMERIC(19, 2)           NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT ck_ledger_entries_amount CHECK (amount > 0),
    CONSTRAINT fk_ledger_entries_balance FOREIGN KEY (balance_id) REFERENCES balances (id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_entries_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Extrato: leitura por cliente em ordem cronológica (ASC atende os dois sentidos).
CREATE INDEX idx_ledger_entries_user_occurred ON ledger_entries (user_id, occurred_at, id);
CREATE INDEX idx_ledger_entries_transaction ON ledger_entries (transaction_id);

-- Transferência entre contas. A chave de idempotência (cabeçalho Idempotency-Key)
-- impede que um retry do cliente movimente dinheiro duas vezes.
CREATE TABLE transfers (
    id              UUID                     NOT NULL,
    idempotency_key VARCHAR(80)              NOT NULL,
    source_user_id  UUID                     NOT NULL,
    target_user_id  UUID                     NOT NULL,
    amount          NUMERIC(19, 2)           NOT NULL,
    status          VARCHAR(20)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT uk_transfers_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_transfers_distinct_accounts CHECK (source_user_id <> target_user_id),
    CONSTRAINT fk_transfers_source_user FOREIGN KEY (source_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_transfers_target_user FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE CASCADE
);
