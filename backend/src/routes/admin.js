const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');

// 1. Get all topup requests (Admin)
router.get('/topup-requests', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM topup_requests ORDER BY created_at DESC');
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Admin fetch topup requests error:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

// 2. Approve Topup Request
router.post('/topup-requests/:id/approve', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    await client.query('BEGIN');

    const reqRes = await client.query('SELECT * FROM topup_requests WHERE id = $1 FOR UPDATE', [id]);
    if (reqRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'Request not found' });
    }

    const topup = reqRes.rows[0];
    if (topup.status !== 'PENDING') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'Request is already processed' });
    }

    // Update status
    await client.query("UPDATE topup_requests SET status = 'APPROVED' WHERE id = $1", [id]);

    // Add wallet points
    await client.query('UPDATE users SET wallet_points = wallet_points + $1 WHERE id = $2', [
      topup.package_points,
      topup.user_id,
    ]);

    // Record wallet transaction
    await client.query(
      `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
       VALUES ($1, $2, 'TOP_UP', $3, $4, 'شحن محفظة عبر شام كاش (تأكيد الأدمن)', 'COMPLETED')`,
      [uuidv4(), topup.user_id, topup.package_points, topup.package_price_usd]
    );

    // Notification
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'تمت الموافقة على طلب الشحن!', $3, 'APPROVAL')`,
      [
        uuidv4(),
        topup.user_id,
        `تمت الموافقة على طلبك وإضافة ${topup.package_points} نقطة بنجاح إلى محفظتك.`,
      ]
    );

    // Admin activity log
    await client.query(
      'INSERT INTO admin_activity_logs (id, action_type, details) VALUES ($1, $2, $3)',
      [
        uuidv4(),
        'موافقة شحن نقاط',
        `الموافقة على شحن ${topup.package_points} نقطة للمستخدم ${topup.user_name}`,
      ]
    );

    await client.query('COMMIT');
    res.json({ success: true, message: 'Topup request approved and points credited' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Approve topup error:', err);
    res.status(500).json({ success: false, error: 'Transaction failed' });
  } finally {
    client.release();
  }
});

// 3. Reject Topup Request
router.post('/topup-requests/:id/reject', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { reason = 'إيصال غير صالح أو غير مكتمل' } = req.body;

    await client.query('BEGIN');
    const reqRes = await client.query('SELECT * FROM topup_requests WHERE id = $1 FOR UPDATE', [id]);
    if (reqRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'Request not found' });
    }

    const topup = reqRes.rows[0];
    await client.query(
      "UPDATE topup_requests SET status = 'REJECTED', rejection_reason = $1 WHERE id = $2",
      [reason, id]
    );

    // Notification
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'تم رفض طلب الشحن', $3, 'SYSTEM')`,
      [
        uuidv4(),
        topup.user_id,
        `تم رفض طلب شراء النقاط الخاص بك. السبب: ${reason}. يرجى التواصل مع الدعم.`,
      ]
    );

    await client.query(
      'INSERT INTO admin_activity_logs (id, action_type, details) VALUES ($1, $2, $3)',
      [uuidv4(), 'رفض شحن نقاط', `رفض طلب ${topup.user_name} بسبب: ${reason}`]
    );

    await client.query('COMMIT');
    res.json({ success: true, message: 'Topup request rejected' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Reject topup error:', err);
    res.status(500).json({ success: false, error: 'Transaction failed' });
  } finally {
    client.release();
  }
});

// 4. Suspend/Unsuspend User
router.post('/users/:id/toggle-suspend', async (req, res) => {
  try {
    const { id } = req.params;
    const { reason } = req.body;

    const userRes = await db.query('SELECT is_suspended, name FROM users WHERE id = $1', [id]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'User not found' });
    }

    const currentStatus = userRes.rows[0].is_suspended;
    const nextStatus = !currentStatus;

    await db.query(
      'UPDATE users SET is_suspended = $1, suspend_reason = $2 WHERE id = $3',
      [nextStatus, nextStatus ? reason || 'مخالفة الشروط والأحكام' : null, id]
    );

    await db.query(
      'INSERT INTO admin_activity_logs (id, action_type, details) VALUES ($1, $2, $3)',
      [
        uuidv4(),
        nextStatus ? 'حظر مستخدم' : 'فك حظر مستخدم',
        `${nextStatus ? 'حظر' : 'فك حظر'} المستخدم ${userRes.rows[0].name}`,
      ]
    );

    res.json({ success: true, isSuspended: nextStatus });
  } catch (err) {
    console.error('Toggle suspend user error:', err);
    res.status(500).json({ success: false, error: 'Failed to update user suspension' });
  }
});

module.exports = router;
