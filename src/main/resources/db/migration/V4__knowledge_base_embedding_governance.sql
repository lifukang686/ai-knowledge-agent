ALTER TABLE knowledge_base
    ADD COLUMN IF NOT EXISTS embedding_model_id BIGINT,
    ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER,
    ADD COLUMN IF NOT EXISTS embedding_version VARCHAR(64);

ALTER TABLE document
    ADD COLUMN IF NOT EXISTS embedding_model_id BIGINT,
    ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER,
    ADD COLUMN IF NOT EXISTS embedding_version VARCHAR(64);

ALTER TABLE document_chunk
    ADD COLUMN IF NOT EXISTS embedding_model_id BIGINT,
    ADD COLUMN IF NOT EXISTS embedding_dimension INTEGER,
    ADD COLUMN IF NOT EXISTS embedding_version VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_knowledge_base_embedding_model_id ON knowledge_base (embedding_model_id);
CREATE INDEX IF NOT EXISTS idx_document_embedding_model_id ON document (embedding_model_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding_model_id ON document_chunk (embedding_model_id);
