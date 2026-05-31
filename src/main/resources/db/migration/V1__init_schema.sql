CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS document (
    id BIGINT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    uploader_id BIGINT,
    status VARCHAR(20),
    error_message VARCHAR(2000),
    chunk_count INTEGER,
    processing_duration_ms BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_order INTEGER NOT NULL,
    token_count INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS model_provider (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    api_base_url VARCHAR(255),
    api_key VARCHAR(255),
    description VARCHAR(500),
    is_default BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS model_config (
    id BIGINT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    model_type VARCHAR(32),
    default_params JSON,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS agent (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    tool_ids JSON,
    system_prompt TEXT,
    max_steps INTEGER,
    execution_strategy VARCHAR(20),
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

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

CREATE TABLE IF NOT EXISTS tool_definition (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    executor_type VARCHAR(20) NOT NULL,
    executor_config TEXT,
    parameters_schema TEXT,
    enabled BOOLEAN,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT PRIMARY KEY,
    model_id BIGINT NOT NULL,
    prompt TEXT,
    response TEXT,
    token_usage INTEGER NOT NULL,
    latency_ms INTEGER NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_document_knowledge_base_id ON document (knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_document_status ON document (status);
CREATE INDEX IF NOT EXISTS idx_document_chunk_document_id ON document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_model_config_provider_id ON model_config (provider_id);
CREATE INDEX IF NOT EXISTS idx_model_config_type ON model_config (model_type);
CREATE INDEX IF NOT EXISTS idx_agent_run_agent_id ON agent_run (agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_run_status ON agent_run (status);
CREATE INDEX IF NOT EXISTS idx_tool_definition_enabled ON tool_definition (enabled);
CREATE INDEX IF NOT EXISTS idx_ai_call_log_model_id ON ai_call_log (model_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_fts
    ON document_chunk USING GIN (to_tsvector('simple', chunk_text));
