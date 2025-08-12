ALTER TABLE document
    ADD COLUMN IF NOT EXISTS last_error VARCHAR(2000);

CREATE INDEX IF NOT EXISTS idx_document_status ON document (status);
CREATE INDEX IF NOT EXISTS idx_document_upload_date ON document (upload_date);
