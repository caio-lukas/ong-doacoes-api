-- Doações. Cada doação tem um tipo só: DINHEIRO (valor > 0) ou ITENS (sem valor).
CREATE TABLE donations (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    donor_id             BIGINT         NOT NULL,
    tipo                 VARCHAR(20)    NOT NULL,
    valor                NUMERIC(12, 2),
    data_doacao          DATE           NOT NULL,
    status               VARCHAR(20)    NOT NULL,
    registrado_por       BIGINT         NOT NULL,
    cancelada_por        BIGINT,
    cancelada_em         TIMESTAMPTZ,
    motivo_cancelamento  VARCHAR(255),
    criado_em            TIMESTAMPTZ    NOT NULL,

    CONSTRAINT fk_donations_donor          FOREIGN KEY (donor_id)       REFERENCES donors (id),
    CONSTRAINT fk_donations_registrado_por FOREIGN KEY (registrado_por) REFERENCES users (id),
    CONSTRAINT fk_donations_cancelada_por  FOREIGN KEY (cancelada_por)  REFERENCES users (id),

    CONSTRAINT ck_donations_tipo   CHECK (tipo IN ('DINHEIRO', 'ITENS')),
    CONSTRAINT ck_donations_status CHECK (status IN ('REGISTRADA', 'CANCELADA')),

    -- DINHEIRO exige valor positivo; ITENS não tem valor
    CONSTRAINT ck_donations_tipo_valor CHECK (
        (tipo = 'DINHEIRO' AND valor > 0) OR (tipo = 'ITENS' AND valor IS NULL)
    ),

    -- cancelada <=> tem autor e data de cancelamento
    CONSTRAINT ck_donations_cancelamento CHECK (
        (status = 'REGISTRADA' AND cancelada_em IS NULL     AND cancelada_por IS NULL) OR
        (status = 'CANCELADA'  AND cancelada_em IS NOT NULL AND cancelada_por IS NOT NULL)
    )
);

CREATE INDEX idx_donations_donor_id   ON donations (donor_id);
-- Útil para o relatório mensal; vale medir com EXPLAIN ANALYZE quando houver dados.
CREATE INDEX idx_donations_data_doacao ON donations (data_doacao);
