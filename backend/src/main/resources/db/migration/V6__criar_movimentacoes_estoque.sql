-- Histórico imutável de entradas e saídas de estoque.
CREATE TABLE stock_movements (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id         BIGINT      NOT NULL,
    tipo            VARCHAR(10) NOT NULL,
    motivo          VARCHAR(30) NOT NULL,
    quantidade      INTEGER     NOT NULL,
    donation_id     BIGINT,
    registrado_por  BIGINT      NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_stock_movements_item          FOREIGN KEY (item_id)        REFERENCES items (id),
    CONSTRAINT fk_stock_movements_donation      FOREIGN KEY (donation_id)    REFERENCES donations (id),
    CONSTRAINT fk_stock_movements_registrado_por FOREIGN KEY (registrado_por) REFERENCES users (id),

    CONSTRAINT ck_stock_movements_tipo   CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_stock_movements_motivo CHECK (
        motivo IN ('DOACAO', 'CANCELAMENTO_DOACAO', 'DISTRIBUICAO', 'AJUSTE')
    ),
    CONSTRAINT ck_stock_movements_quantidade CHECK (quantidade > 0),

    -- DOACAO entra; CANCELAMENTO_DOACAO e DISTRIBUICAO saem; AJUSTE vale para os dois
    CONSTRAINT ck_stock_movements_tipo_motivo CHECK (
        (motivo = 'DOACAO' AND tipo = 'ENTRADA') OR
        (motivo IN ('CANCELAMENTO_DOACAO', 'DISTRIBUICAO') AND tipo = 'SAIDA') OR
        (motivo = 'AJUSTE')
    ),

    -- movimentações ligadas a doação precisam apontar para ela
    CONSTRAINT ck_stock_movements_doacao CHECK (
        motivo NOT IN ('DOACAO', 'CANCELAMENTO_DOACAO') OR donation_id IS NOT NULL
    )
);

CREATE INDEX idx_stock_movements_item_data ON stock_movements (item_id, criado_em);
CREATE INDEX idx_stock_movements_donation_id ON stock_movements (donation_id);
