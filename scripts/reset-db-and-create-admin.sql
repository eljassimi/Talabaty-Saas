-- Reset database and create admin account for Talabaty
-- Run with: psql or via reset-db-and-create-admin.sh

-- Truncate all application tables (preserves Liquibase changelog)
TRUNCATE
  webhook_subscription_events,
  webhook_subscriptions,
  order_status_history,
  order_import_rows,
  orders,
  order_import_batches,
  stored_files,
  store_team_members,
  store_settings,
  excel_sync_configs,
  youcan_stores,
  shipping_providers,
  stores,
  api_credentials,
  users,
  accounts
RESTART IDENTITY CASCADE;

-- Create admin account (ID will be generated; we use a fixed UUID for predictability)
INSERT INTO accounts (id, name, type, status, created_at, updated_at, version)
VALUES (
  'a0000000-0000-0000-0000-000000000001',
  'Admin Account',
  'INDIVIDUAL',
  'ACTIVE',
  NOW(),
  NOW(),
  0
);

-- Create admin user (PLATFORM_ADMIN role)
-- Password: admin123 (change after first login)
-- BCrypt hash for 'admin123' (strength 10)
INSERT INTO users (id, account_id, email, password_hash, first_name, last_name, role, status, created_at, updated_at, must_change_password)
VALUES (
  'u0000000-0000-0000-0000-000000000001',
  'a0000000-0000-0000-0000-000000000001',
  'admin@talabaty.local',
  '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW',
  'Admin',
  'User',
  'PLATFORM_ADMIN',
  'ACTIVE',
  NOW(),
  NOW(),
  false
);
