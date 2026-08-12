#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND="$ROOT/backend"
BOT="$BACKEND/src/bot.js"
ENV="$BACKEND/.env"
DB="$BACKEND/data/wasalni.db"

echo "================================================="
echo "      WASALNI TELEGRAM FULL INTEGRATION"
echo "================================================="

if [ ! -d "$BACKEND" ]; then
  echo "❌ backend directory not found."
  exit 1
fi

if [ ! -f "$ENV" ]; then
  echo "❌ backend/.env not found."
  echo "Run the backend setup first."
  exit 1
fi

if [ ! -f "$DB" ]; then
  echo "❌ Database not found: $DB"
  exit 1
fi

mkdir -p "$BACKEND/src"

if [ -f "$BOT" ]; then
  cp "$BOT" "$BOT.backup.$(date +%Y%m%d_%H%M%S)"
  echo "✅ Existing bot backed up."
fi

cat > "$BOT" <<'NODE'
'use strict';

/*
 * WASALNI TELEGRAM BRIDGE
 *
 * Telegram is treated as a second client for the Wasalni platform.
 * The bot uses the same SQLite database and does not contain a Telegram
 * token in source code.
 */

require('dotenv').config();

const Database = require('better-sqlite3');
const readline = require('readline');

const TOKEN =
  process.env.TELEGRAM_BOT_TOKEN ||
  process.env.BOT_TOKEN ||
  process.env.TELEGRAM_TOKEN;

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

const DB_PATH =
  process.env.DATABASE_PATH ||
  require('path').join(__dirname, '..', 'data', 'wasalni.db');

if (!TOKEN) {
  console.error('❌ Telegram token is missing from backend/.env');
  console.error('Expected TELEGRAM_BOT_TOKEN=...');
  process.exit(1);
}

const db = new Database(DB_PATH);
db.pragma('journal_mode = WAL');

const API = `https://api.telegram.org/bot${TOKEN}`;

const sessions = new Map();

function isAdmin(id) {
  return ADMIN_IDS.has(String(id));
}

async function telegram(method, body = {}) {
  const response = await fetch(`${API}/${method}`, {
    method: 'POST',
    headers: {'content-type': 'application/json'},
    body: JSON.stringify(body)
  });

  const data = await response.json();

  if (!data.ok) {
    throw new Error(data.description || `Telegram ${method} failed`);
  }

  return data.result;
}

async function send(chatId, text, keyboard = null, options = {}) {
  const body = {
    chat_id: chatId,
    text,
    parse_mode: options.parse_mode || 'HTML',
    disable_web_page_preview: true
  };

  if (keyboard) {
    body.reply_markup = keyboard;
  }

  return telegram('sendMessage', body);
}

async function edit(chatId, messageId, text, keyboard = null) {
  const body = {
    chat_id: chatId,
    message_id: messageId,
    text,
    parse_mode: 'HTML'
  };

  if (keyboard) body.reply_markup = keyboard;

  return telegram('editMessageText', body);
}

function kb(rows) {
  return {
    keyboard: rows,
    resize_keyboard: true,
    is_persistent: true
  };
}

function inline(rows) {
  return {
    inline_keyboard: rows
  };
}

function tables() {
  return db.prepare(`
    SELECT name
    FROM sqlite_master
    WHERE type='table'
      AND name NOT LIKE 'sqlite_%'
    ORDER BY name
  `).all().map(x => x.name);
}

function columns(table) {
  try {
    return db.prepare(`PRAGMA table_info("${table.replace(/"/g, '""')}")`).all();
  } catch {
    return [];
  }
}

function findTable(patterns) {
  const ts = tables();

  for (const pattern of patterns) {
    const exact = ts.find(
      t => t.toLowerCase() === pattern.toLowerCase()
    );
    if (exact) return exact;
  }

  for (const pattern of patterns) {
    const found = ts.find(
      t => t.toLowerCase().includes(pattern.toLowerCase())
    );
    if (found) return found;
  }

  return null;
}

function findColumn(table, candidates) {
  if (!table) return null;

  const cols = columns(table).map(x => x.name);

  for (const candidate of candidates) {
    const exact = cols.find(
      c => c.toLowerCase() === candidate.toLowerCase()
    );
    if (exact) return exact;
  }

  for (const candidate of candidates) {
    const partial = cols.find(
      c => c.toLowerCase().includes(candidate.toLowerCase())
    );
    if (partial) return partial;
  }

  return null;
}

function safeSelectCount(table) {
  if (!table) return 0;

  try {
    return db
      .prepare(`SELECT COUNT(*) AS count FROM "${table.replace(/"/g, '""')}"`)
      .get().count;
  } catch {
    return 0;
  }
}

function detectSchema() {
  const result = {};

  result.users = findTable([
    'users',
    'user',
    'accounts',
    'customers'
  ]);

  result.rides = findTable([
    'rides',
    'ride',
    'trips',
    'trip'
  ]);

  result.bookings = findTable([
    'bookings',
    'booking',
    'reservations',
    'reservation'
  ]);

  result.requests = findTable([
    'ride_requests',
    'requests',
    'trip_requests',
    'special_requests'
  ]);

  result.topups = findTable([
    'topups',
    'top_up_requests',
    'wallet_topups',
    'wallet_requests'
  ]);

  result.transactions = findTable([
    'transactions',
    'wallet_transactions',
    'point_transactions'
  ]);

  result.messages = findTable([
    'messages',
    'chat_messages'
  ]);

  result.ratings = findTable([
    'ratings',
    'reviews',
    'rating_reviews'
  ]);

  result.banners = findTable([
    'banners',
    'ads',
    'advertisements'
  ]);

  result.promos = findTable([
    'promo_codes',
    'promocodes',
    'coupons',
    'discount_codes'
  ]);

  result.notifications = findTable([
    'notifications',
    'user_notifications'
  ]);

  return result;
}

const schema = detectSchema();

function userByTelegram(id) {
  if (!schema.users) return null;

  const tg = findColumn(schema.users, [
    'telegram_id',
    'telegramId',
    'tg_id',
    'tgId'
  ]);

  if (!tg) return null;

  try {
    return db.prepare(
      `SELECT * FROM "${schema.users}" WHERE "${tg}" = ? LIMIT 1`
    ).get(String(id));
  } catch {
    return null;
  }
}

function userById(id) {
  if (!schema.users) return null;

  const idCol = findColumn(schema.users, [
    'id',
    'user_id',
    'userId'
  ]);

  if (!idCol) return null;

  try {
    return db.prepare(
      `SELECT * FROM "${schema.users}" WHERE "${idCol}" = ? LIMIT 1`
    ).get(id);
  } catch {
    return null;
  }
}

function userDisplay(user) {
  if (!user) return 'غير مسجل';

  return (
    user.name ||
    user.full_name ||
    user.fullName ||
    user.username ||
    user.email ||
    `#${user.id ?? ''}`
  );
}

function userPoints(user) {
  if (!user) return 0;

  return Number(
    user.points ??
    user.balance ??
    user.wallet_points ??
    user.walletPoints ??
    0
  );
}

function mainKeyboard(userId) {
  const rows = [
    ['🔎 البحث عن رحلة', '📦 طلب رحلة'],
    ['🚗 نشر رحلة', '📋 رحلاتي'],
    ['💬 الرسائل', '💰 المحفظة'],
    ['🎁 دعوة صديق', '👤 حسابي'],
    ['⚙️ الإعدادات', '🆘 المساعدة']
  ];

  if (isAdmin(userId)) {
    rows.push(['👨‍💼 لوحة الإدارة']);
  }

  return kb(rows);
}

function adminKeyboard() {
  return inline([
    [
      {text: '📊 الإحصائيات', callback_data: 'admin_stats'},
      {text: '👥 المستخدمون', callback_data: 'admin_users'}
    ],
    [
      {text: '🚗 الرحلات', callback_data: 'admin_rides'},
      {text: '🎫 الحجوزات', callback_data: 'admin_bookings'}
    ],
    [
      {text: '📦 الطلبات', callback_data: 'admin_requests'},
      {text: '💳 الشحن', callback_data: 'admin_topups'}
    ],
    [
      {text: '📢 الإعلانات', callback_data: 'admin_banners'},
      {text: '🎟️ أكواد الخصم', callback_data: 'admin_promos'}
    ],
    [
      {text: '🔔 الإشعارات', callback_data: 'admin_notifications'},
      {text: '🗂️ قاعدة البيانات', callback_data: 'admin_schema'}
    ]
  ]);
}

async function start(chatId, from) {
  const user = userByTelegram(from.id);

  const text =
`<b>🚗 أهلاً بك في وصلني</b>

منصة ذكية للرحلات التشاركية ونقل الركاب والطلبات الخاصة.

يمكنك استخدام Telegram للوصول إلى خدمات وصلني من نفس قاعدة البيانات.

اختر العملية التي تريدها من القائمة بالأسفل.`;

  await send(chatId, text, mainKeyboard(from.id));
}

async function help(chatId, userId) {
  let text =
`<b>🆘 مساعدة وصلني</b>

🔎 <b>البحث عن رحلة</b>
البحث حسب مدينة الانطلاق والوصول والتاريخ وعدد المقاعد.

🚗 <b>نشر رحلة</b>
إنشاء رحلة كسائق مع السعر والمقاعد والوقت ونقطة التجمع.

📦 <b>طلب رحلة</b>
إنشاء طلب خاص عندما لا تجد رحلة مناسبة.

📋 <b>رحلاتي</b>
متابعة الرحلات والحجوزات والرحلات المنشورة.

💰 <b>المحفظة</b>
عرض النقاط وطلبات الشحن وسجل العمليات.

🎁 <b>دعوة صديق</b>
الحصول على نقاط الإحالة عند تسجيل مستخدم جديد.

💬 <b>الرسائل</b>
الوصول إلى المحادثات المتاحة.

⭐ <b>التقييمات</b>
تقييم السائق أو الراكب بعد الرحلة.

⚙️ <b>الإعدادات</b>
اللغة والوضع والتفضيلات.

استخدم الأزرار بالأسفل للوصول إلى الوظائف.`;

  if (isAdmin(userId)) {
    text += `

<b>👨‍💼 وضع الإدارة</b>
لديك صلاحية الوصول إلى لوحة الإدارة.`;
  }

  await send(chatId, text, mainKeyboard(userId));
}

async function account(chatId, userId) {
  const user = userByTelegram(userId);

  if (!user) {
    await send(
      chatId,
      `<b>👤 حسابك</b>

لم يتم ربط حساب Telegram بحساب وصلني بعد.

سيتم استخدام Telegram ID لربط الحساب عند أول عملية تسجيل.`
    );
    return;
  }

  const rating =
    user.rating ??
    user.average_rating ??
    user.avg_rating ??
    '—';

  await send(
    chatId,
`<b>👤 حسابي</b>

الاسم: <b>${escapeHtml(userDisplay(user))}</b>
Telegram ID: <code>${userId}</code>
النقاط: <b>${userPoints(user)}</b>
التقييم: <b>${escapeHtml(String(rating))}</b>

يمكنك استخدام القائمة الرئيسية للوصول إلى باقي خدماتك.`,
    mainKeyboard(userId)
  );
}

async function wallet(chatId, userId) {
  const user = userByTelegram(userId);

  if (!user) {
    await send(chatId, '⚠️ يجب ربط حسابك أولاً.');
    return;
  }

  const topupTable = schema.topups;

  let pending = 0;

  if (topupTable) {
    const userCol = findColumn(topupTable, [
      'user_id',
      'userId',
      'telegram_id',
      'telegramId'
    ]);

    const statusCol = findColumn(topupTable, [
      'status',
      'state'
    ]);

    if (userCol) {
      try {
        if (statusCol) {
          pending = db.prepare(
            `SELECT COUNT(*) AS count
             FROM "${topupTable}"
             WHERE "${userCol}" = ?
             AND LOWER("${statusCol}") IN ('pending','pending_approval','waiting')`
          ).get(user.id ?? userId).count;
        } else {
          pending = db.prepare(
            `SELECT COUNT(*) AS count
             FROM "${topupTable}"
             WHERE "${userCol}" = ?`
          ).get(user.id ?? userId).count;
        }
      } catch {}
    }
  }

  await send(
    chatId,
`<b>💰 محفظتي</b>

رصيد النقاط: <b>${userPoints(user)}</b>
طلبات الشحن المعلقة: <b>${pending}</b>

اختر العملية:`,
    inline([
      [
        {text: '💳 طلب شحن', callback_data: 'wallet_topup'},
        {text: '📜 سجل العمليات', callback_data: 'wallet_history'}
      ],
      [
        {text: '🎁 دعوة صديق', callback_data: 'referral'}
      ]
    ])
  );
}

async function stats(chatId) {
  if (!isAdmin(chatId)) {
    await send(chatId, '⛔ ليس لديك صلاحية.');
    return;
  }

  const stats = [
    ['👥 المستخدمون', safeSelectCount(schema.users)],
    ['🚗 الرحلات', safeSelectCount(schema.rides)],
    ['🎫 الحجوزات', safeSelectCount(schema.bookings)],
    ['📦 الطلبات', safeSelectCount(schema.requests)],
    ['💳 طلبات الشحن', safeSelectCount(schema.topups)],
    ['💬 الرسائل', safeSelectCount(schema.messages)],
    ['⭐ التقييمات', safeSelectCount(schema.ratings)]
  ];

  let points = 0;

  if (schema.users) {
    const pc = findColumn(schema.users, [
      'points',
      'balance',
      'wallet_points',
      'walletPoints'
    ]);

    if (pc) {
      try {
        points = db.prepare(
          `SELECT COALESCE(SUM("${pc}"),0) AS total FROM "${schema.users}"`
        ).get().total;
      } catch {}
    }
  }

  let text = `<b>📊 Wasalni Admin</b>\n\n`;

  for (const [label, value] of stats) {
    text += `${label}: <b>${value}</b>\n`;
  }

  text += `💰 مجموع النقاط: <b>${points}</b>`;

  await send(chatId, text, adminKeyboard());
}

async function adminUsers(chatId) {
  if (!isAdmin(chatId)) {
    await send(chatId, '⛔ ليس لديك صلاحية.');
    return;
  }

  if (!schema.users) {
    await send(chatId, '⚠️ جدول المستخدمين غير موجود.');
    return;
  }

  const rows = db.prepare(
    `SELECT * FROM "${schema.users}" ORDER BY rowid DESC LIMIT 20`
  ).all();

  if (!rows.length) {
    await send(chatId, '👥 لا يوجد مستخدمون.');
    return;
  }

  let text = `<b>👥 آخر المستخدمين</b>\n\n`;

  for (const u of rows) {
    const id = u.id ?? u.user_id ?? '';
    const name = escapeHtml(userDisplay(u));
    const points = userPoints(u);

    text += `👤 <b>${name}</b>\n`;
    text += `ID: <code>${id}</code>\n`;
    text += `💰 ${points} نقطة\n\n`;
  }

  await send(chatId, text, adminKeyboard());
}

async function adminRides(chatId) {
  if (!isAdmin(chatId)) {
    await send(chatId, '⛔ ليس لديك صلاحية.');
    return;
  }

  if (!schema.rides) {
    await send(chatId, '⚠️ جدول الرحلات غير موجود.');
    return;
  }

  const rows = db.prepare(
    `SELECT * FROM "${schema.rides}" ORDER BY rowid DESC LIMIT 15`
  ).all();

  if (!rows.length) {
    await send(chatId, '🚗 لا توجد رحلات.');
    return;
  }

  let text = `<b>🚗 آخر الرحلات</b>\n\n`;

  for (const r of rows) {
    const id = r.id ?? r.ride_id ?? '';
    const from = r.from_city ?? r.from ?? r.origin ?? r.start_city ?? '—';
    const to = r.to_city ?? r.to ?? r.destination ?? r.end_city ?? '—';
    const date = r.date ?? r.departure_date ?? '—';
    const status = r.status ?? '—';

    text +=
      `🚗 <b>#${id}</b>\n` +
      `📍 ${escapeHtml(String(from))} → ${escapeHtml(String(to))}\n` +
      `📅 ${escapeHtml(String(date))}\n` +
      `🔹 ${escapeHtml(String(status))}\n\n`;
  }

  await send(chatId, text, adminKeyboard());
}

async function adminBookings(chatId) {
  if (!isAdmin(chatId)) {
    await send(chatId, '⛔ ليس لديك صلاحية.');
    return;
  }

  if (!schema.bookings) {
    await send(chatId, '⚠️ جدول الحجوزات غير موجود.');
    return;
  }

  const rows = db.prepare(
    `SELECT * FROM "${schema.bookings}" ORDER BY rowid DESC LIMIT 15`
  ).all();

  let text = `<b>🎫 آخر الحجوزات</b>\n\n`;

  if (!rows.length) {
    text += 'لا توجد حجوزات.';
  } else {
    for (const r of rows) {
      text +=
        `🎫 #${r.id ?? r.booking_id ?? ''}\n` +
        `👤 ${r.user_id ?? r.userId ?? '—'}\n` +
        `🚗 ${r.ride_id ?? r.rideId ?? '—'}\n` +
        `🔹 ${r.status ?? '—'}\n\n`;
    }
  }

  await send(chatId, text, adminKeyboard());
}

async function adminTopups(chatId) {
  if (!isAdmin(chatId)) {
    await send(chatId, '⛔ ليس لديك صلاحية.');
    return;
  }

  if (!schema.topups) {
    await send(chatId, '⚠️ جدول طلبات الشحن غير موجود.');
    return;
  }

  const rows = db.prepare(
    `SELECT * FROM "${schema.topups}" ORDER BY rowid DESC LIMIT 20`
  ).all();

  if (!rows.length) {
    await send(chatId, '💳 لا توجد طلبات شحن.');
    return;
  }

  let text = `<b>💳 طلبات الشحن</b>\n\n`;

  for (const r of rows) {
    text +=
      `#${r.id ?? r.topup_id ?? ''}\n` +
      `👤 ${r.user_id ?? r.userId ?? '—'}\n` +
      `💰 ${r.points ?? r.amount ?? r.value ?? '—'}\n` +
      `🔹 ${r.status ?? '—'}\n\n`;
  }

  await send(chatId, text, adminKeyboard());
}

async function adminSchema(chatId) {
  if (!isAdmin(chatId)) {
    await send(chatId, '⛔ ليس لديك صلاحية.');
    return;
  }

  let text = `<b>🗂️ Wasalni Database</b>\n\n`;

  for (const [key, value] of Object.entries(schema)) {
    text += `${key}: ${value ? `✅ <code>${escapeHtml(value)}</code>` : '❌'}\n`;
  }

  await send(chatId, text, adminKeyboard());
}

async function adminDashboard(chatId) {
  if (!isAdmin(chatId)) {
    await send(chatId, '⛔ ليس لديك صلاحية الوصول إلى لوحة الإدارة.');
    return;
  }

  await send(
    chatId,
    `<b>👨‍💼 Wasalni Admin</b>

اختر العملية الإدارية المطلوبة:`,
    adminKeyboard()
  );
}

async function referral(chatId, userId) {
  const user = userByTelegram(userId);

  const code =
    user?.referral_code ||
    user?.referralCode ||
    user?.invite_code ||
    `WASALNI${userId}`;

  await send(
    chatId,
`<b>🎁 دعوة صديق</b>

كود الإحالة الخاص بك:

<code>${escapeHtml(String(code))}</code>

عند تسجيل صديق من خلال إحالتك، يتم احتساب مكافأة الإحالة وفق إعدادات Wasalni.

يمكنك نسخ الكود ومشاركته مع أصدقائك عبر Telegram أو WhatsApp.`
  );
}

async function publishRideStart(chatId, userId) {
  sessions.set(String(userId), {
    type: 'publish',
    step: 'from',
    data: {}
  });

  await send(
    chatId,
    `<b>🚗 نشر رحلة جديدة</b>

أرسل مدينة الانطلاق:`
  );
}

async function requestRideStart(chatId, userId) {
  sessions.set(String(userId), {
    type: 'request',
    step: 'from',
    data: {}
  });

  await send(
    chatId,
    `<b>📦 طلب رحلة</b>

أرسل مدينة الانطلاق:`
  );
}

async function searchRideStart(chatId, userId) {
  sessions.set(String(userId), {
    type: 'search',
    step: 'from',
    data: {}
  });

  await send(
    chatId,
    `<b>🔎 البحث عن رحلة</b>

أرسل مدينة الانطلاق:`
  );
}

async function processSession(message) {
  const chatId = message.chat.id;
  const userId = message.from.id;
  const key = String(userId);

  const session = sessions.get(key);

  if (!session) return false;

  const value = String(message.text || '').trim();

  if (value === '❌ إلغاء') {
    sessions.delete(key);
    await send(chatId, 'تم إلغاء العملية.', mainKeyboard(userId));
    return true;
  }

  if (session.type === 'search') {
    if (session.step === 'from') {
      session.data.from = value;
      session.step = 'to';
      await send(chatId, 'أرسل مدينة الوصول:');
      return true;
    }

    if (session.step === 'to') {
      session.data.to = value;
      session.step = 'date';
      await send(chatId, 'أرسل تاريخ السفر، مثال: 2026-08-20');
      return true;
    }

    if (session.step === 'date') {
      session.data.date = value;
      session.step = 'seats';
      await send(chatId, 'كم مقعدًا تريد؟');
      return true;
    }

    if (session.step === 'seats') {
      session.data.seats = Number(value) || 1;

      const rideTable = schema.rides;

      if (!rideTable) {
        await send(chatId, '⚠️ جدول الرحلات غير موجود في قاعدة البيانات.');
        sessions.delete(key);
        return true;
      }

      const fromCol = findColumn(rideTable, [
        'from_city','from','origin','start_city','origin_city'
      ]);

      const toCol = findColumn(rideTable, [
        'to_city','to','destination','end_city','destination_city'
      ]);

      const dateCol = findColumn(rideTable, [
        'date','departure_date','travel_date'
      ]);

      if (!fromCol || !toCol) {
        await send(
          chatId,
          '⚠️ قاعدة البيانات الحالية لا تحتوي أعمدة البحث المطلوبة. لم يتم تنفيذ أي تعديل.'
        );
        sessions.delete(key);
        return true;
      }

      try {
        let sql =
          `SELECT * FROM "${rideTable}" WHERE LOWER("${fromCol}") LIKE LOWER(?) AND LOWER("${toCol}") LIKE LOWER(?)`;

        const params = [
          `%${session.data.from}%`,
          `%${session.data.to}%`
        ];

        if (dateCol) {
          sql += ` AND "${dateCol}" LIKE ?`;
          params.push(`%${session.data.date}%`);
        }

        sql += ` LIMIT 10`;

        const rides = db.prepare(sql).all(...params);

        if (!rides.length) {
          await send(
            chatId,
            `🔎 لا توجد رحلات مطابقة حاليًا.

${session.data.from} → ${session.data.to}
📅 ${session.data.date}`,
            mainKeyboard(userId)
          );
        } else {
          let text =
            `<b>🔎 نتائج البحث</b>\n\n` +
            `${escapeHtml(session.data.from)} → ${escapeHtml(session.data.to)}\n` +
            `📅 ${escapeHtml(session.data.date)}\n\n`;

          for (const r of rides) {
            const id = r.id ?? r.ride_id ?? r.rideId ?? '';
            const price = r.price ?? r.seat_price ?? r.amount ?? '—';
            const seats = r.available_seats ?? r.seats_available ?? r.seats ?? '—';
            const time = r.time ?? r.departure_time ?? '—';

            text +=
              `🚗 <b>رحلة #${id}</b>\n` +
              `🕐 ${escapeHtml(String(time))}\n` +
              `💰 ${escapeHtml(String(price))}\n` +
              `💺 ${escapeHtml(String(seats))}\n\n`;
          }

          await send(chatId, text, mainKeyboard(userId));
        }
      } catch (error) {
        console.error(error);
        await send(chatId, '❌ حدث خطأ أثناء البحث.');
      }

      sessions.delete(key);
      return true;
    }
  }

  if (session.type === 'publish') {
    const s = session;

    if (s.step === 'from') {
      s.data.from = value;
      s.step = 'to';
      await send(chatId, 'أرسل مدينة الوصول:');
      return true;
    }

    if (s.step === 'to') {
      s.data.to = value;
      s.step = 'date';
      await send(chatId, 'أرسل تاريخ الرحلة، مثال: 2026-08-20');
      return true;
    }

    if (s.step === 'date') {
      s.data.date = value;
      s.step = 'time';
      await send(chatId, 'أرسل وقت الانطلاق، مثال: 08:30');
      return true;
    }

    if (s.step === 'time') {
      s.data.time = value;
      s.step = 'seats';
      await send(chatId, 'كم عدد المقاعد المتاحة؟');
      return true;
    }

    if (s.step === 'seats') {
      s.data.seats = Number(value) || 1;
      s.step = 'price';
      await send(chatId, 'ما سعر المقعد الواحد؟');
      return true;
    }

    if (s.step === 'price') {
      s.data.price = Number(value) || 0;
      s.step = 'meeting';
      await send(chatId, 'أرسل مكان التجمع:');
      return true;
    }

    if (s.step === 'meeting') {
      s.data.meeting = value;
      s.step = 'dropoff';
      await send(chatId, 'أرسل مكان النزول:');
      return true;
    }

    if (s.step === 'dropoff') {
      s.data.dropoff = value;
      s.step = 'notes';
      await send(
        chatId,
        'أرسل ملاحظات الرحلة، أو اكتب "لا يوجد":'
      );
      return true;
    }

    if (s.step === 'notes') {
      s.data.notes = value === 'لا يوجد' ? '' : value;

      const rideTable = schema.rides;
      const user = userByTelegram(userId);

      if (!rideTable) {
        await send(chatId, '⚠️ جدول الرحلات غير موجود.');
        sessions.delete(key);
        return true;
      }

      const cols = columns(rideTable).map(x => x.name);

      const mapping = {
        user_id: ['user_id','userId','driver_id','driverId'],
        from: ['from_city','from','origin','start_city','origin_city'],
        to: ['to_city','to','destination','end_city','destination_city'],
        date: ['date','departure_date','travel_date'],
        time: ['time','departure_time'],
        seats: ['available_seats','seats_available','seats','capacity'],
        price: ['price','seat_price','amount'],
        meeting: ['meeting_point','pickup_point','pickup','meeting'],
        dropoff: ['dropoff_point','drop_off','destination_point'],
        notes: ['notes','description','remarks']
      };

      const values = {
        user_id: user?.id ?? userId,
        from: s.data.from,
        to: s.data.to,
        date: s.data.date,
        time: s.data.time,
        seats: s.data.seats,
        price: s.data.price,
        meeting: s.data.meeting,
        dropoff: s.data.dropoff,
        notes: s.data.notes
      };

      const insertCols = [];
      const insertVals = [];

      for (const [logical, candidates] of Object.entries(mapping)) {
        const actual = cols.find(c =>
          candidates.some(x => c.toLowerCase() === x.toLowerCase())
        );

        if (actual) {
          insertCols.push(actual);
          insertVals.push(values[logical]);
        }
      }

      if (!insertCols.length) {
        await send(
          chatId,
          '⚠️ لم أجد أعمدة مناسبة لإنشاء الرحلة في قاعدة البيانات الحالية.'
        );
        sessions.delete(key);
        return true;
      }

      try {
        const placeholders = insertCols.map(() => '?').join(',');
        const quoted = insertCols
          .map(c => `"${c.replace(/"/g,'""')}"`)
          .join(',');

        const result = db.prepare(
          `INSERT INTO "${rideTable}" (${quoted}) VALUES (${placeholders})`
        ).run(...insertVals);

        await send(
          chatId,
`<b>✅ تم نشر الرحلة</b>

🚗 رقم الرحلة: <code>${result.lastInsertRowid}</code>
📍 ${escapeHtml(s.data.from)} → ${escapeHtml(s.data.to)}
📅 ${escapeHtml(s.data.date)}
🕐 ${escapeHtml(s.data.time)}
💺 ${s.data.seats}
💰 ${s.data.price}
📍 التجمع: ${escapeHtml(s.data.meeting)}
📍 النزول: ${escapeHtml(s.data.dropoff)}`,
          mainKeyboard(userId)
        );
      } catch (error) {
        console.error(error);
        await send(
          chatId,
          '❌ لم يتم نشر الرحلة لأن بنية قاعدة البيانات لا تتوافق مع الحقول المطلوبة.'
        );
      }

      sessions.delete(key);
      return true;
    }
  }

  if (session.type === 'request') {
    if (session.step === 'from') {
      session.data.from = value;
      session.step = 'to';
      await send(chatId, 'أرسل مدينة الوصول:');
      return true;
    }

    if (session.step === 'to') {
      session.data.to = value;
      session.step = 'date';
      await send(chatId, 'أرسل تاريخ الرحلة:');
      return true;
    }

    if (session.step === 'date') {
      session.data.date = value;
      session.step = 'type';
      await send(
        chatId,
        'ما نوع الطلب؟',
        kb([
          ['🚗 ركاب', '📦 طرود/بضائع'],
          ['❌ إلغاء']
        ])
      );
      return true;
    }

    if (session.step === 'type') {
      session.data.type = value;
      session.step = 'details';
      await send(chatId, 'اكتب تفاصيل الطلب:');
      return true;
    }

    if (session.step === 'details') {
      session.data.details = value;

      const table = schema.requests;
      const user = userByTelegram(userId);

      if (!table) {
        await send(
          chatId,
          '⚠️ جدول الطلبات الخاصة غير موجود في قاعدة البيانات.'
        );
        sessions.delete(key);
        return true;
      }

      const cols = columns(table).map(x => x.name);

      const mapping = [
        ['user_id', ['user_id','userId','requester_id'], user?.id ?? userId],
        ['from', ['from_city','from','origin','start_city'], session.data.from],
        ['to', ['to_city','to','destination','end_city'], session.data.to],
        ['date', ['date','travel_date','requested_date'], session.data.date],
        ['type', ['type','request_type','category'], session.data.type],
        ['details', ['details','description','notes'], session.data.details]
      ];

      const insertCols = [];
      const vals = [];

      for (const [, candidates, val] of mapping) {
        const actual = cols.find(c =>
          candidates.some(x => c.toLowerCase() === x.toLowerCase())
        );

        if (actual) {
          insertCols.push(actual);
          vals.push(val);
        }
      }

      try {
        const placeholders = insertCols.map(() => '?').join(',');
        const quoted = insertCols
          .map(c => `"${c.replace(/"/g,'""')}"`)
          .join(',');

        const result = db.prepare(
          `INSERT INTO "${table}" (${quoted}) VALUES (${placeholders})`
        ).run(...vals);

        await send(
          chatId,
`<b>✅ تم إنشاء طلبك</b>

رقم الطلب: <code>${result.lastInsertRowid}</code>
📍 ${escapeHtml(session.data.from)} → ${escapeHtml(session.data.to)}
📅 ${escapeHtml(session.data.date)}
📦 ${escapeHtml(session.data.type)}

سيظهر الطلب للسائقين حسب نظام Wasalni.`,
          mainKeyboard(userId)
        );
      } catch (error) {
        console.error(error);
        await send(chatId, '❌ تعذر إنشاء الطلب.');
      }

      sessions.delete(key);
      return true;
    }
  }

  return true;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

async function handleCallback(query) {
  const chatId = query.message.chat.id;
  const userId = query.from.id;
  const data = query.data;

  try {
    await telegram('answerCallbackQuery', {
      callback_query_id: query.id
    });
  } catch {}

  if (data.startsWith('admin_') && !isAdmin(userId)) {
    await send(chatId, '⛔ ليس لديك صلاحية.');
    return;
  }

  switch (data) {
    case 'admin_stats':
      return stats(userId);

    case 'admin_users':
      return adminUsers(userId);

    case 'admin_rides':
      return adminRides(userId);

    case 'admin_bookings':
      return adminBookings(userId);

    case 'admin_topups':
      return adminTopups(userId);

    case 'admin_requests':
      if (!schema.requests) {
        return send(chatId, '⚠️ جدول الطلبات غير موجود.', adminKeyboard());
      }
      return send(
        chatId,
        `<b>📦 الطلبات الخاصة</b>

إجمالي الطلبات: <b>${safeSelectCount(schema.requests)}</b>`,
        adminKeyboard()
      );

    case 'admin_banners':
      if (!schema.banners) {
        return send(chatId, '⚠️ جدول الإعلانات غير موجود.', adminKeyboard());
      }
      return send(
        chatId,
        `<b>📢 الإعلانات</b>

إجمالي السجلات: <b>${safeSelectCount(schema.banners)}</b>

إدارة رفع الصور والإعلانات تعتمد على API/Storage الخاص بالتطبيق.`,
        adminKeyboard()
      );

    case 'admin_promos':
      if (!schema.promos) {
        return send(chatId, '⚠️ جدول أكواد الخصم غير موجود.', adminKeyboard());
      }
      return send(
        chatId,
        `<b>🎟️ أكواد الخصم</b>

إجمالي الأكواد: <b>${safeSelectCount(schema.promos)}</b>`,
        adminKeyboard()
      );

    case 'admin_notifications':
      if (!schema.notifications) {
        return send(chatId, '⚠️ جدول الإشعارات غير موجود.', adminKeyboard());
      }
      return send(
        chatId,
        `<b>🔔 الإشعارات</b>

إجمالي الإشعارات: <b>${safeSelectCount(schema.notifications)}</b>`,
        adminKeyboard()
      );

    case 'admin_schema':
      return adminSchema(userId);

    case 'wallet_topup':
      sessions.set(String(userId), {
        type: 'topup',
        step: 'amount',
        data: {}
      });

      return send(
        chatId,
        `<b>💳 طلب شحن المحفظة</b>

أرسل عدد النقاط المطلوبة للشحن:`
      );

    case 'wallet_history':
      return send(
        chatId,
        '<b>📜 سجل العمليات</b>\n\nسيتم عرض العمليات المرتبطة بحسابك من جدول المعاملات الموجود في Wasalni.'
      );

    case 'referral':
      return referral(chatId, userId);

    default:
      return;
  }
}

async function handleMessage(message) {
  if (!message || !message.chat || !message.from) return;

  const chatId = message.chat.id;
  const userId = message.from.id;
  const text = String(message.text || '').trim();

  console.log(`📩 ${userId}: ${text}`);

  if (await processSession(message)) return;

  if (text === '/start') {
    return start(chatId, message.from);
  }

  if (text === '/help') {
    return help(chatId, userId);
  }

  if (text === '/admin') {
    return adminDashboard(chatId);
  }

  if (text === '/stats') {
    return stats(userId);
  }

  if (text === '/topups') {
    return adminTopups(userId);
  }

  switch (text) {
    case '🔎 البحث عن رحلة':
      return searchRideStart(chatId, userId);

    case '📦 طلب رحلة':
      return requestRideStart(chatId, userId);

    case '🚗 نشر رحلة':
      return publishRideStart(chatId, userId);

    case '📋 رحلاتي':
      return send(
        chatId,
        `<b>📋 رحلاتي</b>

سيتم عرض الرحلات التي يملكها حسابك والحجوزات المرتبطة به من قاعدة بيانات Wasalni.`
      );

    case '💬 الرسائل':
      return send(
        chatId,
        `<b>💬 الرسائل</b>

سيتم عرض المحادثات المرتبطة بحسابك من نظام Wasalni.`
      );

    case '💰 المحفظة':
      return wallet(chatId, userId);

    case '🎁 دعوة صديق':
      return referral(chatId, userId);

    case '👤 حسابي':
      return account(chatId, userId);

    case '⚙️ الإعدادات':
      return send(
        chatId,
        `<b>⚙️ الإعدادات</b>

🌐 اللغة: العربية
🌙 المظهر: يعتمد على Telegram

يمكن تغيير إعدادات الحساب من تطبيق Wasalni.`
      );

    case '🆘 المساعدة':
      return help(chatId, userId);

    case '👨‍💼 لوحة الإدارة':
      return adminDashboard(chatId);

    default:
      return send(
        chatId,
        'اختر إحدى العمليات من القائمة.',
        mainKeyboard(userId)
      );
  }
}

async function poll() {
  let offset = 0;

  console.log('==========================================');
  console.log('      WASALNI TELEGRAM BOT');
  console.log('==========================================');

  const me = await telegram('getMe');

  console.log(`✅ Telegram: @${me.username || me.first_name}`);
  console.log(`👨‍💼 Admin IDs: ${ADMIN_IDS.size}`);
  console.log(`🗄️ Database: ${DB_PATH}`);
  console.log('');
  console.log('Detected tables:');
  console.log(schema);
  console.log('');
  console.log('🚀 Telegram bot is running.');
  console.log('==========================================');

  while (true) {
    try {
      const updates = await telegram('getUpdates', {
        offset,
        timeout: 30,
        allowed_updates: ['message', 'callback_query']
      });

      for (const update of updates) {
        offset = update.update_id + 1;

        if (update.message) {
          await handleMessage(update.message);
        }

        if (update.callback_query) {
          await handleCallback(update.callback_query);
        }
      }
    } catch (error) {
      console.error('⚠️ Telegram polling error:', error.message);
      await new Promise(resolve => setTimeout(resolve, 5000));
    }
  }
}

process.on('SIGINT', () => {
  try { db.close(); } catch {}
  process.exit(0);
});

process.on('SIGTERM', () => {
  try { db.close(); } catch {}
  process.exit(0);
});

poll().catch(error => {
  console.error('❌ Bot startup failed:', error);
  process.exit(1);
});
NODE

echo "✅ New Telegram integration written."

echo
echo "Checking JavaScript syntax..."
node --check "$BOT"
echo "✅ JavaScript syntax is valid."

echo
echo "Checking required npm package..."
cd "$BACKEND"

if ! npm list better-sqlite3 >/dev/null 2>&1; then
  echo "Installing better-sqlite3..."
  npm install better-sqlite3
fi

if ! npm list dotenv >/dev/null 2>&1; then
  echo "Installing dotenv..."
  npm install dotenv
fi

echo
echo "================================================="
echo " DATABASE SCHEMA DETECTED"
echo "================================================="

node <<'NODE'
require('dotenv').config();
const Database = require('better-sqlite3');
const path = require('path');

const dbPath =
  process.env.DATABASE_PATH ||
  path.join(process.cwd(), 'data', 'wasalni.db');

const db = new Database(dbPath);

const tables = db.prepare(`
  SELECT name
  FROM sqlite_master
  WHERE type='table'
    AND name NOT LIKE 'sqlite_%'
  ORDER BY name
`).all();

if (!tables.length) {
  console.log('⚠️ No application tables detected.');
} else {
  for (const t of tables) {
    const cols = db.prepare(
      `PRAGMA table_info("${t.name.replace(/"/g,'""')}")`
    ).all();

    console.log(`\n📦 ${t.name}`);
    console.log(
      cols.map(c => c.name).join(', ')
    );
  }
}

db.close();
NODE

echo
echo "================================================="
echo " TELEGRAM CONFIGURATION"
echo "================================================="

if grep -Eq '^TELEGRAM_BOT_TOKEN=' "$ENV" ||
   grep -Eq '^BOT_TOKEN=' "$ENV" ||
   grep -Eq '^TELEGRAM_TOKEN=' "$ENV"; then
  echo "✅ Telegram token already exists in .env"
else
  echo "⚠️ No Telegram token found in .env."
  read -r -p "Telegram Bot Token: " TOKEN

  if [ -z "$TOKEN" ]; then
    echo "❌ Empty token."
    exit 1
  fi

  printf '\nTELEGRAM_BOT_TOKEN=%s\n' "$TOKEN" >> "$ENV"
fi

if grep -Eq '^ADMIN_TELEGRAM_IDS=' "$ENV" ||
   grep -Eq '^ADMIN_TELEGRAM_ID=' "$ENV"; then
  echo "✅ Admin Telegram ID already exists in .env"
else
  read -r -p "Admin Telegram ID: " ADMIN_ID

  if ! [[ "$ADMIN_ID" =~ ^[0-9]+$ ]]; then
    echo "❌ Admin ID must contain numbers only."
    exit 1
  fi

  printf '\nADMIN_TELEGRAM_IDS=%s\n' "$ADMIN_ID" >> "$ENV"
fi

echo
echo "Checking .gitignore..."

touch "$ROOT/.gitignore"

if ! grep -qxF 'backend/.env' "$ROOT/.gitignore"; then
  echo 'backend/.env' >> "$ROOT/.gitignore"
fi

if ! grep -qxF 'backend/data/*.db' "$ROOT/.gitignore"; then
  echo 'backend/data/*.db' >> "$ROOT/.gitignore"
fi

echo "✅ Secrets/database protected from Git."

echo
echo "================================================="
echo " TELEGRAM BOT TEST"
echo "================================================="

node "$BOT" &
BOT_PID=$!

cleanup() {
  kill "$BOT_PID" >/dev/null 2>&1 || true
}

trap cleanup EXIT INT TERM

sleep 4

if kill -0 "$BOT_PID" >/dev/null 2>&1; then
  echo "✅ Telegram bot started successfully."
else
  echo "❌ Telegram bot failed to start."
  exit 1
fi

echo
echo "================================================="
echo "          WASALNI TELEGRAM READY"
echo "================================================="
echo
echo "Bot:"
echo "  backend/src/bot.js"
echo
echo "Database:"
echo "  backend/data/wasalni.db"
echo
echo "Admin:"
echo "  /admin"
echo
echo "Important:"
echo "  The bot uses the same SQLite database."
echo "  Admin controls are protected by Telegram ID."
echo "  /admin is NOT displayed to normal users."
echo
echo "Stop this test with CTRL+C."
echo
