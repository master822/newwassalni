-- ==========================================================
-- Wassalni (وصلني) Production Database Schema - PostgreSQL
-- ==========================================================

-- 0. Ensure public schema exists and search_path is explicitly set
CREATE SCHEMA IF NOT EXISTS public;
SET search_path TO public;

-- Optional: Safely create uuid-ossp extension without failing on constrained cloud databases (e.g. filess.io)
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
EXCEPTION
    WHEN OTHERS THEN
        NULL;
END $$;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    phone VARCHAR(32) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url TEXT DEFAULT '',
    rating NUMERIC(3, 2) DEFAULT 5.0,
    ride_count INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT TRUE,
    wallet_points INT DEFAULT 50, -- Starting Bonus: Exactly 50 points
    is_suspended BOOLEAN DEFAULT FALSE,
    suspend_reason TEXT DEFAULT NULL,
    role VARCHAR(32) DEFAULT 'USER', -- USER, DRIVER, ADMIN, SUPER_ADMIN
    user_role VARCHAR(32) DEFAULT 'راكب وسائق',
    referral_code VARCHAR(32) UNIQUE NOT NULL,
    fcm_token TEXT DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Refresh Tokens Table (JWT rotation & revocation)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. OTP Verifications Table (Server-Side Rate Limited & Hashed OTP)
CREATE TABLE IF NOT EXISTS otp_verifications (
    id VARCHAR(64) PRIMARY KEY,
    phone VARCHAR(32) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    attempts INT DEFAULT 0,
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Rides Table (Published by Drivers)
CREATE TABLE IF NOT EXISTS rides (
    id VARCHAR(64) PRIMARY KEY,
    driver_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    driver_name VARCHAR(120) NOT NULL,
    driver_avatar TEXT DEFAULT '',
    driver_rating NUMERIC(3, 2) DEFAULT 5.0,
    driver_trip_count INT DEFAULT 0,
    driver_verified BOOLEAN DEFAULT TRUE,
    start_city VARCHAR(64) NOT NULL,
    end_city VARCHAR(64) NOT NULL,
    departure_date VARCHAR(32) NOT NULL,
    departure_time VARCHAR(32) NOT NULL,
    duration VARCHAR(32) DEFAULT '2 سا',
    price_per_seat NUMERIC(8, 2) NOT NULL DEFAULT 5.0,
    available_seats INT NOT NULL DEFAULT 3,
    total_seats INT NOT NULL DEFAULT 4,
    car_model VARCHAR(64) DEFAULT 'تويوتا كامري',
    car_color VARCHAR(32) DEFAULT 'فضي',
    car_plate VARCHAR(32) DEFAULT 'دمشق 123456',
    allows_luggage BOOLEAN DEFAULT TRUE,
    accept_cash BOOLEAN DEFAULT TRUE,
    accept_wallet BOOLEAN DEFAULT TRUE,
    is_women_only BOOLEAN DEFAULT FALSE,
    status VARCHAR(32) DEFAULT 'UPCOMING', -- UPCOMING, COMPLETED, CANCELLED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Ride Bookings Table (Passengers booking seats)
CREATE TABLE IF NOT EXISTS ride_bookings (
    id VARCHAR(64) PRIMARY KEY,
    ride_id VARCHAR(64) NOT NULL REFERENCES rides(id) ON DELETE CASCADE,
    passenger_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    passenger_name VARCHAR(120) NOT NULL,
    seats_booked INT NOT NULL DEFAULT 1,
    status VARCHAR(32) DEFAULT 'UPCOMING', -- UPCOMING, COMPLETED, CANCELLED
    booked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Requested Trips Table (Trip requests posted by passengers)
CREATE TABLE IF NOT EXISTS requested_trips (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_name VARCHAR(120) NOT NULL,
    user_phone VARCHAR(32) NOT NULL,
    user_avatar TEXT DEFAULT '',
    start_city VARCHAR(64) NOT NULL,
    end_city VARCHAR(64) NOT NULL,
    departure_date VARCHAR(32) NOT NULL,
    departure_time VARCHAR(32) NOT NULL,
    men_count INT DEFAULT 1,
    women_count INT DEFAULT 0,
    children_count INT DEFAULT 0,
    status VARCHAR(32) DEFAULT 'OPEN', -- OPEN, ACCEPTED, CANCELLED
    accepted_by_driver_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    accepted_by_driver_name VARCHAR(120) DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Wallet Transactions Table (Double-entry Ledger)
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL, -- WELCOME_BONUS, REFERRAL_REWARD, TOP_UP, TRANSFER, COMMISSION, DEDUCTION, ADMIN_ADJUSTMENT
    points INT NOT NULL,
    amount_usd NUMERIC(8, 2) DEFAULT 0.0,
    description TEXT NOT NULL,
    status VARCHAR(32) DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. TopUp Requests Table (Cham Cash receipts)
CREATE TABLE IF NOT EXISTS topup_requests (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_name VARCHAR(120) NOT NULL,
    package_points INT NOT NULL,
    package_price_usd NUMERIC(8, 2) NOT NULL,
    receipt_image_path TEXT DEFAULT '',
    status VARCHAR(32) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    rejection_reason TEXT DEFAULT NULL,
    processed_by_admin_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

-- 9. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(32) DEFAULT 'SYSTEM', -- SYSTEM, BOOKING, APPROVAL, REFERRAL, BROADCAST
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. Chat Messages Table
CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(64) PRIMARY KEY,
    ride_id VARCHAR(64) NOT NULL REFERENCES rides(id) ON DELETE CASCADE,
    sender_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_name VARCHAR(120) NOT NULL,
    sender_avatar TEXT DEFAULT '',
    message TEXT NOT NULL,
    timestamp VARCHAR(32) NOT NULL,
    is_driver BOOLEAN DEFAULT FALSE,
    image_uri TEXT DEFAULT NULL,
    is_location BOOLEAN DEFAULT FALSE,
    latitude NUMERIC(10, 6) DEFAULT NULL,
    longitude NUMERIC(10, 6) DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 11. Admin Activity Logs Table
CREATE TABLE IF NOT EXISTS admin_activity_logs (
    id VARCHAR(64) PRIMARY KEY,
    admin_id VARCHAR(64) NOT NULL,
    admin_name VARCHAR(120) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) DEFAULT NULL,
    details TEXT NOT NULL,
    ip_address VARCHAR(45) DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 12. Support Tickets Table
CREATE TABLE IF NOT EXISTS support_tickets (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) REFERENCES users(id) ON DELETE SET NULL,
    user_name VARCHAR(120) NOT NULL,
    user_email VARCHAR(120) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message_text TEXT NOT NULL,
    priority VARCHAR(16) DEFAULT 'MEDIUM', -- HIGH, MEDIUM, LOW
    status VARCHAR(16) DEFAULT 'OPEN', -- OPEN, RESOLVED, CLOSED
    admin_reply TEXT DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 13. App Settings Table (Dynamic Remote Configuration)
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Default Settings Seed
INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('app_name', 'وصلني'),
    ('app_tagline', 'نسافر معاً، نوصل بأمان'),
    ('app_logo_url', 'https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=300'),
    ('sham_cash_account', 'ba64858e96d4ad9c6096948bc2dbc970'),
    ('maintenance_mode', 'false'),
    ('ride_publish_cost', '50'),
    ('app_commission_percent', '5.0'),
    ('cancellation_refund_percent', '100'),
    ('app_download_url', 'https://wasalni.app/download')
ON CONFLICT (setting_key) DO NOTHING;

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_rides_cities ON rides(start_city, end_city, departure_date, status);
CREATE INDEX IF NOT EXISTS idx_requested_trips_status ON requested_trips(status, departure_date);
CREATE INDEX IF NOT EXISTS idx_chat_ride ON chat_messages(ride_id, created_at);
CREATE INDEX IF NOT EXISTS idx_wallet_user ON wallet_transactions(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id, is_revoked);
CREATE INDEX IF NOT EXISTS idx_otp_phone ON otp_verifications(phone, expires_at);
