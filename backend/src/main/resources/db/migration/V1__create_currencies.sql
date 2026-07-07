CREATE TABLE currencies (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(3) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
