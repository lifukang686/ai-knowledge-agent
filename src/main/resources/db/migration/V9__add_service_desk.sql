CREATE TABLE IF NOT EXISTS service_ticket (
    id BIGINT PRIMARY KEY,
    ticket_no VARCHAR(40) NOT NULL UNIQUE,
    service_type VARCHAR(20) NOT NULL,
    category VARCHAR(64),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    agent_summary TEXT,
    creator_id BIGINT NOT NULL,
    assignee_id BIGINT,
    source_run_id BIGINT,
    source_conversation_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_service_ticket_creator ON service_ticket (creator_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_service_ticket_no ON service_ticket (ticket_no);
CREATE INDEX IF NOT EXISTS idx_service_ticket_status ON service_ticket (status);
CREATE INDEX IF NOT EXISTS idx_service_ticket_type ON service_ticket (service_type);

CREATE TABLE IF NOT EXISTS service_desk_run (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    service_type VARCHAR(20),
    intent VARCHAR(40),
    knowledge_base_id BIGINT,
    conversation_id BIGINT,
    answer TEXT,
    status VARCHAR(20) NOT NULL,
    ticket_id BIGINT,
    event_log TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_service_desk_run_user ON service_desk_run (user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_service_desk_run_ticket ON service_desk_run (ticket_id);
CREATE INDEX IF NOT EXISTS idx_service_desk_run_intent ON service_desk_run (intent);
