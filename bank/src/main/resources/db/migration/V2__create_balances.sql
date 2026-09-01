-- Saldo financeiro do cliente: relação 1:1 com users (user_id único).
CREATE TABLE balances (
    id         UUID                        NOT NULL,
    user_id    UUID                        NOT NULL,
    amount     NUMERIC(19, 2)              NOT NULL DEFAULT 0.00,
    version    BIGINT                      NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT pk_balances PRIMARY KEY (id),
    CONSTRAINT uk_balances_user UNIQUE (user_id),
    CONSTRAINT fk_balances_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
