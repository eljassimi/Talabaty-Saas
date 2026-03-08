-- Add missing user columns (run once if signup fails with "must_change_password does not exist")
-- Run from project root:
--   Local DB:  psql -h localhost -p 5432 -U postgres -d talabaty -f scripts/add-missing-user-columns.sql
--   Docker:   psql -h localhost -p 5433 -U postgres -d talabaty -f scripts/add-missing-user-columns.sql
--   (password: postgres for Docker, or your local postgres password)

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS selected_store_id UUID;
