-- ============================================================================
-- Fase 2 — Read models (CQRS)
-- O lado de leitura passa a usar modelos próprios, mantidos por um projector
-- idempotente que consome o log de eventos (outbox_events).
-- ============================================================================

-- Snapshot de saldo: read model otimizado para consulta (sem joins, sem entidade
-- de escrita). O valor é sempre o saldo resultante do último evento aplicado.
CREATE TABLE balance_snapshot (
    balance_id    UUID                     NOT NULL,
    user_id       UUID                     NOT NULL,
    amount        NUMERIC(19, 2)           NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    last_event_id UUID                     NOT NULL,
    CONSTRAINT pk_balance_snapshot PRIMARY KEY (balance_id),
    CONSTRAINT uk_balance_snapshot_user UNIQUE (user_id)
);

-- Dedupe por consumidor: a entrega é at-least-once (replay do log, redelivery do
-- broker), então todo consumidor precisa ser idempotente.
CREATE TABLE processed_events (
    id           UUID                     NOT NULL,
    consumer     VARCHAR(60)              NOT NULL,
    event_id     UUID                     NOT NULL,
    event_type   VARCHAR(120)             NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (id),
    CONSTRAINT uk_processed_events_consumer_event UNIQUE (consumer, event_id)
);

-- Posição do consumidor no log de eventos (análogo ao offset de um consumer Kafka).
CREATE TABLE projection_checkpoints (
    consumer         VARCHAR(60)              NOT NULL,
    last_occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_event_id    UUID                     NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_projection_checkpoints PRIMARY KEY (consumer)
);

-- Dead letter: eventos que falharam ao projetar. Após N tentativas o evento é
-- pulado (checkpoint avança) para não bloquear o stream — mas nunca é perdido
-- silenciosamente: fica registrado aqui para intervenção/reprocessamento.
CREATE TABLE projection_dead_letters (
    id              UUID                     NOT NULL,
    consumer        VARCHAR(60)              NOT NULL,
    event_id        UUID                     NOT NULL,
    event_type      VARCHAR(120)             NOT NULL,
    aggregate_id    UUID                     NOT NULL,
    payload         VARCHAR(65535)           NOT NULL,
    error           VARCHAR(1000),
    attempts        INTEGER                  NOT NULL DEFAULT 0,
    first_failed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_failed_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_projection_dead_letters PRIMARY KEY (id),
    CONSTRAINT uk_projection_dead_letters_consumer_event UNIQUE (consumer, event_id)
);

-- ---------------------------------------------------------------------------
-- Bootstrap do read model
-- O snapshot parte do estado ATUAL de escrita (sem reprojetar o histórico) e o
-- checkpoint começa no fim do log de eventos: a partir daqui a projeção é
-- incremental. Novos usuários geram BalanceOpened e nascem direto no read model.
-- ---------------------------------------------------------------------------
INSERT INTO balance_snapshot (balance_id, user_id, amount, created_at, updated_at, last_event_id)
SELECT b.id, b.user_id, b.amount, b.created_at, b.updated_at, '00000000-0000-0000-0000-000000000000'
FROM balances b;

INSERT INTO projection_checkpoints (consumer, last_occurred_at, last_event_id, updated_at)
SELECT 'balance-snapshot',
       COALESCE((SELECT occurred_at FROM outbox_events ORDER BY occurred_at DESC, id DESC LIMIT 1),
                TIMESTAMPTZ '1970-01-01 00:00:00+00'),
       COALESCE((SELECT id FROM outbox_events ORDER BY occurred_at DESC, id DESC LIMIT 1),
                '00000000-0000-0000-0000-000000000000'),
       NOW();
