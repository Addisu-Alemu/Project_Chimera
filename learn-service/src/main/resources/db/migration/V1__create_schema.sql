-- Project Chimera — LEARN service schema

CREATE TABLE IF NOT EXISTS engagement_signals (
    id             UUID        PRIMARY KEY,
    agent_id       UUID        NOT NULL,
    post_result_id UUID        NOT NULL,
    signal_type    VARCHAR(20) NOT NULL,
    value          BIGINT      NOT NULL DEFAULT 0,
    recorded_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_engagement_signals_agent_id       ON engagement_signals(agent_id);
CREATE INDEX IF NOT EXISTS idx_engagement_signals_post_result_id ON engagement_signals(post_result_id);

CREATE TABLE IF NOT EXISTS feedback_reports (
    id                UUID           PRIMARY KEY,
    agent_id          UUID           NOT NULL,
    content_bundle_id UUID           NOT NULL,
    confidence_score  DECIMAL(4, 3)  NOT NULL,
    likes             BIGINT         NOT NULL DEFAULT 0,
    shares            BIGINT         NOT NULL DEFAULT 0,
    comments          BIGINT         NOT NULL DEFAULT 0,
    views             BIGINT         NOT NULL DEFAULT 0,
    click_through_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.0,
    review_status     VARCHAR(30)    NOT NULL,
    generated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    dispatched_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_feedback_reports_agent_id          ON feedback_reports(agent_id);
CREATE INDEX IF NOT EXISTS idx_feedback_reports_content_bundle_id ON feedback_reports(content_bundle_id);

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
