-- ============================================================================
-- Fase 4 — Schema registry de eventos
--
-- Guarda o schema vigente de cada tipo de evento (derivado do próprio payload)
-- para detectar evolução incompatível: remover campo ou mudar tipo exige
-- incrementar a versão do evento. Mudanças aditivas são permitidas.
-- ============================================================================

CREATE TABLE event_schemas (
    event_type    VARCHAR(120)             NOT NULL,
    version       INTEGER                  NOT NULL,
    fingerprint   VARCHAR(64)              NOT NULL,
    fields        VARCHAR(4000)            NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_event_schemas PRIMARY KEY (event_type)
);
