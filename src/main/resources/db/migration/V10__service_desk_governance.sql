ALTER TABLE service_desk_run
    ADD COLUMN IF NOT EXISTS approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pending_ticket_id BIGINT,
    ADD COLUMN IF NOT EXISTS feedback_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_service_desk_run_pending_ticket ON service_desk_run (pending_ticket_id);
CREATE INDEX IF NOT EXISTS idx_service_desk_run_feedback ON service_desk_run (feedback_id);

CREATE TABLE IF NOT EXISTS service_ticket_event (
    id BIGINT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    operator_id BIGINT,
    message VARCHAR(500),
    payload TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_service_ticket_event_ticket ON service_ticket_event (ticket_id, create_time ASC);
CREATE INDEX IF NOT EXISTS idx_service_ticket_event_type ON service_ticket_event (event_type);

CREATE TABLE IF NOT EXISTS service_desk_feedback (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    ticket_id BIGINT,
    resolved BOOLEAN NOT NULL,
    comment TEXT,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_service_desk_feedback_run_user ON service_desk_feedback (run_id, user_id);
CREATE INDEX IF NOT EXISTS idx_service_desk_feedback_ticket ON service_desk_feedback (ticket_id);
