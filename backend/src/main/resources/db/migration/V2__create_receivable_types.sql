-- Spread por tipo de recebível vive em banco (não hardcoded no código), permitindo
-- ajuste operacional sem deploy. As classes Strategy leem esse valor da entidade;
-- os percentuais do enunciado (1,5% / 2,5%) entram como dado de seed em V7, não como constante Java.
CREATE TABLE receivable_types (
    id                      BIGSERIAL PRIMARY KEY,
    code                    VARCHAR(50) NOT NULL UNIQUE,
    name                    VARCHAR(100) NOT NULL,
    spread_percent_monthly  NUMERIC(7,4) NOT NULL CHECK (spread_percent_monthly >= 0),
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
