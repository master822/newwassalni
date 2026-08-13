const express = require('express');
const router = express.Router();
const db = require('../database');
const bcrypt = require('bcryptjs');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

/**
 * 1. Get current authenticated user profile
 */
router.get('/profile', authenticateToken, async (req, res) => {
  try {
    const result = await db.query(
      `SELECT id, name, email, phone, avatar_url, rating, ride_count, is_verified,
              wallet_points, is_suspended, suspend_reason, role, user_role, referral_code, created_at
       FROM users WHERE id = $1`,
      [req.user.userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }

    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error fetching profile:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 2. Update user profile (Name, Avatar, Phone)
 */
router.put('/profile', authenticateToken, async (req, res) => {
  try {
    const { name, avatarUrl, phone } = req.body;
    const userId = req.user.userId;

    const query = `
      UPDATE users
      SET name = COALESCE($1, name),
          avatar_url = COALESCE($2, avatar_url),
          phone = COALESCE($3, phone),
          updated_at = CURRENT_TIMESTAMP
      WHERE id = $4
      RETURNING id, name, email, phone, avatar_url, rating, ride_count, is_verified, wallet_points, role, user_role, referral_code
    `;

    const result = await db.query(query, [name || null, avatarUrl || null, phone || null, userId]);
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }

    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error updating profile:', err);
    res.status(500).json({ success: false, error: 'Failed to update profile' });
  }
});

/**
 * 3. Update FCM Device Token for Push Notifications
 */
router.post('/fcm-token', authenticateToken, async (req, res) => {
  try {
    const { fcmToken } = req.body;
    if (!fcmToken) {
      return res.status(400).json({ success: false, error: 'fcmToken is required' });
    }

    await db.query('UPDATE users SET fcm_token = $1 WHERE id = $2', [fcmToken.trim(), req.user.userId]);
    res.json({ success: true, message: 'FCM token updated successfully' });
  } catch (err) {
    console.error('Error saving FCM token:', err);
    res.status(500).json({ success: false, error: 'Failed to update FCM token' });
  }
});

/**
 * 4. Account Deletion (Google Play Data Safety Requirement)
 * - Removes user personal records, revokes tokens, or anonymizes historical transactions
 */
router.delete('/me', authenticateToken, async (req, res) => {
  const client = await db.pool.connect();
  try {
    const userId = req.user.userId;
    await client.query('BEGIN');

    // Revoke all refresh tokens
    await client.query('DELETE FROM refresh_tokens WHERE user_id = $1', [userId]);

    // Anonymize/delete user profile
    await client.query('DELETE FROM users WHERE id = $1', [userId]);

    await client.query('COMMIT');
    res.json({ success: true, message: 'تم حذف الحساب وجميع البيانات الشخصية المرتبطة به بنجاح.' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Account deletion error:', err);
    res.status(500).json({ success: false, error: 'فشل في إتمام عملية حذف الحساب' });
  } finally {
    client.release();
  }
});

/**
 * 5. Get all users (Admin only)
 */
router.get('/all', authenticateToken, requireAdmin, async (req, res) => {
  try {
    const result = await db.query(
      `SELECT id, name, email, phone, avatar_url, rating, ride_count, is_verified,
              wallet_points, is_suspended, suspend_reason, role, user_role, referral_code, created_at
       FROM users ORDER BY created_at DESC`
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching all users:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

module.exports = router;
