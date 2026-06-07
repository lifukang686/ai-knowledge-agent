CREATE TABLE IF NOT EXISTS user_memory (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    memory_type VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 0.8,
    source_conversation_id BIGINT,
    source_message_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_memory_user_status
    ON user_memory (user_id, status);

CREATE INDEX IF NOT EXISTS idx_user_memory_user_type
    ON user_memory (user_id, memory_type);
