-- Itens de estoque. A coluna "versao" é usada pelo @Version (bloqueio otimista).
CREATE TABLE items (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                VARCHAR(120) NOT NULL,
    categoria           VARCHAR(60)  NOT NULL,
    unidade             VARCHAR(20)  NOT NULL,
    quantidade_estoque  INTEGER      NOT NULL DEFAULT 0,
    versao              BIGINT       NOT NULL DEFAULT 0,
    criado_em           TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_items_nome UNIQUE (nome),
    CONSTRAINT ck_items_estoque_nao_negativo CHECK (quantidade_estoque >= 0)
);
