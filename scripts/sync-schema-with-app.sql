-- Sync database schema with application entities (run once if you get "column X does not exist" errors)
-- Run from project root:
--   Local:  psql -h localhost -p 5432 -U postgres -d talabaty -f scripts/sync-schema-with-app.sql
--   Docker: PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d talabaty -f scripts/sync-schema-with-app.sql

-- stores: logo and color
ALTER TABLE stores ADD COLUMN IF NOT EXISTS logo_url TEXT;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS color VARCHAR(7) DEFAULT '#0284c7';

-- orders: product/city/assigned_to
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ozon_tracking_number VARCHAR(120);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS city VARCHAR(120);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS product_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS assigned_to UUID REFERENCES users(id);

-- shipping_providers table
CREATE TABLE IF NOT EXISTS shipping_providers (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL REFERENCES accounts(id),
  store_id UUID REFERENCES stores(id),
  provider_type VARCHAR(32) NOT NULL,
  customer_id VARCHAR(100) NOT NULL,
  api_key VARCHAR(255) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT true,
  display_name VARCHAR(180),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- youcan_stores table
CREATE TABLE IF NOT EXISTS youcan_stores (
  id UUID PRIMARY KEY,
  account_id UUID NOT NULL REFERENCES accounts(id),
  store_id UUID NOT NULL REFERENCES stores(id),
  youcan_store_id VARCHAR(100) NOT NULL,
  youcan_store_domain VARCHAR(255),
  youcan_store_name VARCHAR(255),
  access_token VARCHAR(2000) NOT NULL,
  refresh_token VARCHAR(2000),
  token_expires_at TIMESTAMP WITH TIME ZONE,
  scopes VARCHAR(500),
  is_active BOOLEAN NOT NULL DEFAULT true,
  last_sync_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

