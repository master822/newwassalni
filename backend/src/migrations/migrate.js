const fs = require('fs');
const path = require('path');
const db = require('../database');

async function runMigration() {
  const client = await db.pool.connect();
  try {
    console.log('🔄 Running PostgreSQL database migrations...');

    // Log diagnostic information to help identify environment details safely
    try {
      const diagRes = await client.query(`
        SELECT 
          current_user AS db_user, 
          current_database() AS db_name, 
          current_schema() AS db_schema,
          current_setting('search_path') AS search_path
      `);
      if (diagRes.rows && diagRes.rows.length > 0) {
        const d = diagRes.rows[0];
        console.log(`ℹ️ Connected to database: ${d.db_name} as user: ${d.db_user} (schema: ${d.db_schema}, search_path: ${d.search_path})`);
      }
    } catch (diagErr) {
      console.warn('⚠️ Could not fetch diagnostic schema info:', diagErr.message);
    }

    const sqlPath = path.join(__dirname, 'init.sql');
    const sql = fs.readFileSync(sqlPath, 'utf8');

    await client.query(sql);

    console.log('✅ PostgreSQL database migrations completed successfully');
  } catch (err) {
    console.error('❌ Migration failed:', err.message || err);
    throw err;
  } finally {
    client.release();
  }
}

if (require.main === module) {
  runMigration()
    .then(() => {
      console.log('🏁 Migration process finished.');
      process.exit(0);
    })
    .catch((err) => {
      console.error('🛑 Migration process failed with error:', err.message || err);
      process.exit(1);
    });
}

module.exports = runMigration;
