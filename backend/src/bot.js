'use strict';

require('dotenv').config();

const db = require('./pg-compat');

/* =========================================================
   CONFIG
========================================================= */

const TOKEN =
  process.env.TELEGRAM_BOT_TOKEN ||
  process.env.BOT_TOKEN ||
  process.env.TELEGRAM_TOKEN;
console.log('[BOT] Token loaded:', !!TOKEN, 'length:', TOKEN?.length || 0);
const ADMIN_IDS = new Set(
  String(
    process.env.ADMIN_TELEGRAM_IDS ||
    process.env.ADMIN_TELEGRAM_ID ||
    ''
  )
    .split(',')
    .map(x => x.trim())
    .filter(Boolean)
);

if (!TOKEN) {
  console.error('❌ BOT TOKEN missing');
  process.exit(1);
}




const API = `https://api.telegram.org/bot${TOKEN}`;

const sessions = new Map();

/* =========================================================
   HELPERS
========================================================= */

const esc = value =>
  String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

const isAdmin = id => ADMIN_IDS.has(String(id));

async function tg(method, body = {}) {
  const response = await fetch(`${API}/${method}`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json'
    },
    body: JSON.stringify(body)
  });

  const json = await response.json();

  if (!json.ok) {
    throw new Error(json.description || method);
  }

  return json.result;
}

async function send(chatId, text, replyMarkup = null) {
  return tg('sendMessage', {
    chat_id: chatId,
    text,
    parse_mode: 'HTML',
    disable_web_page_preview: true,
    ...(replyMarkup ? { reply_markup: replyMarkup } : {})
  });
}

async function edit(chatId, messageId, text, replyMarkup = null) {
  return tg('editMessageText', {
    chat_id: chatId,
    message_id: messageId,
    text,
    parse_mode: 'HTML',
    disable_web_page_preview: true,
    ...(replyMarkup ? { reply_markup: replyMarkup } : {})
  });
}

function kb(rows) {
  return {
    keyboard: rows,
    resize_keyboard: true,
    is_persistent: true
  };
}

function ikb(rows) {
  return {
    inline_keyboard: rows
  };
}

/* =========================================================
   DATABASE HELPERS
========================================================= */

function tableExists(name) {
  const row = db.prepare(`
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
      AND table_name = ?
    LIMIT 1
  `).get(name);

  return !!row;
}

function columns(table) {
  return db.prepare(`
    SELECT column_name AS name
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = ?
    ORDER BY ordinal_position
  `).all(table).map(row => row.name);
}

function hasColumn(table, column) {
  return columns(table).some(
    c => c.toLowerCase() === column.toLowerCase()
  );
}

function col(table, names) {
  const cols = columns(table);

  for (const name of names) {
    const found = cols.find(
      c => c.toLowerCase() === name.toLowerCase()
    );

    if (found) return found;
  }

  return null;
}

function count(table) {
  if (!tableExists(table)) return 0;

  const row = db
    .prepare(`SELECT COUNT(*) AS c FROM "${table}"`)
    .get();

  return Number(row?.c || 0);
}

/* =========================================================
   USER
========================================================= */

function userByTelegram(telegramId) {
  if (!tableExists('users')) return null;

  return db
    .prepare(`
      SELECT *
      FROM users
      WHERE telegram_id = ?
      LIMIT 1
    `)
    .get(String(telegramId));
}

function userById(id) {
  if (!tableExists('users')) return null;

  return db
    .prepare(`
      SELECT *
      FROM users
      WHERE id = ?
      LIMIT 1
    `)
    .get(id);
}

function ensureUser(from) {
  let user = userByTelegram(from.id);

  if (user) return user;

  const name =
    [from.first_name, from.last_name]
      .filter(Boolean)
      .join(' ') ||
    from.username ||
    `Telegram ${from.id}`;

  const referralCode =
    `WASALNI${String(from.id).slice(-8)}`;

  try {
    db.prepare(`
      INSERT INTO users
      (
        telegram_id,
        name,
        role,
        referral_code,
        wallet_points
      )
      VALUES (?, ?, ?, ?, ?)
    `).run(
      String(from.id),
      name,
      'passenger',
      referralCode,
      150
    );
  } catch (error) {
    console.error(
      'ensureUser insert:',
      error.message
    );
  }

  return userByTelegram(from.id);
}

function points(user) {
  return Number(
    user?.wallet_points ??
    user?.points ??
    user?.balance ??
    0
  );
}

/* =========================================================
   MAIN KEYBOARD
========================================================= */

function mainKeyboard(id) {
  const rows = [
    ['🔎 البحث عن رحلة', '📦 طلب رحلة'],
    ['🚗 نشر رحلة', '📋 رحلاتي'],
    ['💬 الرسائل', '🔔 الإشعارات'],
    ['💰 المحفظة', '🎁 دعوة صديق'],
    ['👤 حسابي', '⚙️ الإعدادات'],
    ['🆘 المساعدة']
  ];

  if (isAdmin(id)) {
    rows.push(['👨‍💼 لوحة الإدارة']);
  }

  return kb(rows);
}

function adminKeyboard() {
  return ikb([
    [
      {
        text: '📊 الإحصائيات',
        callback_data: 'admin_stats'
      },
      {
        text: '👥 المستخدمون',
        callback_data: 'admin_users'
      }
    ],
    [
      {
        text: '🚗 الرحلات',
        callback_data: 'admin_rides'
      },
      {
        text: '🎫 الحجوزات',
        callback_data: 'admin_bookings'
      }
    ],
    [
      {
        text: '📦 الطلبات',
        callback_data: 'admin_requests'
      },
      {
        text: '💳 الشحن',
        callback_data: 'admin_topups'
      }
    ],
    [
      {
        text: '📢 الإعلانات',
        callback_data: 'admin_banners'
      },
      {
        text: '🎟️ أكواد الخصم',
        callback_data: 'admin_promos'
      }
    ],
    [
      {
        text: '🔔 الإشعارات',
        callback_data: 'admin_notifications'
      },
      {
        text: '🗂️ قاعدة البيانات',
        callback_data: 'admin_schema'
      }
    ],
    [
      {
        text: '🔒 إدارة المستخدمين',
        callback_data: 'admin_manage_users'
      },
      {
        text: '⬅️ القائمة الرئيسية',
        callback_data: 'main_menu'
      }
    ]
  ]);
}

/* =========================================================
   MIGRATION
========================================================= */

function migrate() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS ratings (
      id TEXT PRIMARY KEY,
      ride_id TEXT,
      from_user_id INTEGER NOT NULL,
      to_user_id INTEGER NOT NULL,
      rating INTEGER NOT NULL,
      comment TEXT DEFAULT '',
      created_at INTEGER NOT NULL DEFAULT (unixepoch())
    );

    CREATE TABLE IF NOT EXISTS banners (
      id TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      image_url TEXT DEFAULT '',
      link_url TEXT DEFAULT '',
      is_active INTEGER NOT NULL DEFAULT 1,
      created_at INTEGER NOT NULL DEFAULT (unixepoch())
    );

    CREATE TABLE IF NOT EXISTS promo_codes (
      id TEXT PRIMARY KEY,
      code TEXT UNIQUE NOT NULL,
      discount_points INTEGER NOT NULL DEFAULT 0,
      max_uses INTEGER NOT NULL DEFAULT 0,
      used_count INTEGER NOT NULL DEFAULT 0,
      expires_at INTEGER,
      is_active INTEGER NOT NULL DEFAULT 1,
      created_at INTEGER NOT NULL DEFAULT (unixepoch())
    );
  `);

  if (tableExists('rides')) {
    if (!hasColumn('rides', 'dropoff_point')) {
      try {
        db.exec(`
          ALTER TABLE rides
          ADD COLUMN dropoff_point TEXT DEFAULT ''
        `);
      } catch {}
    }
  }
}

migrate();

/* =========================================================
   START
========================================================= */

async function start(chatId, from) {
  const user = ensureUser(from);

  await send(
    chatId,
    `<b>🚗 أهلاً بك في وصلني</b>

منصة الرحلات التشاركية والطلبات الخاصة.

👤 ${esc(user?.name || from.first_name || '')}
💰 رصيدك: <b>${points(user)} نقطة</b>

اختر الخدمة من القائمة:`,
    mainKeyboard(from.id)
  );
}

/* =========================================================
   HELP
========================================================= */

async function help(chatId, id) {
  await send(
    chatId,
    `<b>🆘 مساعدة وصلني</b>

🔎 <b>البحث عن رحلة</b>
البحث عن الرحلات حسب الانطلاق والوصول والتاريخ والمقاعد.

🚗 <b>نشر رحلة</b>
نشر رحلة كسائق وتحديد السعر والمقاعد والسيارة ومكان التجمع والنزول.

📦 <b>طلب رحلة</b>
إنشاء طلب رحلة خاصة.

📋 <b>رحلاتي</b>
عرض الرحلات التي نشرتها وحجوزاتك وطلباتك.

💬 <b>الرسائل</b>
عرض الرسائل المرتبطة بالرحلات.

💰 <b>المحفظة</b>
عرض الرصيد وطلبات الشحن وسجل العمليات.

🎁 <b>دعوة صديق</b>
مشاركة كود الإحالة.

👤 <b>حسابي</b>
بيانات الحساب والتقييم والرحلات.

⚙️ <b>الإعدادات</b>
إعدادات الحساب واللغة.`,
    mainKeyboard(id)
  );
}

/* =========================================================
   ACCOUNT
========================================================= */

async function account(chatId, id) {
  const u = userByTelegram(id);

  if (!u) {
    return send(
      chatId,
      '⚠️ استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  await send(
    chatId,
    `<b>👤 حسابي</b>

الاسم: <b>${esc(u.name || '')}</b>
Telegram ID: <code>${id}</code>

⭐ التقييم: <b>${u.rating ?? 0}</b>
🚗 الرحلات: <b>${u.ride_count ?? 0}</b>
💰 النقاط: <b>${points(u)}</b>
🔐 الحالة: ${
      u.is_suspended
        ? '🚫 موقوف'
        : '✅ فعال'
    }

🎁 كود الإحالة:
<code>${esc(u.referral_code || '')}</code>`,
    mainKeyboard(id)
  );
}

/* =========================================================
   WALLET
========================================================= */

async function wallet(chatId, id) {
  const u = userByTelegram(id);

  if (!u) {
    return send(
      chatId,
      '⚠️ استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  let pending = 0;

  if (tableExists('topup_requests')) {
    pending = db.prepare(`
      SELECT COUNT(*) AS c
      FROM topup_requests
      WHERE user_id = ?
      AND status = 'PENDING'
    `).get(u.id).c;
  }

  await send(
    chatId,
    `<b>💰 محفظتي</b>

💎 الرصيد:
<b>${points(u)} نقطة</b>

💳 طلبات الشحن المعلقة:
<b>${pending}</b>`,
    ikb([
      [
        {
          text: '💳 طلب شحن',
          callback_data: 'wallet_topup'
        },
        {
          text: '📜 سجل العمليات',
          callback_data: 'wallet_history'
        }
      ],
      [
        {
          text: '🎁 دعوة صديق',
          callback_data: 'referral'
        }
      ]
    ])
  );
}

/* =========================================================
   REFERRAL
========================================================= */

async function referral(chatId, id) {
  const u = userByTelegram(id);

  if (!u) {
    return send(
      chatId,
      '⚠️ استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  await send(
    chatId,
    `<b>🎁 دعوة صديق</b>

كود الإحالة:

<code>${esc(u.referral_code || '')}</code>

🎁 مكافأة الإحالة:
<b>100 نقطة</b>`,
    ikb([
      [
        {
          text: '📤 مشاركة عبر Telegram',
          switch_inline_query:
            `انضم إلى Wasalni باستخدام كود ${u.referral_code || ''}`
        }
      ],
      [
        {
          text: '⬅️ رجوع',
          callback_data: 'main_menu'
        }
      ]
    ])
  );
}

/* =========================================================
   SEARCH
========================================================= */

function searchStart(chatId, id) {
  sessions.set(String(id), {
    type: 'search',
    step: 'from',
    data: {}
  });

  return send(
    chatId,
    `<b>🔎 البحث عن رحلة</b>

أرسل مدينة الانطلاق:`,
    kb([['❌ إلغاء']])
  );
}

async function finishSearch(chatId, id, data) {
  if (!tableExists('rides')) {
    return send(
      chatId,
      '❌ جدول الرحلات غير موجود.',
      mainKeyboard(id)
    );
  }

  const rows = db.prepare(`
    SELECT *
    FROM rides
    WHERE LOWER(start_city) LIKE LOWER(?)
      AND LOWER(end_city) LIKE LOWER(?)
      AND departure_date LIKE ?
      AND status = 'UPCOMING'
      AND available_seats >= ?
    ORDER BY departure_time
    LIMIT 20
  `).all(
    `%${data.from}%`,
    `%${data.to}%`,
    `%${data.date}%`,
    data.seats
  );

  if (!rows.length) {
    return send(
      chatId,
      `<b>🔎 نتائج البحث</b>

لا توجد رحلة مطابقة حالياً.

📍 ${esc(data.from)} → ${esc(data.to)}
📅 ${esc(data.date)}
💺 ${data.seats}`,
      mainKeyboard(id)
    );
  }

  for (const r of rows) {
    const driver = userById(r.driver_id);

    await send(
      chatId,
      `<b>🚗 رحلة #${esc(r.id)}</b>

📍 ${esc(r.start_city)} → ${esc(r.end_city)}
📅 ${esc(r.departure_date)}
🕐 ${esc(r.departure_time)}

👤 السائق:
<b>${esc(driver?.name || '—')}</b>

⭐ التقييم:
${driver?.rating ?? 0}

🚘 السيارة:
${esc(r.car_model || '—')}

🎨 اللون:
${esc(r.car_color || '—')}

💺 المقاعد:
${r.available_seats}

💰 السعر:
<b>${r.price_per_seat} ${esc(r.price_currency || 'POINTS')}</b>

📍 التجمع:
${esc(r.meeting_point || '—')}

📍 النزول:
${esc(r.dropoff_point || '—')}

📝 ${esc(r.notes || 'لا توجد ملاحظات')}`,
      ikb([
        [
          {
            text: '🎫 حجز',
            callback_data:
              `book:${r.id}:${data.seats}`
          },
          {
            text: '📞 السائق',
            callback_data:
              `driver:${r.driver_id}`
          }
        ]
      ])
    );
  }
}

/* =========================================================
   PUBLISH
========================================================= */

function publishStart(chatId, id) {
  sessions.set(String(id), {
    type: 'publish',
    step: 'from',
    data: {}
  });

  return send(
    chatId,
    `<b>🚗 نشر رحلة</b>

أرسل مدينة الانطلاق:`,
    kb([['❌ إلغاء']])
  );
}

/* =========================================================
   REQUEST
========================================================= */

function requestStart(chatId, id) {
  sessions.set(String(id), {
    type: 'request',
    step: 'from',
    data: {}
  });

  return send(
    chatId,
    `<b>📦 طلب رحلة</b>

أرسل مدينة الانطلاق:`,
    kb([['❌ إلغاء']])
  );
}

/* =========================================================
   MY RIDES - FIXED
========================================================= */

async function myRides(chatId, id) {
  const u = userByTelegram(id);

  console.log('========== MY RIDES ==========');
  console.log('Telegram ID:', id);
  console.log('User:', u);

  if (!u) {
    console.log('MY RIDES: USER NOT FOUND');

    return send(
      chatId,
      '⚠️ الحساب غير موجود. استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  console.log('Internal user ID:', u.id);

  // =====================================================
  // 1. الرحلات التي نشرها المستخدم
  // =====================================================

  const published = db.prepare(`
    SELECT
      r.*,

      COALESCE(
        (
          SELECT SUM(
            CASE
              WHEN LOWER(COALESCE(b.status, '')) NOT IN
                ('cancelled', 'canceled', 'rejected')
              THEN COALESCE(b.seats_booked, 0)
              ELSE 0
            END
          )
          FROM bookings b
          WHERE b.ride_id = r.id
        ),
        0
      ) AS booked_seats

    FROM rides r

    WHERE r.driver_id = ?

    ORDER BY r.created_at DESC

    LIMIT 20
  `).all(u.id);

  console.log('Published rides:', published);

  // =====================================================
  // 2. الحجوزات الخاصة بالمستخدم
  // =====================================================

  const bookings = db.prepare(`
    SELECT
      b.*,

      r.start_city,
      r.end_city,
      r.departure_date,
      r.departure_time,
      r.price_per_seat,
      r.price_currency,
      r.driver_id,

      u2.name AS driver_name

    FROM bookings b

    LEFT JOIN rides r
      ON r.id = b.ride_id

    LEFT JOIN users u2
      ON u2.id = r.driver_id

    WHERE b.passenger_id = ?

    ORDER BY b.created_at DESC

    LIMIT 20
  `).all(u.id);

  console.log('Bookings:', bookings);

  // =====================================================
  // 3. طلبات الرحلات الخاصة
  // =====================================================

  let requests = [];

  if (tableExists('requested_trips')) {
    requests = db.prepare(`
      SELECT *
      FROM requested_trips
      WHERE user_id = ?
      ORDER BY created_at DESC
      LIMIT 20
    `).all(u.id);
  }

  console.log('Requests:', requests);

  // =====================================================
  // بناء الرسالة
  // =====================================================

  let text = '<b>📋 رحلاتي</b>\n\n';

  // =====================================================
  // الرحلات المنشورة
  // =====================================================

  text += '<b>🚗 الرحلات التي نشرتها</b>\n\n';

  if (!published.length) {
    text += 'لا توجد رحلات منشورة.\n\n';
  } else {
    for (const r of published) {
      const booked = Number(r.booked_seats || 0);

      const total = Number(
        r.total_seats ??
        r.available_seats ??
        0
      );

      const remaining = Math.max(
        0,
        Number(r.available_seats ?? total) - booked
      );

      text +=
        `🚗 <b>#${esc(r.id)}</b>\n` +
        `📍 ${esc(r.start_city || '—')} → ${esc(r.end_city || '—')}\n` +
        `📅 ${esc(r.departure_date || '—')}\n` +
        `🕐 ${esc(r.departure_time || '—')}\n` +
        `💰 ${esc(r.price_per_seat ?? 0)} ${esc(r.price_currency || 'POINTS')}\n` +
        `💺 المتاح: <b>${remaining}/${total}</b>\n` +
        `🚘 السيارة: ${esc(r.car_model || '—')}\n` +
        `🎨 اللون: ${esc(r.car_color || '—')}\n` +
        `🔢 اللوحة: ${esc(r.car_plate || '—')}\n` +
        `📍 التجمع: ${esc(r.meeting_point || '—')}\n` +
        `📍 النزول: ${esc(r.dropoff_point || '—')}\n` +
        `🔹 الحالة: <b>${esc(r.status || 'UPCOMING')}</b>\n\n`;
    }
  }

  // =====================================================
  // الحجوزات
  // =====================================================

  text += '<b>🎫 حجوزاتي</b>\n\n';

  if (!bookings.length) {
    text += 'لا توجد حجوزات.\n\n';
  } else {
    for (const b of bookings) {
      text +=
        `🎫 <b>#${esc(b.id)}</b>\n` +
        `🚗 الرحلة: <code>${esc(b.ride_id || '—')}</code>\n` +
        `📍 ${esc(b.start_city || '—')} → ${esc(b.end_city || '—')}\n` +
        `📅 ${esc(b.departure_date || '—')}\n` +
        `🕐 ${esc(b.departure_time || '—')}\n` +
        `👤 السائق: ${esc(b.driver_name || '—')}\n` +
        `💺 المقاعد: ${esc(b.seats_booked || 0)}\n` +
        `💰 النقاط: ${esc(b.total_points || 0)}\n` +
        `🔹 الحالة: <b>${esc(b.status || '—')}</b>\n\n`;
    }
  }

  // =====================================================
  // الطلبات الخاصة
  // =====================================================

  text += '<b>📦 طلباتي الخاصة</b>\n\n';

  if (!requests.length) {
    text += 'لا توجد طلبات خاصة.\n\n';
  } else {
    for (const r of requests) {
      text +=
        `📦 <b>#${esc(r.id)}</b>\n` +
        `📍 ${esc(r.start_city || '—')} → ${esc(r.end_city || '—')}\n` +
        `📅 ${esc(r.departure_date || '—')}\n` +
        `🕐 ${esc(r.departure_time || '—')}\n` +
        `👨 ${esc(r.men_count || 0)} ` +
        `👩 ${esc(r.women_count || 0)} ` +
        `👶 ${esc(r.children_count || 0)}\n` +
        `🔹 الحالة: <b>${esc(r.status || 'OPEN')}</b>\n\n`;
    }
  }

  console.log('Sending MY RIDES response to:', id);
  console.log('Published count:', published.length);
  console.log('Bookings count:', bookings.length);
  console.log('Requests count:', requests.length);
  console.log('==============================');

  return send(
    chatId,
    text,
    mainKeyboard(id)
  );
}

/* =========================================================
   BOOKING
========================================================= */

async function bookRide(
  chatId,
  id,
  rideId,
  seats
) {
  const u = userByTelegram(id);

  if (!u) {
    return send(
      chatId,
      '⚠️ استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  const r = db
    .prepare(`
      SELECT *
      FROM rides
      WHERE id = ?
    `)
    .get(rideId);

  if (!r) {
    return send(
      chatId,
      '❌ الرحلة غير موجودة.'
    );
  }

  seats = Number(seats);

  if (!Number.isInteger(seats) || seats < 1) {
    return send(
      chatId,
      '❌ عدد المقاعد غير صحيح.'
    );
  }

  if (
    Number(r.available_seats) <
    seats
  ) {
    return send(
      chatId,
      '❌ عدد المقاعد غير كافٍ.'
    );
  }

  const total =
    Number(r.price_per_seat || 0) *
    seats;

  const bookingId =
    `B${Date.now()}${Math.floor(Math.random() * 1000)}`;

  const transaction =
    db.transaction(() => {
      db.prepare(`
        INSERT INTO bookings
        (
          id,
          ride_id,
          passenger_id,
          seats_booked,
          total_points,
          status
        )
        VALUES (?, ?, ?, ?, ?, 'UPCOMING')
      `).run(
        bookingId,
        rideId,
        u.id,
        seats,
        total
      );

      db.prepare(`
        UPDATE rides
        SET available_seats =
          available_seats - ?
        WHERE id = ?
      `).run(
        seats,
        rideId
      );
    });

  transaction();

  await send(
    chatId,
    `<b>✅ تم الحجز بنجاح</b>

🎫 رقم الحجز:
<code>${bookingId}</code>

🚗 الرحلة:
<code>${esc(rideId)}</code>

💺 المقاعد:
${seats}

💰 الإجمالي:
${total} نقدًا`,
    mainKeyboard(id)
  );
}
/* =========================================================
   CANCEL BOOKING
========================================================= */

async function cancelBooking(
  chatId,
  id,
  bookingId
) {
  const u = userByTelegram(id);

  if (!u) {
    return send(
      chatId,
      '⚠️ استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  const booking = db.prepare(`
    SELECT
      b.*,
      r.start_city,
      r.end_city,
      r.departure_date,
      r.departure_time,
      r.driver_id
    FROM bookings b
    LEFT JOIN rides r
      ON r.id = b.ride_id
    WHERE b.id = ?
      AND b.passenger_id = ?
  `).get(
    bookingId,
    u.id
  );

  if (!booking) {
    return send(
      chatId,
      '❌ الحجز غير موجود أو لا يخص حسابك.',
      mainKeyboard(id)
    );
  }

  const status =
    String(booking.status || '').toUpperCase();

  if (
    status === 'CANCELLED' ||
    status === 'CANCELED' ||
    status === 'REJECTED'
  ) {
    return send(
      chatId,
      '⚠️ هذا الحجز ملغى بالفعل.',
      mainKeyboard(id)
    );
  }

  const seats =
    Math.max(
      1,
      Number(booking.seats_booked || 0)
    );

  const transaction =
    db.transaction(() => {

      // 1. إلغاء الحجز
      db.prepare(`
        UPDATE bookings
        SET status = 'CANCELLED'
        WHERE id = ?
          AND passenger_id = ?
          AND UPPER(COALESCE(status, '')) NOT IN
            ('CANCELLED', 'CANCELED', 'REJECTED')
      `).run(
        bookingId,
        u.id
      );

      // 2. إعادة المقاعد للرحلة
      db.prepare(`
        UPDATE rides
        SET available_seats =
          available_seats + ?
        WHERE id = ?
      `).run(
        seats,
        booking.ride_id
      );
    });

  try {
    transaction();

    await send(
      chatId,
      `<b>✅ تم إلغاء الحجز</b>

🎫 رقم الحجز:
<code>${esc(bookingId)}</code>

🚗 الرحلة:
${esc(booking.start_city || '—')} → ${esc(booking.end_city || '—')}

💺 تمت إعادة:
<b>${seats}</b> مقعد

💵 الدفع كان نقدًا، لذلك لم يتم تغيير رصيد النقاط.`,
      mainKeyboard(id)
    );

  } catch (error) {

    console.error(
      'Cancel booking error:',
      error
    );

    await send(
      chatId,
      `<b>❌ فشل إلغاء الحجز</b>

<code>${esc(error.message)}</code>`,
      mainKeyboard(id)
    );
  }
}
/* =========================================================
   TOPUP
========================================================= */

function topupStart(chatId, id) {
  sessions.set(String(id), {
    type: 'topup',
    step: 'points',
    data: {}
  });

  return send(
    chatId,
    `<b>💳 طلب شحن المحفظة</b>

أرسل عدد النقاط المطلوبة:

مثال:
<code>500</code>`,
    kb([['❌ إلغاء']])
  );
}

/* =========================================================
   SESSION ENGINE
========================================================= */

async function sessionMessage(message) {
  const id = message.from.id;
  const chatId = message.chat.id;

  const session =
    sessions.get(String(id));

  if (!session) {
    return false;
  }

  const text =
    String(message.text || '').trim();

  if (text === '❌ إلغاء') {
    sessions.delete(String(id));

    await send(
      chatId,
      '❌ تم إلغاء العملية.',
      mainKeyboard(id)
    );

    return true;
  }

  /* =======================================================
     SEARCH SESSION
  ======================================================= */

  if (session.type === 'search') {
    const d = session.data;

    if (session.step === 'from') {
      d.from = text;
      session.step = 'to';

      await send(
        chatId,
        'أرسل مدينة الوصول:'
      );
    }

    else if (session.step === 'to') {
      d.to = text;
      session.step = 'date';

      await send(
        chatId,
        'أرسل تاريخ السفر:\nمثال: 2026-08-20'
      );
    }

    else if (session.step === 'date') {
      d.date = text;
      session.step = 'seats';

      await send(
        chatId,
        'كم مقعداً تريد؟'
      );
    }

    else if (session.step === 'seats') {
      d.seats =
        Math.max(
          1,
          Number(text) || 1
        );

      await finishSearch(
        chatId,
        id,
        d
      );

      sessions.delete(String(id));
    }

    return true;
  }

  /* =======================================================
     PUBLISH SESSION
  ======================================================= */

  if (session.type === 'publish') {
    const d = session.data;

    if (session.step === 'from') {
      d.from = text;
      session.step = 'to';

      await send(
        chatId,
        'أرسل مدينة الوصول:'
      );
    }

    else if (session.step === 'to') {
      d.to = text;
      session.step = 'date';

      await send(
        chatId,
        'أرسل تاريخ الرحلة:\nمثال: 2026-08-20'
      );
    }

    else if (session.step === 'date') {
      d.date = text;
      session.step = 'time';

      await send(
        chatId,
        'أرسل وقت الانطلاق:\nمثال: 08:30'
      );
    }

    else if (session.step === 'time') {
      d.time = text;
      session.step = 'seats';

      await send(
        chatId,
        'عدد المقاعد؟'
      );
    }

    else if (session.step === 'seats') {
      d.seats =
        Math.max(
          1,
          Number(text) || 1
        );

      session.step = 'price';

      await send(
        chatId,
        'سعر المقعد؟'
      );
    }

    else if (session.step === 'price') {
      d.price =
        Math.max(
          0,
          Number(text) || 0
        );

      session.step = 'car';

      await send(
        chatId,
        'نوع السيارة؟'
      );
    }

    else if (session.step === 'car') {
      d.car = text;
      session.step = 'color';

      await send(
        chatId,
        'لون السيارة؟'
      );
    }

    else if (session.step === 'color') {
      d.color = text;
      session.step = 'plate';

      await send(
        chatId,
        'رقم اللوحة؟'
      );
    }

    else if (session.step === 'plate') {
      d.plate = text;
      session.step = 'meeting';

      await send(
        chatId,
        'مكان التجمع؟'
      );
    }

    else if (session.step === 'meeting') {
      d.meeting = text;
      session.step = 'dropoff';

      await send(
        chatId,
        'مكان النزول؟'
      );
    }

    else if (session.step === 'dropoff') {
      d.dropoff = text;
      session.step = 'notes';

      await send(
        chatId,
        'ملاحظات الرحلة أو اكتب "لا يوجد":'
      );
    }

    else if (session.step === 'notes') {
      d.notes =
        text === 'لا يوجد'
          ? ''
          : text;

      const u =
        userByTelegram(id);

      if (!u) {
        await send(
          chatId,
          '⚠️ استخدم /start أولاً.'
        );

        sessions.delete(String(id));
        return true;
      }

      if (!tableExists('rides')) {
        await send(
          chatId,
          '❌ جدول الرحلات غير موجود في قاعدة البيانات.'
        );

        sessions.delete(String(id));
        return true;
      }

      const rideId =
        `R${Date.now()}${Math.floor(Math.random() * 1000)}`;

      try {
        db.prepare(`
          INSERT INTO rides
          (
            id,
            driver_id,
            start_city,
            end_city,
            departure_date,
            departure_time,
            price_per_seat,
            price_currency,
            available_seats,
            total_seats,
            car_model,
            car_color,
            car_plate,
            allows_luggage,
            accept_cash,
            accept_wallet,
            women_only,
            status,
            meeting_point,
            dropoff_point,
            notes
          )
          VALUES
          (
            ?, ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, 1, 1, 0,
            0, 'UPCOMING', ?, ?, ?
          )
        `).run(
          rideId,
          u.id,
          d.from,
          d.to,
          d.date,
          d.time,
          d.price,
          'POINTS',
          d.seats,
          d.seats,
          d.car,
          d.color,
          d.plate,
          d.meeting,
          d.dropoff,
          d.notes
        );

        if (
          tableExists('users') &&
          hasColumn('users', 'ride_count')
        ) {
          db.prepare(`
            UPDATE users
            SET ride_count =
              COALESCE(ride_count, 0) + 1
            WHERE id = ?
          `).run(u.id);
        }

        console.log('✅ RIDE INSERTED:', rideId);

        if (
          tableExists('users') &&
          hasColumn('users', 'ride_count')
        ) {
          db.prepare(`
            UPDATE users
            SET ride_count =
              COALESCE(ride_count, 0) + 1
            WHERE id = ?
          `).run(u.id);
        }

        console.log('✅ RIDE COUNT UPDATED:', u.id);

        const successText =
`<b>✅ تم نشر الرحلة</b>

🚗 رقم الرحلة:
<code>${rideId}</code>

📍 ${esc(d.from)} → ${esc(d.to)}
📅 ${esc(d.date)}
🕐 ${esc(d.time)}

💺 ${d.seats}
💰 ${d.price} نقطة

🚘 ${esc(d.car)}
🎨 ${esc(d.color)}
🔢 ${esc(d.plate)}

📍 التجمع:
${esc(d.meeting)}

📍 النزول:
${esc(d.dropoff)}

📝 ${esc(d.notes || 'لا يوجد')}`;

        console.log('📤 SENDING PUBLISH SUCCESS MESSAGE');

        try {
          await send(
            chatId,
            successText,
            mainKeyboard(id)
          );

          console.log('✅ PUBLISH SUCCESS MESSAGE SENT');
        } catch (telegramError) {
          console.error(
            '❌ RIDE SAVED BUT SUCCESS MESSAGE FAILED:',
            telegramError
          );
        }

      } catch (error) {
        console.error(
          '❌ Publish ride database error:',
          error
        );

        try {
          await send(
            chatId,
            `<b>❌ فشل نشر الرحلة</b>

<code>${esc(error.message)}</code>`,
            mainKeyboard(id)
          );
        } catch (telegramError) {
          console.error(
            '❌ Could not send error message:',
            telegramError
          );
        }
      }

      sessions.delete(String(id));
    }

    return true;
  }

  /* =======================================================
     REQUEST SESSION
  ======================================================= */

  if (session.type === 'request') {
    const d = session.data;

    if (session.step === 'from') {
      d.from = text;
      session.step = 'to';

      await send(
        chatId,
        'مدينة الوصول؟'
      );
    }

    else if (session.step === 'to') {
      d.to = text;
      session.step = 'date';

      await send(
        chatId,
        'تاريخ الرحلة؟'
      );
    }

    else if (session.step === 'date') {
      d.date = text;
      session.step = 'time';

      await send(
        chatId,
        'وقت الرحلة؟'
      );
    }

    else if (session.step === 'time') {
      d.time = text;
      session.step = 'men';

      await send(
        chatId,
        'عدد الرجال؟'
      );
    }

    else if (session.step === 'men') {
      d.men =
        Math.max(
          0,
          Number(text) || 0
        );

      session.step = 'women';

      await send(
        chatId,
        'عدد النساء؟'
      );
    }

    else if (session.step === 'women') {
      d.women =
        Math.max(
          0,
          Number(text) || 0
        );

      session.step = 'children';

      await send(
        chatId,
        'عدد الأطفال؟'
      );
    }

    else if (session.step === 'children') {
      d.children =
        Math.max(
          0,
          Number(text) || 0
        );

      const u =
        userByTelegram(id);

      if (!u) {
        await send(
          chatId,
          '⚠️ استخدم /start أولاً.'
        );

        sessions.delete(String(id));
        return true;
      }

      if (!tableExists('requested_trips')) {
        await send(
          chatId,
          '❌ جدول الطلبات غير موجود.'
        );

        sessions.delete(String(id));
        return true;
      }

      const requestId =
        `Q${Date.now()}${Math.floor(Math.random() * 1000)}`;

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
          children_count,
          status
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')
      `).run(
        requestId,
        u.id,
        d.from,
        d.to,
        d.date,
        d.time,
        d.men,
        d.women,
        d.children
      );

      await send(
        chatId,
        `<b>✅ تم إنشاء طلب الرحلة</b>

📦 رقم الطلب:
<code>${requestId}</code>

📍 ${esc(d.from)} → ${esc(d.to)}
📅 ${esc(d.date)}
🕐 ${esc(d.time)}

👨 ${d.men}
👩 ${d.women}
👶 ${d.children}

سيظهر الطلب للسائقين.`,
        mainKeyboard(id)
      );

      sessions.delete(String(id));
    }

    return true;
  }

  /* =======================================================
     TOPUP SESSION
  ======================================================= */

  if (session.type === 'topup') {
    if (session.step === 'points') {
      const p =
        Number(text);

      if (
        !Number.isFinite(p) ||
        p <= 0
      ) {
        await send(
          chatId,
          '❌ أدخل عدد نقاط صحيح.'
        );

        return true;
      }

      session.data.points =
        Math.floor(p);

      session.step = 'price';

      await send(
        chatId,
        'أدخل سعر الشحن بالدولار:'
      );
    }

    else if (session.step === 'price') {
      const price =
        Number(text);

      if (
        !Number.isFinite(price) ||
        price < 0
      ) {
        await send(
          chatId,
          '❌ أدخل سعراً صحيحاً.'
        );

        return true;
      }

      const u =
        userByTelegram(id);

      if (!u) {
        await send(
          chatId,
          '⚠️ استخدم /start أولاً.'
        );

        sessions.delete(String(id));
        return true;
      }

      if (!tableExists('topup_requests')) {
        await send(
          chatId,
          '❌ جدول طلبات الشحن غير موجود.'
        );

        sessions.delete(String(id));
        return true;
      }

      const requestId =
        `TOP${Date.now()}${Math.floor(Math.random() * 1000)}`;

      db.prepare(`
        INSERT INTO topup_requests
        (
          id,
          user_id,
          package_points,
          package_price_usd,
          status
        )
        VALUES (?, ?, ?, ?, 'PENDING')
      `).run(
        requestId,
        u.id,
        session.data.points,
        price
      );

      await send(
        chatId,
        `<b>✅ تم إرسال طلب الشحن</b>

رقم الطلب:
<code>${requestId}</code>

💎 النقاط:
${session.data.points}

💵 السعر:
$${price}

⏳ بانتظار موافقة الإدارة.`,
        mainKeyboard(id)
      );

      for (const adminId of ADMIN_IDS) {
        try {
          await send(
            adminId,
            `<b>💳 طلب شحن جديد</b>

رقم الطلب:
<code>${requestId}</code>

المستخدم:
<code>${u.id}</code>

الاسم:
${esc(u.name)}

النقاط:
<b>${session.data.points}</b>

السعر:
<b>$${price}</b>`,
            ikb([
              [
                {
                  text: '✅ قبول',
                  callback_data:
                    `topup_approve:${requestId}`
                },
                {
                  text: '❌ رفض',
                  callback_data:
                    `topup_reject:${requestId}`
                }
              ]
            ])
          );
        } catch (error) {
          console.error(
            'Admin notification:',
            error.message
          );
        }
      }

      sessions.delete(String(id));
    }

    return true;
  }

  return true;
}

/* =========================================================
   ADMIN
========================================================= */

async function admin(chatId, id) {
  if (!isAdmin(id)) {
    return send(
      chatId,
      '⛔ غير مصرح لك.'
    );
  }

  await send(
    chatId,
    `<b>👨‍💼 Wasalni Admin</b>

لوحة التحكم الكاملة.`,
    adminKeyboard()
  );
}

async function adminStats(chatId) {
  if (!isAdmin(chatId)) return;

  let totalPoints = 0;

  if (tableExists('users')) {
    totalPoints =
      db.prepare(`
        SELECT COALESCE(
          SUM(wallet_points), 0
        ) AS p
        FROM users
      `).get().p;
  }

  await send(
    chatId,
    `<b>📊 إحصائيات Wasalni</b>

👥 المستخدمون:
<b>${count('users')}</b>

🚗 الرحلات:
<b>${count('rides')}</b>

🎫 الحجوزات:
<b>${count('bookings')}</b>

📦 الطلبات:
<b>${count('requested_trips')}</b>

💬 الرسائل:
<b>${count('chat_messages')}</b>

⭐ التقييمات:
<b>${count('ratings')}</b>

💳 طلبات الشحن:
<b>${count('topup_requests')}</b>

📢 الإعلانات:
<b>${count('banners')}</b>

🎟️ أكواد الخصم:
<b>${count('promo_codes')}</b>

💰 مجموع النقاط:
<b>${totalPoints}</b>`,
    adminKeyboard()
  );
}

async function adminUsers(chatId) {
  const rows = db.prepare(`
    SELECT *
    FROM users
    ORDER BY id DESC
    LIMIT 30
  `).all();

  let text =
    '<b>👥 المستخدمون</b>\n\n';

  if (!rows.length) {
    text += 'لا يوجد مستخدمون.';
  }

  for (const u of rows) {
    text +=
      `👤 <b>${esc(u.name)}</b>\n` +
      `ID: <code>${u.id}</code>\n` +
      `TG: <code>${esc(u.telegram_id || '')}</code>\n` +
      `💰 ${u.wallet_points || 0} نقطة\n` +
      `⭐ ${u.rating || 0}\n` +
      `🚗 ${u.ride_count || 0}\n` +
      `🔐 ${
        u.is_suspended
          ? '🚫 موقوف'
          : '✅ فعال'
      }\n\n`;
  }

  await send(
    chatId,
    text,
    adminKeyboard()
  );
}

async function adminRides(chatId) {
  if (!tableExists('rides')) {
    return send(
      chatId,
      '❌ جدول الرحلات غير موجود.',
      adminKeyboard()
    );
  }

  const rows = db.prepare(`
    SELECT *
    FROM rides
    ORDER BY
      COALESCE(created_at, 0) DESC
    LIMIT 50
  `).all();

  let text =
    '<b>🚗 الرحلات</b>\n\n';

  if (!rows.length) {
    text += 'لا توجد رحلات.';
  }

  for (const r of rows) {
    text +=
      `🚗 <b>#${esc(r.id)}</b>\n` +
      `👤 السائق: ${esc(r.driver_id)}\n` +
      `📍 ${esc(r.start_city)} → ${esc(r.end_city)}\n` +
      `📅 ${esc(r.departure_date)}\n` +
      `🕐 ${esc(r.departure_time)}\n` +
      `💺 ${r.available_seats}/${r.total_seats}\n` +
      `💰 ${r.price_per_seat}\n` +
      `🔹 ${esc(r.status)}\n\n`;
  }

  await send(
    chatId,
    text,
    adminKeyboard()
  );
}

async function adminBookings(chatId) {
  if (!tableExists('bookings')) {
    return send(
      chatId,
      '❌ جدول الحجوزات غير موجود.',
      adminKeyboard()
    );
  }

  const rows = db.prepare(`
    SELECT *
    FROM bookings
    ORDER BY
      COALESCE(created_at, 0) DESC
    LIMIT 50
  `).all();

  let text =
    '<b>🎫 الحجوزات</b>\n\n';

  if (!rows.length) {
    text += 'لا توجد حجوزات.';
  }

  for (const b of rows) {
    text +=
      `🎫 <b>#${esc(b.id)}</b>\n` +
      `🚗 ${esc(b.ride_id)}\n` +
      `👤 ${b.passenger_id}\n` +
      `💺 ${b.seats_booked}\n` +
      `💰 ${b.total_points}\n` +
      `🔹 ${esc(b.status)}\n\n`;
  }

  await send(
    chatId,
    text,
    adminKeyboard()
  );
}

async function adminRequests(chatId) {
  if (!tableExists('requested_trips')) {
    return send(
      chatId,
      '❌ جدول الطلبات غير موجود.',
      adminKeyboard()
    );
  }

  const rows = db.prepare(`
    SELECT *
    FROM requested_trips
    ORDER BY
      COALESCE(created_at, 0) DESC
    LIMIT 50
  `).all();

  let text =
    '<b>📦 طلبات الرحلات</b>\n\n';

  if (!rows.length) {
    text += 'لا توجد طلبات.';
  }

  for (const r of rows) {
    text +=
      `📦 <b>#${esc(r.id)}</b>\n` +
      `👤 ${r.user_id}\n` +
      `📍 ${esc(r.start_city)} → ${esc(r.end_city)}\n` +
      `📅 ${esc(r.departure_date)}\n` +
      `🕐 ${esc(r.departure_time)}\n` +
      `👨 ${r.men_count} ` +
      `👩 ${r.women_count} ` +
      `👶 ${r.children_count}\n` +
      `🔹 ${esc(r.status)}\n\n`;
  }

  await send(
    chatId,
    text,
    adminKeyboard()
  );
}

async function adminTopups(chatId) {
  if (!tableExists('topup_requests')) {
    return send(
      chatId,
      '❌ جدول طلبات الشحن غير موجود.',
      adminKeyboard()
    );
  }

  const rows = db.prepare(`
    SELECT
      t.*,
      u.name
    FROM topup_requests t
    LEFT JOIN users u
      ON u.id = t.user_id
    ORDER BY
      COALESCE(t.created_at, 0) DESC
    LIMIT 50
  `).all();

  let text =
    '<b>💳 طلبات الشحن</b>\n\n';

  if (!rows.length) {
    text += 'لا توجد طلبات.';
  }

  for (const r of rows) {
    text +=
      `💳 <b>#${esc(r.id)}</b>\n` +
      `👤 ${esc(r.name || r.user_id)}\n` +
      `💎 ${r.package_points}\n` +
      `💵 $${r.package_price_usd}\n` +
      `🔹 ${esc(r.status)}\n\n`;
  }

  await send(
    chatId,
    text,
    adminKeyboard()
  );
}

/* =========================================================
   ADMIN USER MANAGEMENT
========================================================= */

async function adminUsersManage(chatId) {
  const rows = db.prepare(`
    SELECT
      id,
      name,
      wallet_points,
      is_suspended
    FROM users
    ORDER BY id DESC
    LIMIT 30
  `).all();

  const buttons = rows.map(u => [
    {
      text:
        `${u.is_suspended ? '🔓' : '🔒'} ${u.name}`,
      callback_data:
        `user_toggle:${u.id}`
    }
  ]);

  buttons.push([
    {
      text: '⬅️ رجوع',
      callback_data: 'admin_home'
    }
  ]);

  await send(
    chatId,
    `<b>🔒 إدارة المستخدمين</b>

اضغط على المستخدم لتغيير حالة الإيقاف.`,
    ikb(buttons)
  );
}

async function toggleUser(
  chatId,
  targetId
) {
  const u = userById(targetId);

  if (!u) {
    return send(
      chatId,
      '❌ المستخدم غير موجود.',
      adminKeyboard()
    );
  }

  const next =
    u.is_suspended ? 0 : 1;

  db.prepare(`
    UPDATE users
    SET
      is_suspended = ?,
      suspend_reason = ?
    WHERE id = ?
  `).run(
    next,
    next
      ? 'Suspended by admin'
      : null,
    targetId
  );

  await send(
    chatId,
    next
      ? `🔒 تم إيقاف <b>${esc(u.name)}</b>.`
      : `🔓 تم فك إيقاف <b>${esc(u.name)}</b>.`,
    adminKeyboard()
  );
}

/* =========================================================
   TOPUP ADMIN
========================================================= */

async function approveTopup(
  chatId,
  requestId
) {
  const r = db.prepare(`
    SELECT *
    FROM topup_requests
    WHERE id = ?
    AND status = 'PENDING'
  `).get(requestId);

  if (!r) {
    return send(
      chatId,
      '❌ الطلب غير موجود أو تمت معالجته.'
    );
  }

  const transaction =
    db.transaction(() => {
      db.prepare(`
        UPDATE topup_requests
        SET status = 'APPROVED'
        WHERE id = ?
      `).run(requestId);

      db.prepare(`
        UPDATE users
        SET wallet_points =
          wallet_points + ?
        WHERE id = ?
      `).run(
        r.package_points,
        r.user_id
      );

      if (tableExists('wallet_transactions')) {
        db.prepare(`
          INSERT INTO wallet_transactions
          (
            id,
            user_id,
            type,
            points,
            amount_usd,
            description,
            status
          )
          VALUES (?, ?, ?, ?, ?, ?, ?)
        `).run(
          `TOPTX${Date.now()}`,
          r.user_id,
          'TOPUP',
          r.package_points,
          r.package_price_usd,
          `شحن المحفظة ${requestId}`,
          'COMPLETED'
        );
      }
    });

  transaction();

  const u =
    userById(r.user_id);

  await send(
    chatId,
    `<b>✅ تم قبول الشحن</b>

الطلب:
<code>${requestId}</code>

المستخدم:
${esc(u?.name || '')}

النقاط المضافة:
<b>${r.package_points}</b>`,
    adminKeyboard()
  );

  if (u?.telegram_id) {
    await send(
      u.telegram_id,
      `<b>✅ تم قبول طلب الشحن</b>

تمت إضافة:
<b>${r.package_points} نقطة</b>

رصيدك الجديد:
<b>${points(userById(u.id))} نقطة</b>`,
      mainKeyboard(u.telegram_id)
    );
  }
}

async function rejectTopup(
  chatId,
  requestId
) {
  const r = db.prepare(`
    SELECT *
    FROM topup_requests
    WHERE id = ?
    AND status = 'PENDING'
  `).get(requestId);

  if (!r) {
    return send(
      chatId,
      '❌ الطلب غير موجود.'
    );
  }

  db.prepare(`
    UPDATE topup_requests
    SET
      status = 'REJECTED',
      rejection_reason = 'Rejected by admin'
    WHERE id = ?
  `).run(requestId);

  const u =
    userById(r.user_id);

  await send(
    chatId,
    `❌ تم رفض طلب الشحن <code>${requestId}</code>.`,
    adminKeyboard()
  );

  if (u?.telegram_id) {
    await send(
      u.telegram_id,
      `<b>❌ تم رفض طلب الشحن</b>

رقم الطلب:
<code>${requestId}</code>

يمكنك إنشاء طلب جديد.`,
      mainKeyboard(u.telegram_id)
    );
  }
}

/* =========================================================
   USER MESSAGES
========================================================= */

async function userMessages(
  chatId,
  id
) {
  const u =
    userByTelegram(id);

  if (!u) {
    return send(
      chatId,
      '⚠️ استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  if (!tableExists('chat_messages')) {
    return send(
      chatId,
      `<b>💬 الرسائل</b>

لا يوجد جدول للرسائل حالياً.`,
      mainKeyboard(id)
    );
  }

  const rows = db.prepare(`
    SELECT
      m.*,
      su.name AS sender_name,
      ru.name AS receiver_name
    FROM chat_messages m
    LEFT JOIN users su
      ON su.id = m.sender_id
    LEFT JOIN users ru
      ON ru.id = m.receiver_id
    WHERE
      m.sender_id = ?
      OR m.receiver_id = ?
    ORDER BY
      COALESCE(m.created_at, 0) DESC
    LIMIT 30
  `).all(
    u.id,
    u.id
  );

  if (!rows.length) {
    return send(
      chatId,
      `<b>💬 الرسائل</b>

لا توجد رسائل حالياً.`,
      mainKeyboard(id)
    );
  }

  let text =
    '<b>💬 آخر الرسائل</b>\n\n';

  for (const m of rows) {
    const mine =
      Number(m.sender_id) ===
      Number(u.id);

    const name = mine
      ? (m.receiver_name || 'المستلم')
      : (m.sender_name || 'المرسل');

    text +=
      `${mine ? '📤' : '📥'} ` +
      `<b>${esc(name)}</b>\n` +
      `💬 ${esc(m.message_text || '')}\n` +
      `🚗 الرحلة: ` +
      `<code>${esc(m.ride_id || '—')}</code>\n\n`;
  }

  return send(
    chatId,
    text,
    mainKeyboard(id)
  );
}

/* =========================================================
   NOTIFICATIONS
========================================================= */

async function userNotifications(
  chatId,
  id
) {
  const u =
    userByTelegram(id);

  if (!u) {
    return send(
      chatId,
      '⚠️ استخدم /start أولاً.',
      mainKeyboard(id)
    );
  }

  if (!tableExists('notifications')) {
    return send(
      chatId,
      `<b>🔔 الإشعارات</b>

لا يوجد جدول إشعارات.`,
      mainKeyboard(id)
    );
  }

  const rows = db.prepare(`
    SELECT *
    FROM notifications
    WHERE user_id = ?
    ORDER BY
      COALESCE(created_at, 0) DESC
    LIMIT 30
  `).all(u.id);

  if (!rows.length) {
    return send(
      chatId,
      `<b>🔔 الإشعارات</b>

لا توجد إشعارات جديدة.`,
      mainKeyboard(id)
    );
  }

  let text =
    '<b>🔔 الإشعارات</b>\n\n';

  for (const n of rows) {
    const icon =
      Number(n.is_read)
        ? '📭'
        : '🔔';

    text +=
      `${icon} <b>${esc(n.title || 'إشعار')}</b>\n` +
      `${esc(n.message || '')}\n\n`;
  }

  db.prepare(`
    UPDATE notifications
    SET is_read = 1
    WHERE user_id = ?
  `).run(u.id);

  return send(
    chatId,
    text,
    mainKeyboard(id)
  );
}

/* =========================================================
   CREATE NOTIFICATION
========================================================= */

function createNotification(
  userId,
  title,
  messageText,
  type = 'GENERAL'
) {
  if (!tableExists('notifications')) {
    return false;
  }

  try {
    db.prepare(`
      INSERT INTO notifications
      (
        id,
        user_id,
        title,
        message,
        type,
        is_read,
        created_at
      )
      VALUES (?, ?, ?, ?, ?, 0, unixepoch())
    `).run(
      `notif_${Date.now()}_${Math.random()
        .toString(36)
        .slice(2, 8)}`,
      userId,
      title,
      messageText,
      type
    );

    return true;
  } catch (error) {
    console.error(
      'Notification error:',
      error.message
    );

    return false;
  }
}

/* =========================================================
   CALLBACKS
========================================================= */

async function callback(q) {
  if (!q?.message) return;

  const chatId =
    q.message.chat.id;

  const id =
    q.from.id;

  const data =
    String(q.data || '');

  try {
    await tg(
      'answerCallbackQuery',
      {
        callback_query_id: q.id
      }
    );
  } catch {}

  if (
    (
      data.startsWith('admin_') ||
      data.startsWith('topup_') ||
      data.startsWith('user_')
    ) &&
    !isAdmin(id)
  ) {
    return send(
      chatId,
      '⛔ غير مصرح لك.'
    );
  }

  if (data === 'main_menu') {
    return send(
      chatId,
      '<b>🏠 القائمة الرئيسية</b>',
      mainKeyboard(id)
    );
  }

  if (data === 'admin_home') {
    return admin(chatId, id);
  }

  if (data === 'admin_stats') {
    return adminStats(chatId);
  }

  if (data === 'admin_users') {
    return adminUsers(chatId);
  }

  if (data === 'admin_rides') {
    return adminRides(chatId);
  }

  if (data === 'admin_bookings') {
    return adminBookings(chatId);
  }

  if (data === 'admin_requests') {
    return adminRequests(chatId);
  }

  if (data === 'admin_topups') {
    return adminTopups(chatId);
  }

  if (data === 'admin_manage_users') {
    return adminUsersManage(chatId);
  }

  if (data === 'admin_schema') {
    const tables =
      db.prepare(`
        SELECT table_name AS name
        FROM information_schema.tables
        WHERE table_schema = 'public'
        AND table_type = 'BASE TABLE'
        ORDER BY table_name
      `).all();

    return send(
      chatId,
      `<b>🗂️ قاعدة بيانات Wasalni</b>

${tables
  .map(t =>
    `✅ <code>${esc(t.name)}</code>`
  )
  .join('\n')}`,
      adminKeyboard()
    );
  }

  if (data === 'admin_banners') {
    return send(
      chatId,
      `<b>📢 الإعلانات</b>

عدد الإعلانات:
<b>${count('banners')}</b>`,
      adminKeyboard()
    );
  }

  if (data === 'admin_promos') {
    return send(
      chatId,
      `<b>🎟️ أكواد الخصم</b>

عدد الأكواد:
<b>${count('promo_codes')}</b>`,
      adminKeyboard()
    );
  }

  if (data === 'admin_notifications') {
    return send(
      chatId,
      `<b>🔔 الإشعارات</b>

عدد الإشعارات:
<b>${count('notifications')}</b>`,
      adminKeyboard()
    );
  }

  if (data === 'wallet_topup') {
    return topupStart(chatId, id);
  }

  if (data === 'wallet_history') {
    const u =
      userByTelegram(id);

    if (!u) {
      return send(
        chatId,
        '⚠️ استخدم /start أولاً.'
      );
    }

    if (!tableExists('wallet_transactions')) {
      return send(
        chatId,
        'لا يوجد سجل عمليات حالياً.',
        mainKeyboard(id)
      );
    }

    const rows =
      db.prepare(`
        SELECT *
        FROM wallet_transactions
        WHERE user_id = ?
        ORDER BY
          COALESCE(created_at, 0) DESC
        LIMIT 30
      `).all(u.id);

    let text =
      '<b>📜 سجل العمليات</b>\n\n';

    if (!rows.length) {
      text += 'لا توجد عمليات.';
    }

    for (const r of rows) {
      text +=
        `• ${esc(r.type)}\n` +
        `💎 ${esc(r.points)}\n` +
        `📝 ${esc(r.description)}\n` +
        `🔹 ${esc(r.status)}\n\n`;
    }

    return send(
      chatId,
      text,
      mainKeyboard(id)
    );
  }

  if (data === 'referral') {
    return referral(chatId, id);
  }

  if (data.startsWith('book:')) {
    const parts =
      data.split(':');

    return bookRide(
      chatId,
      id,
      parts[1],
      parts[2]
    );
  }

  if (data.startsWith('driver:')) {
    const driverId =
      data.split(':')[1];

    const driver =
      userById(driverId);

    return send(
      chatId,
      `<b>👤 السائق</b>

الاسم:
${esc(driver?.name || '—')}

⭐ التقييم:
${driver?.rating ?? 0}

🚗 الرحلات:
${driver?.ride_count ?? 0}`,
      mainKeyboard(id)
    );
  }

  if (data.startsWith('topup_approve:')) {
    return approveTopup(
      chatId,
      data.split(':')[1]
    );
  }

  if (data.startsWith('topup_reject:')) {
    return rejectTopup(
      chatId,
      data.split(':')[1]
    );
  }

  if (data.startsWith('user_toggle:')) {
    return toggleUser(
      chatId,
      Number(data.split(':')[1])
    );
  }
}

/* =========================================================
   MESSAGE ROUTER
========================================================= */

async function message(msg) {
  if (!msg?.chat || !msg?.from) {
    return;
  }

  const chatId =
    msg.chat.id;

  const id =
    msg.from.id;

  const text =
    String(msg.text || '').trim();

  console.log(
    `Telegram ${id}: ${text}`
  );

  ensureUser(msg.from);

  if (
    await sessionMessage(msg)
  ) {
    return;
  }

  if (text === '/start') {
    return start(
      chatId,
      msg.from
    );
  }

  if (text === '/help') {
    return help(
      chatId,
      id
    );
  }

  if (text === '/admin') {
    return admin(
      chatId,
      id
    );
  }

  switch (text) {
    case '🔎 البحث عن رحلة':
      return searchStart(
        chatId,
        id
      );

    case '📦 طلب رحلة':
      return requestStart(
        chatId,
        id
      );

    case '🚗 نشر رحلة':
      return publishStart(
        chatId,
        id
      );

    case '📋 رحلاتي':
      return myRides(
        chatId,
        id
      );

    case '💬 الرسائل':
      return userMessages(
        chatId,
        id
      );

    case '🔔 الإشعارات':
      return userNotifications(
        chatId,
        id
      );

    case '💰 المحفظة':
      return wallet(
        chatId,
        id
      );

    case '🎁 دعوة صديق':
      return referral(
        chatId,
        id
      );

    case '👤 حسابي':
      return account(
        chatId,
        id
      );

    case '⚙️ الإعدادات':
      return send(
        chatId,
        `<b>⚙️ الإعدادات</b>

🌐 اللغة: العربية
🌙 المظهر: Telegram`,
        mainKeyboard(id)
      );

    case '🆘 المساعدة':
      return help(
        chatId,
        id
      );

    case '👨‍💼 لوحة الإدارة':
      return admin(
        chatId,
        id
      );

    default:
      return send(
        chatId,
        'اختر عملية من القائمة:',
        mainKeyboard(id)
      );
  }
}

/* =========================================================
   POLLING
========================================================= */

async function run() {
  console.log(
    '=========================================='
  );

  console.log(
    '       WASALNI TELEGRAM BOT'
  );

  console.log(
    '=========================================='
  );

  const tables =
    db.prepare(`
      SELECT table_name AS name
      FROM information_schema.tables
      WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
      ORDER BY table_name
    `).all();

  console.log(
    '📋 Tables:',
    tables.map(x => x.name).join(', ')
  );

  const me =
    await tg('getMe');

  console.log(
    `✅ Telegram: @${me.username || me.first_name}`
  );

  console.log(
    `👨‍💼 Admin IDs: ${ADMIN_IDS.size}`
  );

  console.log(
    '🚀 Telegram bot is running.'
  );

  let offset = 0;

  while (true) {
    try {
      const updates =
        await tg(
          'getUpdates',
          {
            offset,
            timeout: 30,
            allowed_updates: [
              'message',
              'callback_query'
            ]
          }
        );

      for (const update of updates) {
        offset =
          update.update_id + 1;

        if (update.message) {
          try {
            await message(
              update.message
            );
          } catch (error) {
            console.error(
              'Message error:',
              error
            );
          }
        }

        if (update.callback_query) {
          try {
            await callback(
              update.callback_query
            );
          } catch (error) {
            console.error(
              'Callback error:',
              error
            );
          }
        }
      }
    } catch (error) {
      console.error(
        'Telegram error:',
        error.message
      );

      await new Promise(
        resolve =>
          setTimeout(resolve, 5000)
      );
    }
  }
}

/* =========================================================
   SHUTDOWN
========================================================= */

process.on(
  'SIGINT',
  () => {
    try {
      db.close();
    } catch {}

    process.exit(0);
  }
);

process.on(
  'SIGTERM',
  () => {
    try {
      db.close();
    } catch {}

    process.exit(0);
  }
);

/* =========================================================
   RUN
========================================================= */

run().catch(error => {
  console.error(error);

  try {
    db.close();
  } catch {}

  process.exit(1);
});
