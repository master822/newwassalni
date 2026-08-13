'use strict';

const { execFileSync } = require('child_process');
const { URL } = require('url');

const raw = process.env.DATABASE_URL;

if (!raw) {
  throw new Error('DATABASE_URL is missing');
}

const u = new URL(raw);

const PG = {
  host: u.hostname,
  port: u.port || '5432',
  user: decodeURIComponent(u.username),
  password: decodeURIComponent(u.password),
  database: decodeURIComponent(u.pathname.replace(/^\/+/, ''))
};

function convertSql(sql) {
  let q = String(sql);

  // SQLite -> PostgreSQL
  q = q.replace(/\bunixepoch\(\)/gi,
    'EXTRACT(EPOCH FROM NOW())::bigint'
  );

  q = q.replace(/\bCURRENT_TIMESTAMP\b/gi, 'NOW()');

  // SQLite boolean-ish integer defaults are already compatible.
  // PostgreSQL does not have sqlite_master.
  q = q.replace(/\bsqlite_master\b/gi, 'information_schema.tables');
  q = q.replace(/\bWHERE\s+type\s*=\s*['"]table['"]/gi, "WHERE table_type = 'BASE TABLE'");
  q = q.replace(/\bAND\s+type\s*=\s*['"]table['"]/gi, "AND table_type = 'BASE TABLE'");
  q = q.replace(/\bname\s*=\s*\?/gi, 'table_name = ?');
  q = q.replace(/\bname\s+NOT\s+LIKE/gi, 'table_name NOT LIKE');

  // sqlite_master.name -> information_schema.tables.table_name
  q = q.replace(/\bSELECT\s+name\b/gi, 'SELECT table_name');

  // SQLite internal table filter
  q = q.replace(
    /\s+AND\s+name\s+NOT\s+LIKE\s+['"]sqlite_%['"]/gi,
    ''
  );

  // SQLite INSERT OR IGNORE
  q = q.replace(
    /INSERT\s+OR\s+IGNORE\s+INTO/gi,
    'INSERT INTO'
  );

  // SQLite INSERT OR REPLACE -> basic PostgreSQL equivalent.
  q = q.replace(
    /INSERT\s+OR\s+REPLACE\s+INTO/gi,
    'INSERT INTO'
  );

  // SQLite PRAGMA table_info(...)
  const pragma = q.match(
    /PRAGMA\s+table_info\s*\(\s*["']?([A-Za-z0-9_]+)["']?\s*\)/i
  );

  if (pragma) {
    const table = pragma[1].replace(/'/g, "''");

    return `
      SELECT
        column_name AS name
      FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = '${table}'
      ORDER BY ordinal_position
    `;
  }

  // SQLite placeholders ? -> PostgreSQL $1, $2...
  let index = 0;
  q = q.replace(/\?/g, () => `$${++index}`);

  return q;
}

function sqlLiteral(value) {
  if (value === null || value === undefined) {
    return 'NULL';
  }

  if (typeof value === 'number') {
    if (!Number.isFinite(value)) return 'NULL';
    return String(value);
  }

  if (typeof value === 'boolean') {
    return value ? 'TRUE' : 'FALSE';
  }

  if (value instanceof Date) {
    return `'${value.toISOString().replace(/'/g, "''")}'`;
  }

  return `'${String(value).replace(/'/g, "''")}'`;
}

function bind(sql, params = []) {
  let i = 0;

  return sql.replace(/\$(\d+)/g, (_, n) => {
    const index = Number(n) - 1;

    if (index < 0 || index >= params.length) {
      throw new Error(`Missing SQL parameter $${n}`);
    }

    i++;
    return sqlLiteral(params[index]);
  });
}

function psql(sql, params = []) {
  const converted = convertSql(sql);
  const finalSql = bind(converted, params);

  return execFileSync(
    'psql',
    [
      '-X',
      '-q',
      '-t',
      '-A',
      '-v',
      'ON_ERROR_STOP=1',
      '-h',
      PG.host,
      '-p',
      PG.port,
      '-U',
      PG.user,
      '-d',
      PG.database,
      '-c',
      finalSql
    ],
    {
      env: {
        ...process.env,
        PGPASSWORD: PG.password
      },
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe']
    }
  ).trim();
}

function queryRows(sql, params = []) {
  const converted = convertSql(sql);
  const finalSql = bind(converted, params);

  const wrapped = `
    SELECT COALESCE(
      json_agg(row_to_json(q)),
      '[]'::json
    )
    FROM (
      ${finalSql}
    ) q
  `;

  const output = execFileSync(
    'psql',
    [
      '-X',
      '-q',
      '-t',
      '-A',
      '-v',
      'ON_ERROR_STOP=1',
      '-h',
      PG.host,
      '-p',
      PG.port,
      '-U',
      PG.user,
      '-d',
      PG.database,
      '-c',
      wrapped
    ],
    {
      env: {
        ...process.env,
        PGPASSWORD: PG.password
      },
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe']
    }
  ).trim();

  if (!output) return [];

  return JSON.parse(output);
}

function createDatabaseAdapter() {
  let transactionActive = false;
  let transactionQueue = [];

  function executeTransaction() {
    if (!transactionQueue.length) {
      return;
    }

    const transactionSql = [
      'BEGIN;',
      ...transactionQueue.map(sql => `${sql};`),
      'COMMIT;'
    ].join('\n');

    try {
      execFileSync(
        'psql',
        [
          '-X',
          '-q',
          '-v',
          'ON_ERROR_STOP=1',
          '-h',
          PG.host,
          '-p',
          PG.port,
          '-U',
          PG.user,
          '-d',
          PG.database,
          '-c',
          transactionSql
        ],
        {
          env: {
            ...process.env,
            PGPASSWORD: PG.password
          },
          encoding: 'utf8',
          stdio: ['ignore', 'pipe', 'pipe']
        }
      );
    } finally {
      transactionQueue = [];
    }
  }

  return {
    prepare(sql) {
      return {
        all(...params) {
          return queryRows(sql, params);
        },

        get(...params) {
          const rows = queryRows(sql, params);
          return rows.length ? rows[0] : undefined;
        },

        run(...params) {
          const converted = convertSql(sql);
          const finalSql = bind(converted, params);

          if (transactionActive) {
            transactionQueue.push(finalSql);

            return {
              changes: 0,
              lastInsertRowid: undefined
            };
          }

          psql(sql, params);

          return {
            changes: 0,
            lastInsertRowid: undefined
          };
        }
      };
    },

    exec(sql) {
      if (transactionActive) {
        transactionQueue.push(convertSql(sql));
        return;
      }

      psql(sql);
    },

    pragma() {
      // PostgreSQL does not use SQLite PRAGMA.
    },

    transaction(fn) {
      return (...args) => {
        if (transactionActive) {
          throw new Error(
            'Nested transactions are not supported'
          );
        }

        transactionActive = true;
        transactionQueue = [];

        try {
          const result = fn(...args);

          executeTransaction();

          return result;
        } catch (error) {
          console.error(
            '[PG TRANSACTION] ROLLBACK:',
            error.message
          );

          transactionQueue = [];

          throw error;
        } finally {
          transactionActive = false;
          transactionQueue = [];
        }
      };
    },

    close() {}
  };
}

module.exports = createDatabaseAdapter();

module.exports = createDatabaseAdapter();
