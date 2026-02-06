-- V3__create_resources_table.sql
-- Create resources table (meeting rooms)

CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    capacity INTEGER NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resources_name ON resources(name);
CREATE INDEX idx_resources_location ON resources(location);
CREATE INDEX idx_resources_capacity ON resources(capacity);
CREATE INDEX idx_resources_is_active ON resources(is_active);
