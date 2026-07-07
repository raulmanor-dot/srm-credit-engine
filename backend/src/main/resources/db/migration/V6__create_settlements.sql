-- UNIQUE em receivable_id impede fisicamente uma segunda liquidação do mesmo
-- título (defesa em profundidade além do optimistic lock em receivables.version).
CREATE TABLE settlements (
    id                              BIGSERIAL PRIMARY KEY,
    receivable_id                   BIGINT NOT NULL UNIQUE REFERENCES receivables(id),
    payment_currency_id             BIGINT NOT NULL REFERENCES currencies(id),
    face_value                      NUMERIC(19,6) NOT NULL,
    face_value_currency_id          BIGINT NOT NULL REFERENCES currencies(id),
    base_rate_used                  NUMERIC(9,6) NOT NULL,
    spread_used                     NUMERIC(9,6) NOT NULL,
    term_days                       INTEGER NOT NULL,
    term_months                     NUMERIC(12,6) NOT NULL,
    present_value_face_currency     NUMERIC(19,6) NOT NULL,
    fx_rate_used                    NUMERIC(19,6),
    net_value_payment_currency      NUMERIC(19,6) NOT NULL,
    version                         BIGINT NOT NULL DEFAULT 0,
    settled_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_settlements_settled_at ON settlements (settled_at);
CREATE INDEX idx_settlements_payment_currency ON settlements (payment_currency_id);
