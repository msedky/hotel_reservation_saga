CREATE TABLE flight_reservations (
 id UUID PRIMARY KEY,
 booking_id UUID NOT NULL UNIQUE,
 saga_id UUID NOT NULL,
 flight_offer_id VARCHAR(255) NOT NULL,
 provider_reference VARCHAR(255),
 status VARCHAR(30) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_flight_reservations_saga_id ON flight_reservations(saga_id);
