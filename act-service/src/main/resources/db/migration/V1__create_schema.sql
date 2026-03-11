-- Project Chimera — ACT service schema
-- Append-only: no UPDATE or DELETE statements permitted on transactions table

CREATE TABLE IF NOT EXISTS post_results (
    id                UUID        PRIMARY KEY,
    agent_id          UUID        NOT NULL,
    content_bundle_id UUID        NOT NULL,
    platform          VARCHAR(20) NOT NULL,
    published_at      TIMESTAMPTZ,
    status            VARCHAR(30) NOT NULL,
    attempt_count     INTEGER     NOT NULL DEFAULT 1,
    failure_reason    TEXT,
    platform_post_id  VARCHAR(200),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_post_results_agent_id          ON post_results(agent_id);
CREATE INDEX IF NOT EXISTS idx_post_results_content_bundle_id ON post_results(content_bundle_id);

-- Append-only: each row represents one immutable state snapshot.
-- Status transitions insert a new row; never UPDATE an existing one.
CREATE TABLE IF NOT EXISTS transactions (
    id                UUID           PRIMARY KEY,
    agent_id          UUID           NOT NULL,
    type              VARCHAR(50)    NOT NULL,
    amount            DECIMAL(12, 2) NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    platform          VARCHAR(20),
    content_bundle_id UUID,
    status            VARCHAR(30)    NOT NULL,
    actor             VARCHAR(100)   NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    approver_id       VARCHAR(100),
    completed_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_transactions_agent_id          ON transactions(agent_id);
CREATE INDEX IF NOT EXISTS idx_transactions_content_bundle_id ON transactions(content_bundle_id);

CREATE TABLE IF NOT EXISTS human_alerts (
    id                      UUID         PRIMARY KEY,
    agent_id                UUID         NOT NULL,
    type                    VARCHAR(50)  NOT NULL,
    triggering_record_id    UUID         NOT NULL,
    triggering_record_link  TEXT         NOT NULL,
    threshold_value         VARCHAR(100) NOT NULL,
    actual_value            VARCHAR(100) NOT NULL,
    issued_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at             TIMESTAMPTZ,
    resolving_operator_id   VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_human_alerts_agent_id ON human_alerts(agent_id);
