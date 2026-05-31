ALTER TABLE document_chunk
    ADD COLUMN IF NOT EXISTS search_text TEXT;

UPDATE document_chunk
SET search_text = chunk_text
WHERE search_text IS NULL;

DROP INDEX IF EXISTS idx_document_chunk_fts;

CREATE INDEX IF NOT EXISTS idx_document_chunk_search_text_fts
    ON document_chunk USING GIN (to_tsvector('simple', COALESCE(search_text, chunk_text)));
