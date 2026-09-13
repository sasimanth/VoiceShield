CREATE TABLE speaker_profiles (
    id BIGSERIAL PRIMARY KEY,
    speaker_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200),
    audio_data BYTEA NOT NULL,
    embedding_dimension INTEGER NOT NULL DEFAULT 192,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_speaker_embedding_dimension
        CHECK (embedding_dimension = 192),

    CONSTRAINT chk_speaker_embedding_size
        CHECK (octet_length(audio_data) = 768)
);

CREATE INDEX idx_speaker_profiles_speaker_id
    ON speaker_profiles (speaker_id);