const { Pool } = require('pg');

require('dotenv').config();

const connectionString = process.env.DATABASE_URL;

if (!connectionString) {
  throw new Error('DATABASE_URL is not configured');
}

const dbSchema =
  process.env.DB_SCHEMA ||
  process.env.POSTGRES_SCHEMA ||
  'wassalni_takenenemy';

if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(dbSchema)) {
  throw new Error(`Invalid DB_SCHEMA: ${dbSchema}`);
}

const pool = new Pool({
  connectionString,
  ssl: false,
  options: `-c search_path="${dbSchema}"`,
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 15000
});

pool.on('connect', async (client) => {
  try {
    await client.query(`SET search_path TO "${dbSchema}"`);
  } catch (err) {
    console.error(
      `Failed to set PostgreSQL search_path to ${dbSchema}:`,
      err.message
    );
  }
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle PostgreSQL client:', err);
});

const db = {
  query: (text, params) => pool.query(text, params),
  pool,
  schema: dbSchema
};

module.exports = db;
