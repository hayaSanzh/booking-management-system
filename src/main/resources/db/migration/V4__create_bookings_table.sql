-- Bookings table
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES resources(id),
    user_id UUID NOT NULL REFERENCES users(id),
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT chk_booking_time CHECK (start_at < end_at)
);

-- Indexes for efficient overlap queries
CREATE INDEX idx_bookings_resource_time ON bookings(resource_id, start_at, end_at);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);

-- Composite index for filtering by resource and status (for active bookings check)
CREATE INDEX idx_bookings_resource_status ON bookings(resource_id, status);

COMMENT ON TABLE bookings IS 'Resource bookings with time slots';
COMMENT ON COLUMN bookings.status IS 'CREATED, CONFIRMED, CANCELED';
