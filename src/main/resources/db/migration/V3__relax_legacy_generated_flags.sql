ALTER TABLE document
    ALTER COLUMN summary_generated DROP NOT NULL;

ALTER TABLE document
    ALTER COLUMN title_generated DROP NOT NULL;
