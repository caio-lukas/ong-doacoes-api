-- Usuários do sistema (admin ou voluntário).
-- A tabela se chama "users" porque "user" é palavra reservada no PostgreSQL.
CREATE TABLE users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL,
    senha      VARCHAR(100) NOT NULL,   -- hash BCrypt
    cargo      VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_cargo CHECK (cargo IN ('ADMIN', 'VOLUNTARIO'))
);
