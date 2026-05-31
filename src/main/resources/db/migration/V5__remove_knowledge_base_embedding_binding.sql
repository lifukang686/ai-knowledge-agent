DROP INDEX IF EXISTS idx_knowledge_base_embedding_model_id;

ALTER TABLE knowledge_base
    DROP COLUMN IF EXISTS embedding_model_id,
    DROP COLUMN IF EXISTS embedding_dimension,
    DROP COLUMN IF EXISTS embedding_version;
