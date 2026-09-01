CREATE TABLE users (
    id         UUID                        NOT NULL,
    name       VARCHAR(150)                NOT NULL,
    email      VARCHAR(255)                NOT NULL,
    cpf        VARCHAR(11)                 NOT NULL,
    password   VARCHAR(255)                NOT NULL,
    role       VARCHAR(30)                 NOT NULL DEFAULT 'USER',
    active     BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_cpf UNIQUE (cpf)
);
