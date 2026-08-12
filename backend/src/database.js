const Database = require("better-sqlite3");
const fs = require("fs");
const path = require("path");

const databaseFile =
  process.env.DATABASE_PATH || "./data/wasalni.db";

const absolutePath = path.isAbsolute(databaseFile)
  ? databaseFile
  : path.resolve(__dirname, "..", databaseFile);

fs.mkdirSync(
  path.dirname(absolutePath),
  { recursive: true }
);

const db = new Database(absolutePath);

db.pragma("journal_mode = WAL");
db.pragma("foreign_keys = ON");

db.exec(`
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
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
    referred_by TEXT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE TABLE IF NOT EXISTS rides (
    id TEXT PRIMARY KEY,
    driver_id INTEGER NOT NULL,
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
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY(driver_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id TEXT PRIMARY KEY,
    ride_id TEXT NOT NULL,
    passenger_id INTEGER NOT NULL,
    seats_booked INTEGER NOT NULL,
    total_points REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'UPCOMING',
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY(ride_id) REFERENCES rides(id),
    FOREIGN KEY(passenger_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    type TEXT NOT NULL,
    points INTEGER NOT NULL,
    amount_usd REAL NOT NULL DEFAULT 0,
    description TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'COMPLETED',
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS topup_requests (
    id TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    package_points INTEGER NOT NULL,
    package_price_usd REAL NOT NULL DEFAULT 0,
    receipt_image_path TEXT DEFAULT '',
    status TEXT NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS requested_trips (
    id TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    start_city TEXT NOT NULL,
    end_city TEXT NOT NULL,
    departure_date TEXT NOT NULL,
    departure_time TEXT NOT NULL,
    men_count INTEGER NOT NULL DEFAULT 1,
    women_count INTEGER NOT NULL DEFAULT 0,
    children_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'OPEN',
    accepted_by_driver_id INTEGER,
    accepted_by_driver_name TEXT,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id TEXT PRIMARY KEY,
    ride_id TEXT NOT NULL,
    sender_id INTEGER NOT NULL,
    receiver_id INTEGER NOT NULL,
    message_text TEXT NOT NULL,
    image_uri TEXT,
    is_location INTEGER NOT NULL DEFAULT 0,
    latitude REAL,
    longitude REAL,
    is_payment_reminder INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE TABLE IF NOT EXISTS notifications (
    id TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL,
    is_read INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT (unixepoch()),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_rides_search
ON rides(start_city, end_city, departure_date);

CREATE INDEX IF NOT EXISTS idx_bookings_passenger
ON bookings(passenger_id);

CREATE INDEX IF NOT EXISTS idx_rides_driver
ON rides(driver_id);
`);

function makeId(prefix) {
  return (
    prefix +
    "_" +
    Date.now().toString(36) +
    "_" +
    Math.random().toString(36).substring(2, 8)
  );
}

function makeReferralCode() {
  return (
    "WASALNI-" +
    Math.random()
      .toString(36)
      .substring(2, 8)
      .toUpperCase()
  );
}

function getUserById(id) {
  return db
    .prepare(
      "SELECT * FROM users WHERE id = ?"
    )
    .get(id);
}

function getUserByTelegramId(telegramId) {
  return db
    .prepare(
      "SELECT * FROM users WHERE telegram_id = ?"
    )
    .get(String(telegramId));
}

function createUser(data) {
  let referralCode;

  do {
    referralCode = makeReferralCode();
  } while (
    db
      .prepare(
        "SELECT id FROM users WHERE referral_code = ?"
      )
      .get(referralCode)
  );

  const result = db
    .prepare(`
      INSERT INTO users
      (
        telegram_id,
        name,
        email,
        phone,
        role,
        referral_code
      )
      VALUES (?, ?, ?, ?, ?, ?)
    `)
    .run(
      data.telegram_id
        ? String(data.telegram_id)
        : null,
      data.name || "مستخدم وصلني",
      data.email || null,
      data.phone || null,
      data.role || "passenger",
      referralCode
    );

  return getUserById(
    result.lastInsertRowid
  );
}

function getOrCreateTelegramUser(data) {
  let user =
    getUserByTelegramId(data.telegram_id);

  if (user) {
    db.prepare(`
      UPDATE users
      SET name = ?,
          telegram_id = ?
      WHERE id = ?
    `).run(
      data.name || user.name,
      String(data.telegram_id),
      user.id
    );

    return getUserById(user.id);
  }

  user = createUser(data);

  if (
    data.referral_code &&
    data.referral_code !== user.referral_code
  ) {
    const inviter = db
      .prepare(`
        SELECT *
        FROM users
        WHERE referral_code = ?
      `)
      .get(data.referral_code);

    if (
      inviter &&
      inviter.id !== user.id
    ) {
      db.prepare(`
        UPDATE users
        SET referred_by = ?
        WHERE id = ?
      `).run(
        inviter.id,
        user.id
      );

      const points = Number(
        process.env.POINTS_PER_REFERRAL || 100
      );

      addPoints(
        inviter.id,
        points,
        "مكافأة دعوة مستخدم جديد"
      );

      addPoints(
        user.id,
        points,
        "مكافأة التسجيل بالإحالة"
      );
    }
  }

  return user;
}

function updateUser(id, fields) {
  const allowed = [
    "name",
    "email",
    "phone",
    "avatar_url",
    "role",
    "is_verified",
    "is_suspended",
    "suspend_reason"
  ];

  const keys =
    Object.keys(fields).filter(
      key => allowed.includes(key)
    );

  if (!keys.length) {
    return getUserById(id);
  }

  const sql = `
    UPDATE users
    SET ${keys
      .map(key => `${key} = ?`)
      .join(", ")}
    WHERE id = ?
  `;

  db.prepare(sql).run(
    ...keys.map(
      key => fields[key]
    ),
    id
  );

  return getUserById(id);
}

function addPoints(
  userId,
  points,
  description
) {
  const transaction =
    db.transaction(() => {

      db.prepare(`
        UPDATE users
        SET wallet_points =
          wallet_points + ?
        WHERE id = ?
      `).run(
        points,
        userId
      );

      db.prepare(`
        INSERT INTO wallet_transactions
        (
          id,
          user_id,
          type,
          points,
          description
        )
        VALUES (?, ?, 'TOP_UP', ?, ?)
      `).run(
        makeId("tx"),
        userId,
        points,
        description
      );
    });

  transaction();

  return getUserById(userId);
}

function deductPoints(
  userId,
  points,
  description
) {
  const user =
    getUserById(userId);

  if (
    !user ||
    user.wallet_points < points
  ) {
    return false;
  }

  const transaction =
    db.transaction(() => {

      db.prepare(`
        UPDATE users
        SET wallet_points =
          wallet_points - ?
        WHERE id = ?
      `).run(
        points,
        userId
      );

      db.prepare(`
        INSERT INTO wallet_transactions
        (
          id,
          user_id,
          type,
          points,
          description
        )
        VALUES (?, ?, 'DEDUCTION', ?, ?)
      `).run(
        makeId("tx"),
        userId,
        points,
        description
      );
    });

  transaction();

  return true;
}

function createRide(data) {
  const id = makeId("ride");

  db.prepare(`
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
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    id,
    data.driver_id,
    data.start_city,
    data.end_city,
    data.departure_date,
    data.departure_time,
    data.duration || "",
    data.price_per_seat,
    data.total_seats,
    data.total_seats,
    data.car_model || "",
    data.car_color || "",
    data.car_plate || "",
    data.allows_luggage ? 1 : 0,
    data.accept_cash ? 1 : 0,
    data.accept_wallet ? 1 : 0,
    data.women_only ? 1 : 0,
    data.meeting_point || "",
    data.notes || ""
  );

  return getRideById(id);
}

function getRideById(id) {
  return db
    .prepare(`
      SELECT
        rides.*,
        users.name AS driver_name,
        users.telegram_id AS driver_telegram_id,
        users.rating AS driver_rating,
        users.rating_count AS driver_rating_count,
        users.is_verified AS driver_verified
      FROM rides
      JOIN users
        ON users.id = rides.driver_id
      WHERE rides.id = ?
    `)
    .get(id);
}

function searchRides(
  startCity,
  endCity,
  date
) {
  let sql = `
    SELECT
      rides.*,
      users.name AS driver_name,
      users.telegram_id AS driver_telegram_id,
      users.rating AS driver_rating,
      users.rating_count AS driver_rating_count,
      users.is_verified AS driver_verified
    FROM rides
    JOIN users
      ON users.id = rides.driver_id
    WHERE rides.status = 'UPCOMING'
      AND rides.available_seats > 0
  `;

  const params = [];

  if (startCity) {
    sql += `
      AND LOWER(rides.start_city)
      LIKE LOWER(?)
    `;

    params.push(`%${startCity}%`);
  }

  if (endCity) {
    sql += `
      AND LOWER(rides.end_city)
      LIKE LOWER(?)
    `;

    params.push(`%${endCity}%`);
  }

  if (date) {
    sql += `
      AND rides.departure_date = ?
    `;

    params.push(date);
  }

  sql += `
    ORDER BY
      rides.departure_date ASC,
      rides.departure_time ASC
    LIMIT 50
  `;

  return db
    .prepare(sql)
    .all(...params);
}

function createBooking(
  rideId,
  passengerId,
  seats
) {
  return db.transaction(() => {

    const ride =
      getRideById(rideId);

    if (!ride) {
      throw new Error(
        "RIDE_NOT_FOUND"
      );
    }

    if (
      ride.available_seats < seats
    ) {
      throw new Error(
        "NOT_ENOUGH_SEATS"
      );
    }

    if (
      ride.driver_id === passengerId
    ) {
      throw new Error(
        "SELF_BOOKING"
      );
    }

    const existing =
      db.prepare(`
        SELECT *
        FROM bookings
        WHERE ride_id = ?
          AND passenger_id = ?
          AND status = 'UPCOMING'
      `).get(
        rideId,
        passengerId
      );

    if (existing) {
      throw new Error(
        "ALREADY_BOOKED"
      );
    }

    const total =
      Number(ride.price_per_seat) *
      seats;

    if (
      !deductPoints(
        passengerId,
        total,
        `حجز الرحلة ${rideId}`
      )
    ) {
      throw new Error(
        "INSUFFICIENT_BALANCE"
      );
    }

    db.prepare(`
      UPDATE rides
      SET available_seats =
        available_seats - ?
      WHERE id = ?
    `).run(
      seats,
      rideId
    );

    const bookingId =
      makeId("booking");

    db.prepare(`
      INSERT INTO bookings
      (
        id,
        ride_id,
        passenger_id,
        seats_booked,
        total_points
      )
      VALUES (?, ?, ?, ?, ?)
    `).run(
      bookingId,
      rideId,
      passengerId,
      seats,
      total
    );

    return db
      .prepare(`
        SELECT *
        FROM bookings
        WHERE id = ?
      `)
      .get(bookingId);
  })();
}

function getBookingsByPassenger(
  passengerId
) {
  return db
    .prepare(`
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
      JOIN rides
        ON rides.id = bookings.ride_id
      JOIN users
        ON users.id = rides.driver_id
      WHERE bookings.passenger_id = ?
      ORDER BY bookings.created_at DESC
    `)
    .all(passengerId);
}

function getRidesByDriver(
  driverId
) {
  return db
    .prepare(`
      SELECT *
      FROM rides
      WHERE driver_id = ?
      ORDER BY created_at DESC
    `)
    .all(driverId);
}

function getWalletTransactions(
  userId
) {
  return db
    .prepare(`
      SELECT *
      FROM wallet_transactions
      WHERE user_id = ?
      ORDER BY created_at DESC
      LIMIT 50
    `)
    .all(userId);
}

function createTopupRequest(
  userId,
  points,
  amountUsd = 0
) {
  const id = makeId("topup");

  db.prepare(`
    INSERT INTO topup_requests
    (
      id,
      user_id,
      package_points,
      package_price_usd
    )
    VALUES (?, ?, ?, ?)
  `).run(
    id,
    userId,
    points,
    amountUsd
  );

  return db
    .prepare(`
      SELECT *
      FROM topup_requests
      WHERE id = ?
    `)
    .get(id);
}

function getPendingTopups() {
  return db
    .prepare(`
      SELECT
        topup_requests.*,
        users.name,
        users.telegram_id
      FROM topup_requests
      JOIN users
        ON users.id = topup_requests.user_id
      WHERE topup_requests.status = 'PENDING'
      ORDER BY topup_requests.created_at ASC
    `)
    .all();
}

function approveTopup(id) {
  return db.transaction(() => {

    const request =
      db.prepare(`
        SELECT *
        FROM topup_requests
        WHERE id = ?
          AND status = 'PENDING'
      `).get(id);

    if (!request) {
      return null;
    }

    db.prepare(`
      UPDATE topup_requests
      SET status = 'APPROVED'
      WHERE id = ?
    `).run(id);

    addPoints(
      request.user_id,
      request.package_points,
      `تمت الموافقة على طلب الشحن ${id}`
    );

    return request;
  })();
}

function rejectTopup(
  id,
  reason
) {
  return db.prepare(`
    UPDATE topup_requests
    SET status = 'REJECTED',
        rejection_reason = ?
    WHERE id = ?
      AND status = 'PENDING'
  `).run(
    reason || "",
    id
  ).changes > 0;
}

function createRequestedTrip(data) {
  const id =
    makeId("request");

  db.prepare(`
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
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    id,
    data.user_id,
    data.start_city,
    data.end_city,
    data.departure_date,
    data.departure_time,
    data.men_count || 1,
    data.women_count || 0,
    data.children_count || 0
  );

  return db
    .prepare(`
      SELECT *
      FROM requested_trips
      WHERE id = ?
    `).get(id);
}

function getRequestedTrips() {
  return db
    .prepare(`
      SELECT
        requested_trips.*,
        users.name AS user_name,
        users.phone AS user_phone
      FROM requested_trips
      JOIN users
        ON users.id = requested_trips.user_id
      WHERE requested_trips.status = 'OPEN'
      ORDER BY requested_trips.created_at DESC
    `)
    .all();
}

function createChatMessage(data) {
  const id =
    makeId("msg");

  db.prepare(`
    INSERT INTO chat_messages
    (
      id,
      ride_id,
      sender_id,
      receiver_id,
      message_text
    )
    VALUES (?, ?, ?, ?, ?)
  `).run(
    id,
    data.ride_id,
    data.sender_id,
    data.receiver_id,
    data.message_text
  );

  return db
    .prepare(`
      SELECT *
      FROM chat_messages
      WHERE id = ?
    `).get(id);
}

function getChatMessages(rideId) {
  return db
    .prepare(`
      SELECT *
      FROM chat_messages
      WHERE ride_id = ?
      ORDER BY created_at ASC
    `)
    .all(rideId);
}

function getStats() {
  return {
    users: db
      .prepare(
        "SELECT COUNT(*) count FROM users"
      )
      .get().count,

    rides: db
      .prepare(
        "SELECT COUNT(*) count FROM rides"
      )
      .get().count,

    bookings: db
      .prepare(
        "SELECT COUNT(*) count FROM bookings"
      )
      .get().count,

    points: db
      .prepare(
        "SELECT COALESCE(SUM(wallet_points),0) points FROM users"
      )
      .get().points,

    pendingTopups: db
      .prepare(`
        SELECT COUNT(*) count
        FROM topup_requests
        WHERE status = 'PENDING'
      `)
      .get().count
  };
}

module.exports = {
  db,
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
  getPendingTopups,
  approveTopup,
  rejectTopup,
  createRequestedTrip,
  getRequestedTrips,
  createChatMessage,
  getChatMessages,
  getStats
};
