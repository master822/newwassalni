require('dotenv').config();
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const db = require('../database');

async function bootstrapAdmin(shouldExit = false) {
  const email = process.env.SUPER_ADMIN_EMAIL;
  const password = process.env.SUPER_ADMIN_PASSWORD;
  const name = process.env.SUPER_ADMIN_NAME || 'مدير النظام الأول';
  const phone = process.env.SUPER_ADMIN_PHONE || '+963900000001';

  if (!email || !password) {
    console.warn('[BOOTSTRAP] SUPER_ADMIN_EMAIL or SUPER_ADMIN_PASSWORD not set. Skipping bootstrap.');
    if (shouldExit) process.exit(0);
    return;
  }

  if (password.length < 8) {
    console.warn('[BOOTSTRAP] SUPER_ADMIN_PASSWORD is less than 8 chars. Please use a stronger password.');
    if (shouldExit) process.exit(1);
    return;
  }

  console.log(`[BOOTSTRAP] Processing Super Admin setup for: ${email.trim().toLowerCase()}...`);

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const cleanEmail = email.trim().toLowerCase();
    const existingUser = await client.query(
      'SELECT id, name, email, role, wallet_points FROM users WHERE LOWER(email) = $1',
      [cleanEmail]
    );

    if (existingUser.rows.length > 0) {
      const user = existingUser.rows[0];
      console.log(`[BOOTSTRAP] User already exists (ID: ${user.id}, Current Role: ${user.role}). Ensuring SUPER_ADMIN role...`);
      
      const salt = await bcrypt.genSalt(12);
      const passwordHash = await bcrypt.hash(password, salt);

      await client.query(
        `UPDATE users
         SET role = 'SUPER_ADMIN',
             password_hash = $1,
             is_suspended = FALSE,
             is_verified = TRUE,
             updated_at = CURRENT_TIMESTAMP
         WHERE id = $2`,
        [passwordHash, user.id]
      );
      console.log(`[BOOTSTRAP] User ${user.id} updated and verified as SUPER_ADMIN.`);
    } else {
      const salt = await bcrypt.genSalt(12);
      const passwordHash = await bcrypt.hash(password, salt);
      const userId = `admin_${uuidv4().substring(0, 8)}`;
      const referralCode = `ADMIN-${uuidv4().substring(0, 5).toUpperCase()}`;

      // Super Admin starts with 0 points (no welcome or referral bonuses)
      await client.query(
        `INSERT INTO users 
         (id, name, email, phone, password_hash, role, user_role, wallet_points, is_verified, is_suspended, referral_code)
         VALUES ($1, $2, $3, $4, $5, 'SUPER_ADMIN', 'مدير النظام', 0, TRUE, FALSE, $6)`,
        [userId, name, cleanEmail, phone, passwordHash, referralCode]
      );

      console.log(`[BOOTSTRAP] Successfully created Super Admin account with ID: ${userId}`);
    }

    // Log admin activity
    await client.query(
      `INSERT INTO admin_activity_logs (id, admin_id, admin_name, action_type, details, ip_address)
       VALUES ($1, 'SYSTEM_BOOTSTRAP', 'System Bootstrap', 'SUPER_ADMIN_BOOTSTRAP', $2, '127.0.0.1')`,
      [uuidv4(), `Super Admin verified for ${cleanEmail}`]
    );

    await client.query('COMMIT');
    console.log('[BOOTSTRAP] Super Admin provisioning completed successfully.');
    if (shouldExit) process.exit(0);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('[BOOTSTRAP] Error during Super Admin bootstrap:', err.message);
    if (shouldExit) process.exit(1);
  } finally {
    client.release();
  }
}

if (require.main === module) {
  bootstrapAdmin(true);
}

module.exports = bootstrapAdmin;
