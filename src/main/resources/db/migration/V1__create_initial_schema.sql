CREATE TABLE currency(
    code VARCHAR(3) PRIMARY KEY CHECK (code ~ '^[A-Z]{3}$') NOT NULL,
    minor_unit_exponent BIGINT CHECK (minor_unit_exponent >= 0) NOT NULL,
    active BOOLEAN NOT NULL
);
CREATE TABLE gl_account(
    code VARCHAR(20) PRIMARY KEY CHECK (code ~ '^[A-Z0-9]{1,20}$') NOT NULL,
    name VARCHAR(100) NOT NULL,
    class VARCHAR(50) NOT NULL CHECK (
        class IN (
            'ASSET',
            'LIABILITY',
            'EQUITY',
            'INCOME',
            'EXPENSE'
        )
    ),
    normal_side VARCHAR(10) NOT NULL CHECK (normal_side IN ('DEBIT', 'CREDIT'))
);
CREATE TABLE customer(
    id UUID PRIMARY KEY NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (
        status IN (
            'UNVERIFIED',
            'PENDING',
            'VERIFIED',
            'REJECTED',
            'SUSPENDED',
            'EXITED'
        )
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account(
    id UUID PRIMARY KEY NOT NULL,
    customer_id UUID REFERENCES customer(id),
    currency_code VARCHAR(3) NOT NULL REFERENCES currency(code) ON DELETE RESTRICT,
    gl_account_code VARCHAR(20) NOT NULL REFERENCES gl_account(code) ON DELETE RESTRICT,
    type VARCHAR(20) NOT NULL CHECK (
        type IN (
            'CUSTOMER_DEPOSIT',
            'INTERNAL',
            'LOAN'
        )
    ),
    status VARCHAR(20) NOT NULL CHECK (
        status IN (
            'OPEN',
            'DEBIT_FROZEN',
            'FROZEN',
            'CLOSED'
        )
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- internal accounts have no customer; every other type must have one
    CHECK ((type = 'INTERNAL') = (customer_id IS NULL)),
    -- target for posting's composite foreign key below
    UNIQUE (id, currency_code)
);
CREATE INDEX idx_account_customer ON account(customer_id);

CREATE TABLE account_balance(
    account_id UUID PRIMARY KEY REFERENCES account(id),
    balance_minor BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE journal_entry(
    id UUID PRIMARY KEY NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('POSTED')),
    -- nullable until idempotency arrives in Phase 2; Postgres allows many NULLs in a UNIQUE column
    idempotency_key VARCHAR(255) UNIQUE,
    business_date DATE NOT NULL,
    value_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    -- UNIQUE: an entry can be reversed at most once (Section 6.1)
    reverses_entry_id UUID UNIQUE REFERENCES journal_entry(id),
    correlation_id VARCHAR(64),
    CHECK (reverses_entry_id <> id)
);

CREATE TABLE posting(
    id UUID PRIMARY KEY NOT NULL,
    journal_entry_id UUID NOT NULL REFERENCES journal_entry(id),
    account_id UUID NOT NULL,
    currency_code VARCHAR(3) NOT NULL REFERENCES currency(code),
    -- signed: debit positive, credit negative; a zero posting moves nothing
    amount_minor BIGINT NOT NULL CHECK (amount_minor <> 0),
    value_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- a posting's currency must be its account's currency
    FOREIGN KEY (account_id, currency_code) REFERENCES account(id, currency_code)
);
-- Postgres does not index foreign keys automatically
CREATE INDEX idx_posting_account ON posting(account_id);
CREATE INDEX idx_posting_journal_entry ON posting(journal_entry_id);

CREATE TABLE audit_log(
    -- ordered identity, not UUID: the Phase 4 hash chain needs a strict order
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    object_type VARCHAR(50) NOT NULL,
    object_id VARCHAR(100) NOT NULL,
    before JSONB,
    after JSONB,
    correlation_id VARCHAR(64),
    -- filled in Phase 4 (M4.3)
    prev_hash BYTEA,
    hash BYTEA
);

CREATE TABLE outbox_event(
    id UUID PRIMARY KEY NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_seq BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    UNIQUE (aggregate_type, aggregate_id, aggregate_seq)
);
-- partial index: the relay only ever looks for unpublished rows
CREATE INDEX idx_outbox_unpublished ON outbox_event(created_at) WHERE published_at IS NULL;
