CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    knowledge_base_id BIGINT,
    title VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    rewritten_query TEXT,
    status VARCHAR(20),
    metadata TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conversation_summary (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    summary TEXT NOT NULL,
    message_until_id BIGINT,
    token_estimate INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversation_user_id ON conversation (user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_knowledge_base_id ON conversation (knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_conversation_message_conversation_id_create_time
    ON conversation_message (conversation_id, create_time);
CREATE INDEX IF NOT EXISTS idx_conversation_summary_conversation_id
    ON conversation_summary (conversation_id);
