-- ============================================================================
-- Fase 4 — Auditoria WORM (write-once, read-many)
--
-- Cada fato relevante gera um registro imutável, encadeado por hash: o hash de
-- um registro depende do hash do anterior. Isso torna a adulteração detectável
-- (remover ou alterar qualquer registro quebra a cadeia a partir dele).
-- ============================================================================

CREATE TABLE audit_records (
    id             BIGSERIAL                NOT NULL,
    event_id       UUID                     NOT NULL,
    event_type     VARCHAR(120)             NOT NULL,
    event_version  INTEGER                  NOT NULL,
    aggregate_type VARCHAR(60)              NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    actor_id       VARCHAR(64),
    correlation_id VARCHAR(64),
    causation_id   VARCHAR(64),
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    payload        VARCHAR(65535)           NOT NULL,
    previous_hash  VARCHAR(64)              NOT NULL,
    record_hash    VARCHAR(64)              NOT NULL,
    CONSTRAINT pk_audit_records PRIMARY KEY (id),
    CONSTRAINT uk_audit_records_event_id UNIQUE (event_id)
);

CREATE INDEX idx_audit_records_occurred_at ON audit_records (occurred_at);

-- WORM aplicado no banco: nem UPDATE, nem DELETE, nem TRUNCATE.
CREATE OR REPLACE FUNCTION audit_records_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_records e write-once (WORM): % nao e permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_records_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_records
    FOR EACH ROW EXECUTE FUNCTION audit_records_immutable();

CREATE TRIGGER trg_audit_records_no_truncate
    BEFORE TRUNCATE ON audit_records
    FOR EACH STATEMENT EXECUTE FUNCTION audit_records_immutable();

-- Linha única usada para serializar a escrita da cadeia (lock pessimista):
-- sem isso, transações concorrentes poderiam ramificar o encadeamento.
CREATE TABLE audit_chain_guard (
    id         SMALLINT                 NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_audit_chain_guard PRIMARY KEY (id),
    CONSTRAINT ck_audit_chain_guard_single_row CHECK (id = 1)
);

INSERT INTO audit_chain_guard (id, updated_at) VALUES (1, NOW());
