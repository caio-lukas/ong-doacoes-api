-- Doadores
CREATE TABLE donors (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL,
    telefone   VARCHAR(15)  NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL
);
