const fs = require('fs');
const path = require('path');
const db = require('../database');

async function runMigration() {
  const client = await db.pool.connect();

  try {
    console.log('Running PostgreSQL database migrations...');
    console.log(`Application schema: ${db.schema}`);

    await client.query(`
      CREATE SCHEMA IF NOT EXISTS "${db.schema}"
    `);

    await client.query(`
      SET search_path TO "${db.schema}"
    `);

    const diagRes = await client.query(`
      SELECT
        current_user AS db_user,
        current_database() AS db_name,
        current_schema() AS db_schema,
        current_setting('search_path') AS search_path,
        has_database_privilege(
          current_user,
          current_database(),
          'CREATE'
        ) AS database_create,
        has_schema_privilege(
          current_user,
          '${db.schema}',
          'USAGE'
        ) AS schema_usage,
        has_schema_privilege(
          current_user,
          '${db.schema}',
          'CREATE'
        ) AS schema_create
    `);

    if (diagRes.rows.length > 0) {
      const d = diagRes.rows[0];

      console.log(
        `Connected to database: ${d.db_name} ` +
        `as user: ${d.db_user} ` +
        `(schema: ${d.db_schema}, search_path: ${d.search_path})`
      );

      console.log(
        `Schema privileges: ` +
        `USAGE=${d.schema_usage}, CREATE=${d.schema_create}`
      );

      if (!d.schema_usage || !d.schema_create) {
        throw new Error(
          `PostgreSQL user does not have sufficient privileges on schema "${db.schema}"`
        );
      }
    }

    const sqlPath = path.join(__dirname, 'init.sql');
    const sql = fs.readFileSync(sqlPath, 'utf8');

    await client.query('BEGIN');

    await client.query(`
      SET LOCAL search_path TO "${db.schema}"
    `);

    await client.query(sql);

    await client.query('COMMIT');

    console.log(
      `PostgreSQL migrations completed successfully in schema "${db.schema}"`
    );
  } catch (err) {
    try {
      await client.query('ROLLBACK');
    } catch (_) {
    }

    console.error('Migration failed:', err.message || err);
    throw err;
  } finally {
    client.release();
  }
}

if (require.main === module) {
  runMigration()
    .then(() => {
      console.log('Migration process finished.');
      process.exit(0);
    })
    .catch((err) => {
      console.error(
        'Migration process failed with error:',
        err.message || err
      );
      process.exit(1);
    });
}

module.exports = runMigration;
