# Diagrama ER — SRM Credit Engine

Derivado das migrations Flyway (`backend/src/main/resources/db/migration/V1`–`V6`), que
são a fonte de verdade do schema. Este diagrama é uma leitura de apoio; o DDL executável
vive nas próprias migrations.

```mermaid
erDiagram
    CURRENCIES ||--o{ RECEIVABLES : "face_value_currency"
    CURRENCIES ||--o{ SETTLEMENTS : "payment_currency / face_value_currency"
    CURRENCIES ||--o{ EXCHANGE_RATES : "base / quote"
    ASSIGNORS ||--o{ RECEIVABLES : "cede"
    RECEIVABLE_TYPES ||--o{ RECEIVABLES : "define spread"
    RECEIVABLES ||--o| SETTLEMENTS : "liquidado em (0..1)"

    CURRENCIES {
        bigint id PK
        varchar code UK "ISO 4217 (BRL, USD)"
        varchar name
        boolean active
        timestamptz created_at
    }

    RECEIVABLE_TYPES {
        bigint id PK
        varchar code UK "DUPLICATA_MERCANTIL, CHEQUE_PRE_DATADO"
        varchar name
        numeric spread_percent_monthly "regra de risco, editável sem deploy"
        boolean active
    }

    ASSIGNORS {
        bigint id PK
        varchar name
        varchar tax_id UK
        boolean active
    }

    EXCHANGE_RATES {
        bigint id PK
        bigint base_currency_id FK
        bigint quote_currency_id FK
        numeric rate
        varchar source "MANUAL | PROVIDER"
        timestamptz valid_from "append-only, sem UPDATE/DELETE"
    }

    RECEIVABLES {
        bigint id PK
        bigint assignor_id FK
        bigint receivable_type_id FK
        bigint face_value_currency_id FK
        numeric face_value
        varchar document_number
        date issue_date
        date due_date
        varchar status "PENDING | SETTLED | CANCELED"
        bigint version "optimistic lock"
    }

    SETTLEMENTS {
        bigint id PK
        bigint receivable_id FK "UNIQUE — 1 liquidação por título"
        bigint payment_currency_id FK
        bigint face_value_currency_id FK
        numeric base_rate_used
        numeric spread_used
        numeric fx_rate_used "NULL quando não é cross-currency"
        numeric present_value_face_currency
        numeric net_value_payment_currency
        bigint version "optimistic lock"
        timestamptz settled_at
    }
```

## Notas de modelagem

- **`exchange_rates` é append-only** (trigger de banco impede `UPDATE`/`DELETE` — ver
  [ADR 0003](../adr/0003-exchange-rates-append-only.md)): a "taxa vigente" é sempre a
  linha mais recente por par de moeda, preservando o histórico para auditoria.
- **Dupla proteção contra liquidação concorrente/duplicada** (ver
  [ADR 0004](../adr/0004-concurrency-control-settlement.md)): `receivables.version`
  (optimistic lock, camada de aplicação) + `settlements.receivable_id UNIQUE`
  (constraint física, camada de banco).
- **`fx_rate_used` é nulável**: liquidações same-currency (ex. recebível em BRL pago em
  BRL) não passam por conversão cambial.
- **Todos os valores monetários são `NUMERIC(19,6)`**, nunca `FLOAT`/`DOUBLE` — ver
  [ADR 0002](../adr/0002-bigdecimal-precision.md).
