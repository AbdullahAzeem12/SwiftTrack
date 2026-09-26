-- SwiftTrack Seed Data Migration V2

-- Stations
INSERT INTO stations (id, code, name, city, is_airport_terminal, terminal_code, latitude, longitude) VALUES
('11111111-1111-1111-1111-111111111111', 'PAD', 'London Paddington', 'London', FALSE, NULL, 51.5167, -0.1756),
('22222222-2222-2222-2222-222222222222', 'HXX', 'Heathrow Central (Terminals 2 & 3)', 'London', TRUE, 'T23', 51.4719, -0.4542),
('33333333-3333-3333-3333-333333333333', 'HAF', 'Heathrow Terminal 4', 'London', TRUE, 'T4', 51.4583, -0.4448),
('44444444-4444-4444-4444-444444444444', 'HWV', 'Heathrow Terminal 5', 'London', TRUE, 'T5', 51.4700, -0.4897),
('55555555-5555-5555-5555-555555555555', 'GTW', 'Gatwick Airport', 'London', TRUE, 'GTW', 51.1568, -0.1611),
('66666666-6666-6666-6666-666666666666', 'STP', 'St Pancras International', 'London', FALSE, NULL, 51.5314, -0.1261);

-- Routes
INSERT INTO routes (id, code, name, origin_station_id, destination_station_id) VALUES
('a1111111-1111-1111-1111-111111111111', 'EXPRESS_PAD_HWV', 'Paddington to Heathrow T5 Express', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444'),
('a2222222-2222-2222-2222-222222222222', 'EXPRESS_HWV_PAD', 'Heathrow T5 to Paddington Express', '44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111');

-- Passenger Types
INSERT INTO passenger_types (id, code, name, discount_multiplier, min_age, max_age) VALUES
('p1111111-1111-1111-1111-111111111111', 'ADULT', 'Adult (16+)', 1.00, 16, 120),
('p2222222-2222-2222-2222-222222222222', 'CHILD', 'Child (5-15)', 0.50, 5, 15),
('p3333333-3333-3333-3333-333333333333', 'SENIOR', 'Senior (60+)', 0.67, 60, 120),
('p4444444-4444-4444-4444-444444444444', 'STUDENT', 'Student / Railcard', 0.67, 16, 30);

-- Fare Products
INSERT INTO fare_products (id, code, name, travel_class, validity_type, base_price_minor, currency, is_refundable, is_changeable, description) VALUES
('f1111111-1111-1111-1111-111111111111', 'EXPRESS_SINGLE', 'Express Standard Single', 'STANDARD', 'SINGLE', 2500, 'GBP', TRUE, TRUE, 'Standard Single travel between London Paddington and Heathrow.'),
('f2222222-2222-2222-2222-222222222222', 'EXPRESS_RETURN', 'Express Standard Return', 'STANDARD', 'RETURN', 3700, 'GBP', TRUE, TRUE, 'Standard Return travel valid for 5 days outbound and 30 days return.'),
('f3333333-3333-3333-3333-333333333333', 'BUSINESS_SINGLE', 'Business First Single', 'FIRST', 'SINGLE', 3200, 'GBP', TRUE, TRUE, 'Premium seat with free Wi-Fi, extra legroom & quiet car.'),
('f4444444-4444-4444-4444-444444444444', 'BUSINESS_RETURN', 'Business First Return', 'FIRST', 'RETURN', 5500, 'GBP', TRUE, TRUE, 'Premium Return travel with priority boarding and lounge access.');

-- Promo Codes
INSERT INTO promo_codes (id, code, discount_percent, max_discount_minor, valid_from, valid_to, usage_limit, times_used, is_active) VALUES
('c1111111-1111-1111-1111-111111111111', 'EXPRESS20', 20, 1000, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '90 days', 5000, 12, TRUE),
('c2222222-2222-2222-2222-222222222222', 'SUMMER10', 10, 500, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '60 days', 2000, 45, TRUE);

-- Service Alerts
INSERT INTO service_alerts (id, title, description, severity, affected_route_id, is_active, starts_at, ends_at) VALUES
('s1111111-1111-1111-1111-111111111111', 'Normal Service Operating', 'Trains running non-stop every 15 minutes between London Paddington and Heathrow Terminals.', 'INFO', 'a1111111-1111-1111-1111-111111111111', TRUE, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '30 days'),
('s2222222-2222-2222-2222-222222222222', 'Scheduled Maintenance at Terminal 4', 'Transfers to Terminal 4 operate via shuttle bus from Heathrow Central.', 'WARNING', 'a1111111-1111-1111-1111-111111111111', TRUE, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '5 days');

-- Journeys (Generate journeys for today and tomorrow)
INSERT INTO journeys (id, route_id, service_code, departure_station_id, arrival_station_id, scheduled_departure, scheduled_arrival, status, platform, capacity_level) VALUES
('j1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 'HX-101', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP + INTERVAL '15 minutes', CURRENT_TIMESTAMP + INTERVAL '30 minutes', 'ON_TIME', 'Platform 6', 'NORMAL'),
('j2222222-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', 'HX-102', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP + INTERVAL '30 minutes', CURRENT_TIMESTAMP + INTERVAL '45 minutes', 'ON_TIME', 'Platform 7', 'NORMAL'),
('j3333333-3333-3333-3333-333333333333', 'a1111111-1111-1111-1111-111111111111', 'HX-103', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP + INTERVAL '45 minutes', CURRENT_TIMESTAMP + INTERVAL '60 minutes', 'ON_TIME', 'Platform 6', 'SEATS_AVAILABLE'),
('j4444444-4444-4444-4444-444444444444', 'a2222222-2222-2222-2222-222222222222', 'HX-201', '44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP + INTERVAL '20 minutes', CURRENT_TIMESTAMP + INTERVAL '35 minutes', 'ON_TIME', 'Platform 2', 'NORMAL');

-- Default Admin User (password: AdminPass123! -> BCrypt hash)
INSERT INTO users (id, email, password_hash, full_name, role, status, email_verified) VALUES
('u1111111-1111-1111-1111-111111111111', 'admin@swifttrack.com', '$2a$10$wB50vL0T/bN9fDvhFzDce./oQ1ZlhbI/d5mAKPZf0Yx4kL39sQ2eS', 'System Administrator', 'SUPER_ADMIN', 'ACTIVE', TRUE),
('u2222222-2222-2222-2222-222222222222', 'testuser@swifttrack.com', '$2a$10$wB50vL0T/bN9fDvhFzDce./oQ1ZlhbI/d5mAKPZf0Yx4kL39sQ2eS', 'John Doe', 'CUSTOMER', 'ACTIVE', TRUE);

INSERT INTO user_profiles (id, user_id, preferred_currency, preferred_language) VALUES
('up111111-1111-1111-1111-111111111111', 'u1111111-1111-1111-1111-111111111111', 'GBP', 'en'),
('up222222-2222-2222-2222-222222222222', 'u2222222-2222-2222-2222-222222222222', 'GBP', 'en');

-- App Configuration
INSERT INTO app_configuration (config_key, config_value) VALUES
('MAX_PASSENGERS_PER_BOOKING', '8'),
('QUOTE_TTL_SECONDS', '900'),
('SUPPORT_EMAIL', 'support@swifttrack.com'),
('MIN_APP_VERSION_ANDROID', '1.0.0');
