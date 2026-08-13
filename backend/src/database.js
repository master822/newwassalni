require('./env');
'use strict';

const { Pool } = require('pg');

if (!process.env.DATABASE_URL) {
  throw new Error('DATABASE_URL is not configured');
}

const databaseUrl = process.env.DATABASE_URL;

if (!databaseUrl) {
  throw new Error('DATABASE_URL is not configured');
}

let databaseHost = '';

try {
  databaseHost = new URL(databaseUrl).hostname || '';
} catch (error) {
  throw new Error('DATABASE_URL is invalid');
}

const isRenderDatabase =
  databaseHost.includes('render.com');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: false,
  max: 10,
  connectionTimeoutMillis: 10000,
  idleTimeoutMillis: 30000
});

function makeId(prefix) {
  return (
    prefix +
    '_' +
    Date.now().toString(36) +
    '_' +
    Math.random().toString(36).substring(2, 8)
  );
}

function makeReferralCode() {
  return (
    'WASALNI-' +
    Math.random()
      .toString(36)
      .substring(2, 8)
      .toUpperCase()
  );
}

async function initDatabase()
{
  const __wasalni_lock_client = await pool.connect();

  try {
    console.log('[DB] Waiting for Wasalni initialization lock...');

    await __wasalni_lock_client.query(
      'SELECT pg_advisory_lock(819472)'
    );

    console.log('[DB] Wasalni initialization lock acquired.');

    // Repair any sequence left behind by a previous interrupted
    // initialization before CREATE TABLE operations continue.
    await __wasalni_lock_client.query(`
      DO $$
      DECLARE
        seq_exists BOOLEAN;
        users_exists BOOLEAN;
      BEGIN
        SELECT EXISTS (
          SELECT 1
          FROM pg_class
          WHERE relname = 'users_id_seq'
            AND relkind = 'S'
        )
        INTO seq_exists;

        SELECT EXISTS (
          SELECT 1
          FROM pg_class
          WHERE relname = 'users'
            AND relkind = 'r'
        )
        INTO users_exists;

        IF seq_exists AND NOT users_exists THEN
          EXECUTE 'DROP SEQUENCE IF EXISTS users_id_seq CASCADE';
        END IF;
      END $$;
    `);


  await __wasalni_lock_client.query(`
    CREATE TABLE IF NOT EXISTS users (
      id BIGSERIAL PRIMARY KEY,
      telegram_id TEXT UNIQUE,
      name TEXT NOT NULL,
      email TEXT,
      phone TEXT,
      avatar_url TEXT,
      role TEXT NOT NULL DEFAULT 'passenger',
      rating REAL NOT NULL DEFAULT 0,
      rating_count INTEGER NOT NULL DEFAULT 0,
      ride_count INTEGER NOT NULL DEFAULT 0,
      wallet_points INTEGER NOT NULL DEFAULT 150,
      is_verified INTEGER NOT NULL DEFAULT 0,
      is_suspended INTEGER NOT NULL DEFAULT 0,
      suspend_reason TEXT,
      referral_code TEXT UNIQUE,
      referred_by BIGINT,
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );
    ALTER TABLE users
  ADD COLUMN IF NOT EXISTS telegram_id TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS name TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS email TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS phone TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS avatar_url TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'passenger';

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS rating REAL DEFAULT 0;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS rating_count INTEGER DEFAULT 0;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS ride_count INTEGER DEFAULT 0;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS wallet_points INTEGER DEFAULT 150;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS is_verified INTEGER DEFAULT 0;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS is_suspended INTEGER DEFAULT 0;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS suspend_reason TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS referral_code TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS referred_by BIGINT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS created_at BIGINT
    DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT;

    CREATE TABLE IF NOT EXISTS rides (
      id TEXT PRIMARY KEY,
      driver_id BIGINT NOT NULL REFERENCES users(id),
      start_city TEXT NOT NULL,
      end_city TEXT NOT NULL,
      departure_date TEXT NOT NULL,
      departure_time TEXT NOT NULL,
      duration TEXT DEFAULT '',
      price_per_seat REAL NOT NULL,
      price_currency TEXT NOT NULL DEFAULT 'POINTS',
      available_seats INTEGER NOT NULL,
      total_seats INTEGER NOT NULL,
      car_model TEXT DEFAULT '',
      car_color TEXT DEFAULT '',
      car_plate TEXT DEFAULT '',
      allows_luggage INTEGER NOT NULL DEFAULT 1,
      accept_cash INTEGER NOT NULL DEFAULT 1,
      accept_wallet INTEGER NOT NULL DEFAULT 1,
      women_only INTEGER NOT NULL DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'UPCOMING',
      meeting_point TEXT DEFAULT '',
      notes TEXT DEFAULT '',
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );

    CREATE TABLE IF NOT EXISTS bookings (
      id TEXT PRIMARY KEY,
      ride_id TEXT NOT NULL REFERENCES rides(id),
      passenger_id BIGINT NOT NULL REFERENCES users(id),
      seats_booked INTEGER NOT NULL,
      total_points REAL NOT NULL,
      status TEXT NOT NULL DEFAULT 'UPCOMING',
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );

    CREATE TABLE IF NOT EXISTS wallet_transactions (
      id TEXT PRIMARY KEY,
      user_id BIGINT NOT NULL REFERENCES users(id),
      type TEXT NOT NULL,
      points INTEGER NOT NULL,
      amount_usd REAL NOT NULL DEFAULT 0,
      description TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'COMPLETED',
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );

    CREATE TABLE IF NOT EXISTS topup_requests (
      id TEXT PRIMARY KEY,
      user_id BIGINT NOT NULL REFERENCES users(id),
      package_points INTEGER NOT NULL,
      package_price_usd REAL NOT NULL DEFAULT 0,
      receipt_image_path TEXT DEFAULT '',
      status TEXT NOT NULL DEFAULT 'PENDING',
      rejection_reason TEXT,
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );

    CREATE TABLE IF NOT EXISTS requested_trips (
      id TEXT PRIMARY KEY,
      user_id BIGINT NOT NULL REFERENCES users(id),
      start_city TEXT NOT NULL,
      end_city TEXT NOT NULL,
      departure_date TEXT NOT NULL,
      departure_time TEXT NOT NULL,
      men_count INTEGER NOT NULL DEFAULT 1,
      women_count INTEGER NOT NULL DEFAULT 0,
      children_count INTEGER NOT NULL DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'OPEN',
      accepted_by_driver_id BIGINT,
      accepted_by_driver_name TEXT,
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );

    CREATE TABLE IF NOT EXISTS chat_messages (
      id TEXT PRIMARY KEY,
      ride_id TEXT NOT NULL REFERENCES rides(id),
      sender_id BIGINT NOT NULL REFERENCES users(id),
      receiver_id BIGINT NOT NULL REFERENCES users(id),
      message_text TEXT NOT NULL,
      image_uri TEXT,
      is_location INTEGER NOT NULL DEFAULT 0,
      latitude REAL,
      longitude REAL,
      is_payment_reminder INTEGER NOT NULL DEFAULT 0,
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );

    CREATE TABLE IF NOT EXISTS notifications (
      id TEXT PRIMARY KEY,
      user_id BIGINT NOT NULL REFERENCES users(id),
      title TEXT NOT NULL,
      message TEXT NOT NULL,
      type TEXT NOT NULL,
      is_read INTEGER NOT NULL DEFAULT 0,
      created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT
    );

    CREATE INDEX IF NOT EXISTS idx_rides_search
      ON rides(start_city, end_city, departure_date);

    CREATE INDEX IF NOT EXISTS idx_bookings_passenger
      ON bookings(passenger_id);

    CREATE INDEX IF NOT EXISTS idx_rides_driver
      ON rides(driver_id);
  `);

  console.log('PostgreSQL database initialized');

  } finally {
    try {
      await __wasalni_lock_client.query(
        'SELECT pg_advisory_unlock(819472)'
      );
    } catch (_) {}

    __wasalni_lock_client.release();

    console.log('[DB] Wasalni initialization lock released.');
  }
}

async function getUserById(id) {
  const result = await pool.query(
    'SELECT * FROM users WHERE id = $1',
    [id]
  );

  return result.rows[0] || null;
}

async function getUserByTelegramId(telegramId) {
  const result = await pool.query(
    'SELECT * FROM users WHERE telegram_id = $1',
    [String(telegramId)]
  );

  return result.rows[0] || null;
}

async function createUser(data) {
  let referralCode;

  do {
    referralCode = makeReferralCode();

    const check = await pool.query(
      'SELECT id FROM users WHERE referral_code = $1',
      [referralCode]
    );

    if (check.rows.length === 0) break;
  } while (true);

  const result = await pool.query(
    `
      INSERT INTO users
      (
        telegram_id,
        name,
        email,
        phone,
        role,
        referral_code
      )
      VALUES ($1, $2, $3, $4, $5, $6)
      RETURNING *
    `,
    [
      data.telegram_id
        ? String(data.telegram_id)
        : null,
      data.name || 'مستخدم وصلني',
      data.email || null,
      data.phone || null,
      data.role || 'passenger',
      referralCode
    ]
  );

  return result.rows[0];
}

async function getOrCreateTelegramUser(data) {
  let user = null;

  if (data.telegram_id) {
    user = await getUserByTelegramId(data.telegram_id);
  }

  if (user) {
    await pool.query(
      `
        UPDATE users
        SET name = $1,
            telegram_id = $2
        WHERE id = $3
      `,
      [
        data.name || user.name,
        String(data.telegram_id),
        user.id
      ]
    );

    return getUserById(user.id);
  }

  user = await createUser(data);

  if (
    data.referral_code &&
    data.referral_code !== user.referral_code
  ) {
    const inviterResult = await pool.query(
      `
        SELECT *
        FROM users
        WHERE referral_code = $1
      `,
      [data.referral_code]
    );

    const inviter = inviterResult.rows[0];

    if (inviter && inviter.id !== user.id) {
      await pool.query(
        `
          UPDATE users
          SET referred_by = $1
          WHERE id = $2
        `,
        [inviter.id, user.id]
      );

      const points = Number(
        process.env.POINTS_PER_REFERRAL || 100
      );

      await addPoints(
        inviter.id,
        points,
        'مكافأة دعوة مستخدم جديد'
      );

      await addPoints(
        user.id,
        points,
        'مكافأة التسجيل بالإحالة'
      );
    }
  }

  return getUserById(user.id);
}

async function updateUser(id, fields) {
  const allowed = [
    'name',
    'email',
    'phone',
    'avatar_url',
    'role',
    'is_verified',
    'is_suspended',
    'suspend_reason'
  ];

  const keys = Object.keys(fields).filter(
    key => allowed.includes(key)
  );

  if (!keys.length) {
    return getUserById(id);
  }

  const values = keys.map(key => fields[key]);
  const setClause = keys
    .map((key, index) => `${key} = $${index + 1}`)
    .join(', ');

  await pool.query(
    `
      UPDATE users
      SET ${setClause}
      WHERE id = $${keys.length + 1}
    `,
    [...values, id]
  );

  return getUserById(id);
}

async function addPoints(userId, points, description) {
  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    await client.query(
      `
        UPDATE users
        SET wallet_points = wallet_points + $1
        WHERE id = $2
      `,
      [points, userId]
    );

    await client.query(
      `
        INSERT INTO wallet_transactions
        (
          id,
          user_id,
          type,
          points,
          description
        )
        VALUES ($1, $2, 'TOP_UP', $3, $4)
      `,
      [
        makeId('tx'),
        userId,
        points,
        description
      ]
    );

    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }

  return getUserById(userId);
}

async function deductPoints(userId, points, description) {
  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    const userResult = await client.query(
      `
        SELECT *
        FROM users
        WHERE id = $1
        FOR UPDATE
      `,
      [userId]
    );

    const user = userResult.rows[0];

    if (!user || Number(user.wallet_points) < Number(points)) {
      await client.query('ROLLBACK');
      return false;
    }

    await client.query(
      `
        UPDATE users
        SET wallet_points = wallet_points - $1
        WHERE id = $2
      `,
      [points, userId]
    );

    await client.query(
      `
        INSERT INTO wallet_transactions
        (
          id,
          user_id,
          type,
          points,
          description
        )
        VALUES ($1, $2, 'DEDUCTION', $3, $4)
      `,
      [
        makeId('tx'),
        userId,
        points,
        description
      ]
    );

    await client.query('COMMIT');

    return true;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

async function createRide(data) {
  const id = makeId('ride');

  await pool.query(
    `
      INSERT INTO rides
      (
        id,
        driver_id,
        start_city,
        end_city,
        departure_date,
        departure_time,
        duration,
        price_per_seat,
        available_seats,
        total_seats,
        car_model,
        car_color,
        car_plate,
        allows_luggage,
        accept_cash,
        accept_wallet,
        women_only,
        meeting_point,
        notes
      )
      VALUES
      (
        $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,
        $11,$12,$13,$14,$15,$16,$17,$18,$19
      )
    `,
    [
      id,
      data.driver_id,
      data.start_city,
      data.end_city,
      data.departure_date,
      data.departure_time,
      data.duration || '',
      data.price_per_seat,
      data.total_seats,
      data.total_seats,
      data.car_model || '',
      data.car_color || '',
      data.car_plate || '',
      data.allows_luggage ? 1 : 0,
      data.accept_cash ? 1 : 0,
      data.accept_wallet ? 1 : 0,
      data.women_only ? 1 : 0,
      data.meeting_point || '',
      data.notes || ''
    ]
  );

  return getRideById(id);
}

async function getRideById(id) {
  const result = await pool.query(
    `
      SELECT
        rides.*,
        users.name AS driver_name,
        users.telegram_id AS driver_telegram_id,
        users.rating AS driver_rating,
        users.rating_count AS driver_rating_count,
        users.is_verified AS driver_verified
      FROM rides
      JOIN users ON users.id = rides.driver_id
      WHERE rides.id = $1
    `,
    [id]
  );

  return result.rows[0] || null;
}

async function searchRides(startCity, endCity, date) {
  let sql = `
    SELECT
      rides.*,
      users.name AS driver_name,
      users.telegram_id AS driver_telegram_id,
      users.rating AS driver_rating,
      users.rating_count AS driver_rating_count,
      users.is_verified AS driver_verified
    FROM rides
    JOIN users ON users.id = rides.driver_id
    WHERE rides.status = 'UPCOMING'
      AND rides.available_seats > 0
  `;

  const params = [];

  if (startCity) {
    params.push(`%${startCity}%`);
    sql += `
      AND LOWER(rides.start_city)
      LIKE LOWER($${params.length})
    `;
  }

  if (endCity) {
    params.push(`%${endCity}%`);
    sql += `
      AND LOWER(rides.end_city)
      LIKE LOWER($${params.length})
    `;
  }

  if (date) {
    params.push(date);
    sql += `
      AND rides.departure_date = $${params.length}
    `;
  }

  sql += `
    ORDER BY
      rides.departure_date ASC,
      rides.departure_time ASC
    LIMIT 50
  `;

  const result = await pool.query(sql, params);

  return result.rows;
}

async function getRidesByDriver(driverId) {
  const result = await pool.query(
    `
      SELECT *
      FROM rides
      WHERE driver_id = $1
      ORDER BY created_at DESC
    `,
    [driverId]
  );

  return result.rows;
}

async function createBooking(
  rideId,
  passengerId,
  seats
) {
  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    const rideResult = await client.query(
      `
        SELECT *
        FROM rides
        WHERE id = $1
        FOR UPDATE
      `,
      [rideId]
    );

    const ride = rideResult.rows[0];

    if (!ride) {
      throw new Error('RIDE_NOT_FOUND');
    }

    if (Number(ride.available_seats) < Number(seats)) {
      throw new Error('NOT_ENOUGH_SEATS');
    }

    if (Number(ride.driver_id) === Number(passengerId)) {
      throw new Error('SELF_BOOKING');
    }

    const existing = await client.query(
      `
        SELECT *
        FROM bookings
        WHERE ride_id = $1
          AND passenger_id = $2
          AND status = 'UPCOMING'
      `,
      [rideId, passengerId]
    );

    if (existing.rows.length) {
      throw new Error('ALREADY_BOOKED');
    }

    const total =
      Number(ride.price_per_seat) *
      Number(seats);

    const userResult = await client.query(
      `
        SELECT *
        FROM users
        WHERE id = $1
        FOR UPDATE
      `,
      [passengerId]
    );

    const user = userResult.rows[0];

    if (
      !user ||
      Number(user.wallet_points) < total
    ) {
      throw new Error('INSUFFICIENT_BALANCE');
    }

    await client.query(
      `
        UPDATE users
        SET wallet_points = wallet_points - $1
        WHERE id = $2
      `,
      [total, passengerId]
    );

    await client.query(
      `
        INSERT INTO wallet_transactions
        (
          id,
          user_id,
          type,
          points,
          description
        )
        VALUES
        ($1, $2, 'DEDUCTION', $3, $4)
      `,
      [
        makeId('tx'),
        passengerId,
        total,
        `حجز الرحلة ${rideId}`
      ]
    );

    await client.query(
      `
        UPDATE rides
        SET available_seats =
          available_seats - $1
        WHERE id = $2
      `,
      [seats, rideId]
    );

    const bookingId = makeId('booking');

    const bookingResult = await client.query(
      `
        INSERT INTO bookings
        (
          id,
          ride_id,
          passenger_id,
          seats_booked,
          total_points
        )
        VALUES ($1, $2, $3, $4, $5)
        RETURNING *
      `,
      [
        bookingId,
        rideId,
        passengerId,
        seats,
        total
      ]
    );

    await client.query('COMMIT');

    return bookingResult.rows[0];
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

async function getBookingsByPassenger(passengerId) {
  const result = await pool.query(
    `
      SELECT
        bookings.*,
        rides.start_city,
        rides.end_city,
        rides.departure_date,
        rides.departure_time,
        rides.price_per_seat,
        rides.driver_id,
        users.name AS driver_name
      FROM bookings
      JOIN rides ON rides.id = bookings.ride_id
      JOIN users ON users.id = rides.driver_id
      WHERE bookings.passenger_id = $1
      ORDER BY bookings.created_at DESC
    `,
    [passengerId]
  );

  return result.rows;
}

async function getWalletTransactions(userId) {
  const result = await pool.query(
    `
      SELECT *
      FROM wallet_transactions
      WHERE user_id = $1
      ORDER BY created_at DESC
      LIMIT 50
    `,
    [userId]
  );

  return result.rows;
}

async function createTopupRequest(
  userId,
  points,
  amountUsd = 0
) {
  const id = makeId('topup');

  const result = await pool.query(
    `
      INSERT INTO topup_requests
      (
        id,
        user_id,
        package_points,
        package_price_usd
      )
      VALUES ($1, $2, $3, $4)
      RETURNING *
    `,
    [
      id,
      userId,
      points,
      amountUsd
    ]
  );

  return result.rows[0];
}

async function getRequestedTrips() {
  const result = await pool.query(
    `
      SELECT
        requested_trips.*,
        users.name AS user_name,
        users.phone AS user_phone
      FROM requested_trips
      JOIN users
        ON users.id = requested_trips.user_id
      WHERE requested_trips.status = 'OPEN'
      ORDER BY requested_trips.created_at DESC
    `
  );

  return result.rows;
}

async function createRequestedTrip(data) {
  const id = makeId('request');

  const result = await pool.query(
    `
      INSERT INTO requested_trips
      (
        id,
        user_id,
        start_city,
        end_city,
        departure_date,
        departure_time,
        men_count,
        women_count,
        children_count
      )
      VALUES
      ($1,$2,$3,$4,$5,$6,$7,$8,$9)
      RETURNING *
    `,
    [
      id,
      data.user_id,
      data.start_city,
      data.end_city,
      data.departure_date,
      data.departure_time,
      data.men_count || 1,
      data.women_count || 0,
      data.children_count || 0
    ]
  );

  return result.rows[0];
}

async function createChatMessage(data) {
  const id = makeId('msg');

  const result = await pool.query(
    `
      INSERT INTO chat_messages
      (
        id,
        ride_id,
        sender_id,
        receiver_id,
        message_text,
        image_uri,
        is_location,
        latitude,
        longitude,
        is_payment_reminder
      )
      VALUES
      ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10)
      RETURNING *
    `,
    [
      id,
      data.ride_id,
      data.sender_id,
      data.receiver_id,
      data.message_text,
      data.image_uri || null,
      data.is_location ? 1 : 0,
      data.latitude || null,
      data.longitude || null,
      data.is_payment_reminder ? 1 : 0
    ]
  );

  return result.rows[0];
}

async function getChatMessages(rideId) {
  const result = await pool.query(
    `
      SELECT *
      FROM chat_messages
      WHERE ride_id = $1
      ORDER BY created_at ASC
    `,
    [rideId]
  );

  return result.rows;
}

async function getStats() {
  const [
    users,
    rides,
    bookings,
    points,
    pendingTopups
  ] = await Promise.all([
    pool.query('SELECT COUNT(*)::INTEGER AS count FROM users'),
    pool.query('SELECT COUNT(*)::INTEGER AS count FROM rides'),
    pool.query('SELECT COUNT(*)::INTEGER AS count FROM bookings'),
    pool.query(
      'SELECT COALESCE(SUM(wallet_points),0)::INTEGER AS points FROM users'
    ),
    pool.query(
      `
        SELECT COUNT(*)::INTEGER AS count
        FROM topup_requests
        WHERE status = 'PENDING'
      `
    )
  ]);

  return {
    users: users.rows[0].count,
    rides: rides.rows[0].count,
    bookings: bookings.rows[0].count,
    points: points.rows[0].points,
    pendingTopups: pendingTopups.rows[0].count
  };
}

async function getDatabase() {
  return pool;
}

initDatabase().catch(error => {
  console.error('PostgreSQL initialization failed:', error);
  process.exit(1);
});


// ------------------------------------------------------------
// Wasalni single-flight database initialization.
//
// If several parts of the application call initDatabase()
// simultaneously, they all wait for the SAME promise.
// ------------------------------------------------------------

const __wasalniOriginalInitDatabase = initDatabase;

async function initDatabaseSingleFlight() {
  if (global.__wasalniInitPromise) {
    console.log('[DB] Waiting for existing initialization...');
    return global.__wasalniInitPromise;
  }

  global.__wasalniInitPromise =
    __wasalniOriginalInitDatabase()
      .finally(() => {
        global.__wasalniInitPromise = null;
      });

  return global.__wasalniInitPromise;
}

initDatabase = initDatabaseSingleFlight;

module.exports = {
  db: pool,
  pool,
  getDatabase,
  initDatabase,
  getUserById,
  getUserByTelegramId,
  createUser,
  getOrCreateTelegramUser,
  updateUser,
  addPoints,
  deductPoints,
  createRide,
  getRideById,
  searchRides,
  createBooking,
  getBookingsByPassenger,
  getRidesByDriver,
  getWalletTransactions,
  createTopupRequest,
  getRequestedTrips,
  createRequestedTrip,
  createChatMessage,
  getChatMessages,
  getStats
};
