-- Outbox transacional: o evento é gravado na MESMA transação da mudança de estado,
-- eliminando o dual-write (evento fantasma / evento perdido) e habilitando
-- entrega at-least-once com recuperação após falhas.
CREATE TABLE outbox_events (
    id             UUID                     NOT NULL,
    event_id       UUID                     NOT NULL,
    event_type     VARCHAR(120)             NOT NULL,
    event_version  INTEGER                  NOT NULL,
    aggregate_type VARCHAR(60)              NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    correlation_id VARCHAR(64),
    causation_id   VARCHAR(64),
    actor_id       VARCHAR(64),
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    payload        VARCHAR(65535)           NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    attempts       INTEGER                  NOT NULL DEFAULT 0,
    last_error     VARCHAR(500),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT uk_outbox_events_event_id UNIQUE (event_id)
);

-- Índice parcial: cobre exatamente o que o relay consome (pendentes, em ordem).
CREATE INDEX idx_outbox_events_pending ON outbox_events (occurred_at) WHERE published_at IS NULL;
