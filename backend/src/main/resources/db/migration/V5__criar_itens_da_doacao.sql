-- Itens de cada doação (relação N:N com quantidade). Um item aparece uma vez por doação.
CREATE TABLE donation_items (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    donation_id  BIGINT  NOT NULL,
    item_id      BIGINT  NOT NULL,
    quantidade   INTEGER NOT NULL,

    CONSTRAINT fk_donation_items_donation FOREIGN KEY (donation_id) REFERENCES donations (id),
    CONSTRAINT fk_donation_items_item     FOREIGN KEY (item_id)     REFERENCES items (id),
    CONSTRAINT uq_donation_items_donation_item UNIQUE (donation_id, item_id),
    CONSTRAINT ck_donation_items_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_donation_items_item_id ON donation_items (item_id);
