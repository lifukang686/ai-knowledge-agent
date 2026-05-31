CREATE TABLE IF NOT EXISTS agent_run (
    id BIGINT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    input_query TEXT,
    output_answer TEXT,
    status VARCHAR(20),
    log TEXT,
    error_message TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_run_agent_id ON agent_run (agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_run_status ON agent_run (status);
