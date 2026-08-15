const { Pool } = require('pg');
require('dotenv').config();

/**
 * Determine SSL configuration based on DATABASE_SSL environment variable.
 * - Set DATABASE_SSL=false for database providers that do not support SSL (e.g. filess.io).
 * - Set DATABASE_SSL=true for providers requiring SSL (e.g. Neon, AWS RDS, Render Postgres).
 * - Defaults to false when not specified or when DATABASE_SSL is 'false', preventing connection failures.
 */
function getSslConfig() {
  const envSsl = process.env.DATABASE_SSL ? process.env.DATABASE_SSL.trim().toLowerCase() : null;

  if (envSsl === 'false' || envSsl === '0' || envSsl === 'off') {
    return false;
  }

  if (envSsl === 'true' || envSsl === '1' || envSsl === 'on') {
    return { rejectUnauthorized: false };
  }

  return false;
}

const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/wassalni',
  ssl: getSslConfig(),
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle PostgreSQL client', err);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool,
};
