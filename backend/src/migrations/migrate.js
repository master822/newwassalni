const fs = require('fs');
const path = require('path');
const db = require('../database');

async function runMigration() {
  const client = await db.pool.connect();
  try {
    console.log('🔄 Running PostgreSQL database migrations...');

    // Explicitly ensure the public schema exists and set search_path
    await client.query('CREATE SCHEMA IF NOT EXISTS public;');
    await client.query('SET search_path TO public;');

    const sqlPath = path.join(__dirname, 'init.sql');
    const sql = fs.readFileSync(sqlPath, 'utf8');
    await client.query(sql);

    console.log('✅ PostgreSQL migrations completed successfully');
  } catch (err) {
    console.error('❌ Migration failed:', err.message || err);
    throw err;
  } finally {
    client.release();
  }
}

if (require.main === module) {
  runMigration()
    .then(() => process.exit(0))
    .catch(() => process.exit(1));
}

module.exports = runMigration;
