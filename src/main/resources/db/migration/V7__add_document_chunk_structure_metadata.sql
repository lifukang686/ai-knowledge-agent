ALTER TABLE document_chunk
    ADD COLUMN IF NOT EXISTS page_number INTEGER,
    ADD COLUMN IF NOT EXISTS section_title VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_document_chunk_page_number ON document_chunk (document_id, page_number);
