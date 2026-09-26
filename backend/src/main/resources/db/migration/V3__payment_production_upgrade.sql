-- SwiftTrack Database Migration V3: Production Payment Architecture Upgrade
-- Supports Stripe PaymentSheet, PayPal Orders v2, Multi-Currency, Webhook Auditing, and Payout Settlements

ALTER TABLE payments 
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50) NOT NULL DEFAULT 'STRIPE',
    ADD COLUMN IF NOT EXISTS provider_order_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS client_secret_ref VARCHAR(255),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS failure_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS failure_message VARCHAR(500),
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE payment_webhook_events
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50) NOT NULL DEFAULT 'STRIPE',
    ADD COLUMN IF NOT EXISTS signature_verified BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS processing_status VARCHAR(50) NOT NULL DEFAULT 'PROCESSED';

ALTER TABLE payment_refunds
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50) NOT NULL DEFAULT 'STRIPE',
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'GBP',
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS payout_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider VARCHAR(50) NOT NULL, -- STRIPE, PAYPAL
    provider_payout_id VARCHAR(255) NOT NULL UNIQUE,
    amount_minor INT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'GBP',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_TRANSIT, PAID, FAILED, CANCELLED
    destination_bank_last4 VARCHAR(10),
    arrival_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Production Performance & Search Indexes
CREATE INDEX IF NOT EXISTS idx_payments_provider ON payments(provider, provider_payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_idempotency ON payments(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_payment_webhook_provider_event ON payment_webhook_events(provider, event_id);
CREATE INDEX IF NOT EXISTS idx_payout_records_provider ON payout_records(provider, status);
