-- Série temporal append-only: nunca dar UPDATE numa cotação já registrada.
-- A "taxa vigente" é sempre a linha mais recente (valid_from DESC) do par de moedas.
CREATE TABLE exchange_rates (
    id                  BIGSERIAL PRIMARY KEY,
    base_currency_id    BIGINT NOT NULL REFERENCES currencies(id),
    quote_currency_id   BIGINT NOT NULL REFERENCES currencies(id),
    rate                NUMERIC(19,6) NOT NULL CHECK (rate > 0),
    source              VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    valid_from          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_exchange_rates_pair_valid_from
    ON exchange_rates (base_currency_id, quote_currency_id, valid_from DESC);

-- Reforça a imutabilidade no nível do banco: mesmo um bug de aplicação não
-- consegue "corrigir" uma taxa histórica; é preciso inserir uma nova linha.
CREATE OR REPLACE FUNCTION prevent_exchange_rate_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'exchange_rates is append-only: UPDATE/DELETE not allowed (row id=%)', OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_exchange_rates_no_update
    BEFORE UPDATE OR DELETE ON exchange_rates
    FOR EACH ROW EXECUTE FUNCTION prevent_exchange_rate_mutation();
