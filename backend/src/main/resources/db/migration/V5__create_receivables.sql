CREATE TABLE receivables (
    id                      BIGSERIAL PRIMARY KEY,
    assignor_id             BIGINT NOT NULL REFERENCES assignors(id),
    receivable_type_id      BIGINT NOT NULL REFERENCES receivable_types(id),
    face_value_currency_id  BIGINT NOT NULL REFERENCES currencies(id),
    face_value              NUMERIC(19,6) NOT NULL CHECK (face_value > 0),
    document_number         VARCHAR(50),
    issue_date              DATE NOT NULL,
    due_date                DATE NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'SETTLED', 'CANCELED')),
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_receivables_due_after_issue CHECK (due_date > issue_date)
);

CREATE INDEX idx_receivables_assignor ON receivables (assignor_id);
CREATE INDEX idx_receivables_status ON receivables (status);
CREATE INDEX idx_receivables_due_date ON receivables (due_date);
