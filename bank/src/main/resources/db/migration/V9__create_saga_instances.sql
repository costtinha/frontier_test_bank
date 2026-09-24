-- ============================================================================
-- Saga distribuída por orquestração
--
-- O orquestrador mantém o estado da saga de forma durável: cada passo (comando
-- enviado a um participante) é registrado, o avanço sobrevive a reinícios e a
-- compensação é executada em ordem reversa quando um passo falha.
-- ============================================================================

CREATE TABLE saga_instances (
    id              UUID                     NOT NULL,
    saga_type       VARCHAR(60)              NOT NULL,
    state           VARCHAR(20)              NOT NULL,
    step_index      INTEGER                  NOT NULL,
    payload         VARCHAR(65535)           NOT NULL,
    idempotency_key VARCHAR(80),
    attempts        INTEGER                  NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    last_error      VARCHAR(1000),
    correlation_id  VARCHAR(64),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT pk_saga_instances PRIMARY KEY (id),
    -- uma chave de idempotência só pode originar uma saga (evita transferir duas vezes)
    CONSTRAINT uk_saga_instances_idempotency UNIQUE (idempotency_key)
);

-- Índice parcial: cobre exatamente o que o sweeper varre (sagas pendentes de avanço).
CREATE INDEX idx_saga_instances_pending ON saga_instances (next_attempt_at, created_at)
    WHERE state IN ('RUNNING', 'COMPENSATING');

-- Idempotência por passo: a chave única garante, no banco, que um passo executa
-- uma única vez por saga — mesmo com retry, corrida entre requisição e sweeper
-- ou reinício da aplicação no meio do avanço.
CREATE TABLE saga_step_executions (
    id          UUID                     NOT NULL,
    saga_id     UUID                     NOT NULL,
    step_name   VARCHAR(80)              NOT NULL,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_saga_step_executions PRIMARY KEY (id),
    CONSTRAINT uk_saga_step_executions UNIQUE (saga_id, step_name),
    CONSTRAINT fk_saga_step_executions_saga FOREIGN KEY (saga_id) REFERENCES saga_instances (id) ON DELETE CASCADE
);
