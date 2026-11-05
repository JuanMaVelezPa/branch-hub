-- Baseline schema for Branch Hub: branches, the transactional outbox and IAM.
-- Frozen once applied; every later change goes in a new V{n}__ script.

CREATE TABLE branch (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(120) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_event_status_created_at
    ON outbox_event (status, created_at);

CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);

CREATE TABLE iam_role (
    id UUID PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE iam_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) UNIQUE,
    email_verified_at TIMESTAMPTZ,
    password_hash VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE iam_user_role (
    user_id UUID NOT NULL REFERENCES iam_user (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES iam_role (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE iam_audit_log (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_user_id UUID,
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX iam_user_enabled_idx ON iam_user (enabled);
CREATE INDEX iam_user_locked_until_idx ON iam_user (locked_until);

CREATE INDEX iam_audit_log_actor_idx ON iam_audit_log (actor_user_id);
CREATE INDEX iam_audit_log_target_idx ON iam_audit_log (target_user_id);
CREATE INDEX iam_audit_log_created_at_idx ON iam_audit_log (created_at);
