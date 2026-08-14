require('dotenv').config();
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const db = require('../database');

async function bootstrapAdmin() {
  const email = process.env.SUPER_ADMIN_EMAIL || process.env.ADMIN_EMAIL;
  const password = process.env.SUPER_ADMIN_PASSWORD || process.env.ADMIN_PASSWORD;
  const name = process.env.SUPER_ADMIN_NAME || 'مدير النظام الأول';
  const phone = process.env.SUPER_ADMIN_PHONE || '+963900000001';

  if (!email || !password) {
    console.error('================================================================');
    console.error('FATAL: SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD environment variables are required.');
    console.error('Please set them in your .env file or deployment environment:');
    console.error('  SUPER_ADMIN_EMAIL=admin@wassalni.sy');
    console.error('  SUPER_ADMIN_PASSWORD=your_secure_password_here');
    console.error('================================================================');
    process.exit(1);
  }

  if (password.length < 8) {
    console.error('FATAL: SUPER_ADMIN_PASSWORD must be at least 8 characters for production security.');
    process.exit(1);
  }

  console.log(`[BOOTSTRAP] Initiating Super Admin setup for: ${email}...`);

  const client = await db.pool.connect();
  try {
    await client.query('BEGIN');

    const cleanEmail = email.trim().toLowerCase();
    const existingUser = await client.query(
      'SELECT id, name, email, role FROM users WHERE LOWER(email) = $1',
      [cleanEmail]
    );

    const salt = await bcrypt.genSalt(12);
    const passwordHash = await bcrypt.hash(password, salt);

    if (existingUser.rows.length > 0) {
      const user = existingUser.rows[0];
      console.log(`[BOOTSTRAP] User ${cleanEmail} already exists (Current Role: ${user.role}). Updating role to SUPER_ADMIN...`);
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
      console.log(`[BOOTSTRAP] Successfully upgraded user ${user.id} to SUPER_ADMIN.`);
    } else {
      const userId = `admin_${uuidv4().substring(0, 8)}`;
      const referralCode = `ADMIN-${uuidv4().substring(0, 5).toUpperCase()}`;

      await client.query(
        `INSERT INTO users 
         (id, name, email, phone, password_hash, role, user_role, wallet_points, is_verified, is_suspended, referral_code)
         VALUES ($1, $2, $3, $4, $5, 'SUPER_ADMIN', 'مدير النظام', 1000, TRUE, FALSE, $6)`,
        [userId, name, cleanEmail, phone, passwordHash, referralCode]
      );

      // Record initial wallet ledger
      await client.query(
        `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
         VALUES ($1, $2, 'ADMIN_ADJUSTMENT', 1000, 100.0, 'Initial Super Admin Provisioning', 'COMPLETED')`,
        [uuidv4(), userId]
      );

      console.log(`[BOOTSTRAP] Successfully created Super Admin account with ID: ${userId}`);
    }

    // Log admin activity
    await client.query(
      `INSERT INTO admin_activity_logs (id, admin_id, admin_name, action_type, details, ip_address)
       VALUES ($1, 'SYSTEM_BOOTSTRAP', 'System Bootstrap CLI', 'SUPER_ADMIN_BOOTSTRAP', $2, '127.0.0.1')`,
      [uuidv4(), `Super Admin bootstrapped for ${cleanEmail}`]
    );

    await client.query('COMMIT');
    console.log('[BOOTSTRAP] Super Admin provisioning completed successfully.');
    process.exit(0);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('[BOOTSTRAP] Error during Super Admin bootstrap:', err);
    process.exit(1);
  } finally {
    client.release();
  }
}

bootstrapAdmin();
