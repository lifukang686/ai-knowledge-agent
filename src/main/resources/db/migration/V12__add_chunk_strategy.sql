CREATE TABLE IF NOT EXISTS chunk_strategy (
    id BIGINT PRIMARY KEY,
    strategy_name VARCHAR(100) NOT NULL,
    chunk_type VARCHAR(32) NOT NULL,
    max_segment_size INTEGER NOT NULL,
    overlap_size INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chunk_strategy_type ON chunk_strategy (chunk_type);
CREATE INDEX IF NOT EXISTS idx_chunk_strategy_default ON chunk_strategy (is_default);
CREATE UNIQUE INDEX IF NOT EXISTS uk_chunk_strategy_default
    ON chunk_strategy (is_default)
    WHERE is_default = TRUE;
