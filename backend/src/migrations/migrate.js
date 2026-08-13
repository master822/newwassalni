const fs = require('fs');
const path = require('path');
const db = require('../database');

async function runMigration() {
  const client = await db.pool.connect();
  try {
    console.log('🔄 Running PostgreSQL database migrations...');
    const sqlPath = path.join(__dirname, 'init.sql');
    const sql = fs.readFileSync(sqlPath, 'utf8');
    await client.query(sql);
    console.log('✅ Database migrations executed successfully.');
  } catch (err) {
    console.error('❌ Migration failed:', err);
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
