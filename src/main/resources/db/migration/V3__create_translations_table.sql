CREATE TABLE IF NOT EXISTS translations (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    field_value TEXT,
    CONSTRAINT uk_translations_entity_field_lang UNIQUE (entity_type, entity_id, field_name, language_code)
);

CREATE INDEX IF NOT EXISTS idx_translations_entity ON translations (entity_type, entity_id, language_code, field_name);
