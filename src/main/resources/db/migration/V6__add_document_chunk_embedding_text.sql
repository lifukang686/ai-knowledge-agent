ALTER TABLE document_chunk
    ADD COLUMN IF NOT EXISTS embedding_text TEXT,
    ADD COLUMN IF NOT EXISTS embedding_text_version VARCHAR(32);

UPDATE document_chunk
SET embedding_text = chunk_text,
    embedding_text_version = 'legacy-raw-v1'
WHERE embedding_text IS NULL;
