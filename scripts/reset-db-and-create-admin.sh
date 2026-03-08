#!/usr/bin/env bash
#
# Reset Talabaty database and create admin account.
# Usage:
#   ./scripts/reset-db-and-create-admin.sh
#   ./scripts/reset-db-and-create-admin.sh --docker   # use Docker Compose postgres
#
# Admin login after run:
#   Email:    admin@talabaty.local
#   Password: admin123
# (Change password after first login.)

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$SCRIPT_DIR/reset-db-and-create-admin.sql"

# Default: local PostgreSQL (from application.properties)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-talabaty}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-123456789}"

if [[ "${1:-}" == "--docker" ]]; then
  DB_HOST="${DB_HOST:-localhost}"
  DB_PORT="${DB_PORT:-5433}"
  DB_USER="${DB_USER:-postgres}"
  DB_PASSWORD="${DB_PASSWORD:-postgres}"
  echo "Using Docker Compose Postgres (port 5433). Ensure: docker compose up -d postgres"
fi

export PGPASSWORD="$DB_PASSWORD"
echo "Connecting to $DB_NAME at $DB_HOST:$DB_PORT as $DB_USER ..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE"
unset PGPASSWORD
echo "Done. Admin account created: admin@talabaty.local / admin123"
