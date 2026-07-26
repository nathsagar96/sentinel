CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255),
    name        VARCHAR(255) NOT NULL,
    avatar_url  VARCHAR(255),
    provider    VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255),
    created_at  TIMESTAMP(6),
    updated_at  TIMESTAMP(6)
);

CREATE INDEX idx_users_provider_provider_id ON users (provider, provider_id);
