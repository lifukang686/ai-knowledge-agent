CREATE TABLE IF NOT EXISTS evaluation_dataset (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    knowledge_base_id BIGINT,
    target_type VARCHAR(40) NOT NULL DEFAULT 'RAG_QA',
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_evaluation_dataset_kb ON evaluation_dataset (knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_dataset_create_time ON evaluation_dataset (create_time DESC);

CREATE TABLE IF NOT EXISTS evaluation_case (
    id BIGINT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    expected_answer TEXT,
    expected_keywords TEXT,
    expected_chunk_ids TEXT,
    expected_status VARCHAR(40),
    metadata TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_evaluation_case_dataset ON evaluation_case (dataset_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_evaluation_case_enabled ON evaluation_case (enabled);

CREATE TABLE IF NOT EXISTS evaluation_run (
    id BIGINT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    target_type VARCHAR(40) NOT NULL DEFAULT 'RAG_QA',
    status VARCHAR(30) NOT NULL,
    total_count INTEGER NOT NULL DEFAULT 0,
    passed_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    avg_score DOUBLE PRECISION,
    avg_latency_ms BIGINT,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_evaluation_run_dataset ON evaluation_run (dataset_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_evaluation_run_status ON evaluation_run (status);

CREATE TABLE IF NOT EXISTS evaluation_case_result (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    expected_answer TEXT,
    actual_answer TEXT,
    rewritten_query TEXT,
    expected_status VARCHAR(40),
    actual_status VARCHAR(40),
    expected_keywords TEXT,
    expected_chunk_ids TEXT,
    retrieved_chunks TEXT,
    reranked_chunks TEXT,
    retrieval_hit_score DOUBLE PRECISION,
    keyword_score DOUBLE PRECISION,
    status_score DOUBLE PRECISION,
    total_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    metric_detail TEXT,
    latency_ms BIGINT,
    error_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT now(),
    update_time TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_evaluation_case_result_run ON evaluation_case_result (run_id, create_time ASC);
CREATE INDEX IF NOT EXISTS idx_evaluation_case_result_case ON evaluation_case_result (case_id);
CREATE INDEX IF NOT EXISTS idx_evaluation_case_result_passed ON evaluation_case_result (passed);
