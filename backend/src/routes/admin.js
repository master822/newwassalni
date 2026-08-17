const express = require('express');
const router = express.Router();
const db = require('../database');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const {
  JWT_SECRET,
  authenticateToken,
  requireAdmin,
  requireSuperAdmin,
} = require('../middleware/auth');

// Protect all admin routes
router.use(authenticateToken);
router.use(requireAdmin);

/**
 * Helper: Log admin activity to database
 */
async function logAdminActivity(clientOrDb, adminId, adminName, actionType, details, targetId = null, ip = null) {
  const query = `
    INSERT INTO admin_activity_logs (id, admin_id, admin_name, action_type, target_id, details, ip_address)
    VALUES ($1, $2, $3, $4, $5, $6, $7)
  `;
  const executor = clientOrDb.query ? clientOrDb : db;
  await executor.query(query, [
    uuidv4(),
    adminId,
    adminName || 'مدير النظام',
    actionType,
    targetId,
    details,
    ip || '127.0.0.1',
  ]);
}

/**
 * 1. Get All Users (with Search and Filters)
 */
router.get('/users', async (req, res) => {
  try {
    const { search, role, suspended } = req.query;
    let query = `
      SELECT id, name, email, phone, avatar_url, rating, ride_count, is_verified,
             wallet_points, is_suspended, suspend_reason, role, user_role, referral_code, created_at
      FROM users WHERE 1=1
    `;
    const params = [];

    if (search && search.trim()) {
      params.push(`%${search.trim()}%`);
      query += ` AND (name ILIKE $${params.length} OR email ILIKE $${params.length} OR phone ILIKE $${params.length} OR referral_code ILIKE $${params.length})`;
    }
    if (role && role.trim()) {
      params.push(role.trim());
      query += ` AND role = $${params.length}`;
    }
    if (suspended === 'true') {
      query += ` AND is_suspended = TRUE`;
    } else if (suspended === 'false') {
      query += ` AND is_suspended = FALSE`;
    }

    query += ' ORDER BY created_at DESC';
    const result = await db.query(query, params);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching admin users:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 2. Edit User details
 */
router.put('/users/:id', async (req, res) => {
  const { id } = req.params;
  const { name, email, phone, role, isVerified, userRole, walletPoints, wallet_points } = req.body;
  
  const client = await db.getClient();

  try {
    await client.query('BEGIN');

    // 1. جلب البيانات الحالية للمستخدم لمعرفة النقاط القديمة
    const oldUserRes = await client.query('SELECT wallet_points FROM users WHERE id = $1', [id]);
    if (oldUserRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }
    const currentWalletPoints = parseInt(oldUserRes.rows[0].wallet_points, 10) || 0;

    // 2. التحقق من وجود حقل النقاط في الطلب وحساب الفرق
    const requestedPoints = walletPoints !== undefined ? walletPoints : wallet_points;
    if (requestedPoints !== undefined && requestedPoints !== null) {
      const newTargetPoints = parseInt(requestedPoints, 10);
      
      if (!isNaN(newTargetPoints) && newTargetPoints !== currentWalletPoints) {
        const difference = newTargetPoints - currentWalletPoints;
        const type = difference > 0 ? 'ADD' : 'DEDUCT';
        const pointsToAdjust = Math.abs(difference);

        // أ) تحديث النقاط في جدول المستخدمين
        await client.query(
          'UPDATE users SET wallet_points = $1 WHERE id = $2',
          [newTargetPoints, id]
        );

        // ب) تسجيل المعاملة المالية في دفتر الحسابات wallet_transactions
        await client.query(
          `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status, created_at)
           VALUES ($1, $2, $3, $4, $5, $6, 'COMPLETED', CURRENT_TIMESTAMP)`,
          [
            require('crypto').randomUUID(),
            id,
            type,
            pointsToAdjust,
            pointsToAdjust / 10.0,
            'تعديل إداري تلقائي عبر تحديث الملف الشخصي'
          ]
        );
      }
    }

    // 3. تحديث باقي بيانات المستخدم الشخصية
    const query = `
      UPDATE users
      SET name = COALESCE($1, name),
          email = COALESCE($2, email),
          phone = COALESCE($3, phone),
          role = COALESCE($4, role),
          is_verified = COALESCE($5, is_verified),
          user_role = COALESCE($6, user_role),
          updated_at = CURRENT_TIMESTAMP
      WHERE id = $7
      RETURNING id, name, email, phone, role, is_verified, user_role, wallet_points
    `;
    
    const result = await client.query(query, [
      name || null,
      email ? email.trim().toLowerCase() : null,
      phone ? phone.trim() : null,
      role || null,
      isVerified !== undefined ? isVerified : null,
      userRole || null,
      id
    ]);

    await client.query('COMMIT');

    return res.json({
      success: true,
      message: 'تم تحديث بيانات المستخدم والرصيد بنجاح',
      user: result.rows[0]
    });

  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Error in PUT /users/:id:', error);
    return res.status(500).json({ success: false, error: error.message });
  } finally {
    client.release();
  }
});

/**
 * 3. Toggle User Suspension
 */
router.post('/users/:id/toggle-suspend', async (req, res) => {
  try {
    const { id } = req.params;
    const { suspendReason } = req.body;

    const userRes = await db.query('SELECT name, is_suspended FROM users WHERE id = $1', [id]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'User not found' });
    }

    const currentStatus = userRes.rows[0].is_suspended;
    const newStatus = !currentStatus;
    const reason = newStatus ? (suspendReason || 'مخالفة معايير الاستخدام') : null;

    await db.query(
      'UPDATE users SET is_suspended = $1, suspend_reason = $2 WHERE id = $3',
      [newStatus, reason, id]
    );

    // Revoke tokens if suspended
    if (newStatus) {
      await db.query('UPDATE refresh_tokens SET is_revoked = TRUE WHERE user_id = $1', [id]);
    }

    await logAdminActivity(
      db,
      req.user.userId,
      req.user.email,
      newStatus ? 'تجميد حساب' : 'إلغاء تجميد حساب',
      `${newStatus ? 'تم تجميد' : 'تم إلغاء تجميد'} حساب: ${userRes.rows[0].name}. السبب: ${reason || 'لا يوجد'}`,
      id,
      req.ip
    );

    res.json({
      success: true,
      message: newStatus ? 'تم تعليق الحساب بنجاح' : 'تم تفعيل الحساب بنجاح',
      isSuspended: newStatus,
    });
  } catch (err) {
    console.error('Error toggling suspension:', err);
    res.status(500).json({ success: false, error: 'Failed to update suspension status' });
  }
});

/**
 * 4. Adjust User Wallet Points (Strict Audit & Transaction)
 */
router.post('/users/:id/adjust-wallet', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { points, reason } = req.body;

    const deltaPoints = parseInt(points, 10);
    if (isNaN(deltaPoints) || deltaPoints === 0) {
      return res.status(400).json({ success: false, error: 'يرجى إدخال قيمة صحيحة ومختلفة عن الصفر للنقاط' });
    }

    if (!reason || !reason.trim()) {
      return res.status(400).json({ success: false, error: 'سبب تعديل الرصيد مطلوب للتدقيق المالي' });
    }

    await client.query('BEGIN');

    const userRes = await client.query('SELECT name, wallet_points FROM users WHERE id = $1 FOR UPDATE', [id]);
    if (userRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'User not found' });
    }

    const oldBalance = userRes.rows[0].wallet_points;
    const newBalance = oldBalance + deltaPoints;

    if (newBalance < 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({
        success: false,
        error: `لا يمكن خصم ${Math.abs(deltaPoints)} نقطة لأن رصيد المستخدم الحالي هو ${oldBalance} نقطة فقط.`,
      });
    }

    // Update user balance
    await client.query('UPDATE users SET wallet_points = $1 WHERE id = $2', [newBalance, id]);

    // Record wallet ledger transaction
    await client.query(
      `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
       VALUES ($1, $2, 'ADMIN_ADJUSTMENT', $3, $4, $5, 'COMPLETED')`,
      [
        uuidv4(),
        id,
        deltaPoints,
        Math.abs(deltaPoints) / 10.0,
        `تعديل رصيد إداري (${deltaPoints > 0 ? '+' : ''}${deltaPoints} نقطة): ${reason.trim()}`,
      ]
    );

    // Send notification to user
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'تحديث رصيد المحفظة', $3, 'SYSTEM')`,
      [
        uuidv4(),
        id,
        `تم ${deltaPoints > 0 ? 'إضافة' : 'خصم'} ${Math.abs(deltaPoints)} نقطة من محفظتك. السبب: ${reason.trim()}. الرصيد الحالي: ${newBalance} نقطة.`,
      ]
    );

    // Log admin activity
    await logAdminActivity(
      client,
      req.user.userId,
      req.user.email,
      'تعديل رصيد مالي',
      `تعديل رصيد ${userRes.rows[0].name} من ${oldBalance} إلى ${newBalance} (${deltaPoints > 0 ? '+' : ''}${deltaPoints} نقطة). السبب: ${reason.trim()}`,
      id,
      req.ip
    );

    await client.query('COMMIT');
    res.json({
      success: true,
      message: 'تم تعديل الرصيد بنجاح وتسجيل العملية في سجل التدقيق المالي',
      walletPoints: newBalance,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error adjusting wallet points:', err);
    res.status(500).json({ success: false, error: 'فشل في تعديل رصيد المحفظة' });
  } finally {
    client.release();
  }
});

/**
 * 5. Server-Side User Impersonation (Super Admin Only)
 */
router.post('/impersonate/:userId', requireSuperAdmin, async (req, res) => {
  try {
    const { userId } = req.params;

    const userRes = await db.query(
      'SELECT id, name, email, phone, avatar_url, rating, ride_count, is_verified, wallet_points, role, user_role, referral_code FROM users WHERE id = $1',
      [userId]
    );

    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'المستخدم المراد تقمص هويته غير موجود' });
    }

    const targetUser = userRes.rows[0];

    // Generate a temporary scoped token
    const impersonatedToken = jwt.sign(
      {
        userId: targetUser.id,
        email: targetUser.email,
        role: 'USER', // Degraded to USER while impersonating
        isImpersonating: true,
        realAdminId: req.user.userId,
      },
      JWT_SECRET,
      { expiresIn: '2h' }
    );

    await logAdminActivity(
      db,
      req.user.userId,
      req.user.email,
      'تقمص هوية مستخدم',
      `تم الدخول بحساب المستخدم: ${targetUser.name} (${targetUser.id})`,
      targetUser.id,
      req.ip
    );

    res.json({
      success: true,
      message: `تم الدخول بحساب ${targetUser.name} بنجاح`,
      impersonatedToken,
      user: targetUser,
    });
  } catch (err) {
    console.error('Impersonation error:', err);
    res.status(500).json({ success: false, error: 'Failed to start impersonation' });
  }
});

/**
 * 6. TopUp Requests List
 */
router.get('/topup-requests', async (req, res) => {
  try {
    const { status } = req.query;
    let query = 'SELECT * FROM topup_requests';
    const params = [];
    if (status) {
      query += ' WHERE status = $1';
      params.push(status);
    }
    query += ' ORDER BY created_at DESC';
    const result = await db.query(query, params);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching topup requests:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 7. Approve TopUp Request (Atomic with Row Locking & Balance Credit)
 */
router.post('/topup-requests/:id/approve', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    await client.query('BEGIN');

    const topupRes = await client.query('SELECT * FROM topup_requests WHERE id = $1 FOR UPDATE', [id]);
    if (topupRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'طلب الشحن غير موجود' });
    }

    const topup = topupRes.rows[0];
    if (topup.status !== 'PENDING') {
      await client.query('ROLLBACK');
      return res.status(400).json({
        success: false,
        error: `تم معالجة هذا الطلب مسبقاً (الحالة الحالية: ${topup.status})`,
      });
    }

    // Credit Points
    await client.query('UPDATE users SET wallet_points = wallet_points + $1 WHERE id = $2', [
      topup.package_points,
      topup.user_id,
    ]);

    // Record wallet ledger transaction
    await client.query(
      `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
       VALUES ($1, $2, 'TOPUP', $3, $4, $5, 'COMPLETED')`,
      [
        uuidv4(),
        topup.user_id,
        topup.package_points,
        topup.package_price_usd,
        `شحن رصيد شام كاش (${topup.package_points} نقطة)`,
      ]
    );

    // Update TopUp Request
    await client.query(
      `UPDATE topup_requests
       SET status = 'APPROVED', processed_by_admin_id = $1, processed_at = CURRENT_TIMESTAMP
       WHERE id = $2`,
      [req.user.userId, id]
    );

    // Notification to user
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, '✅ تم قبول طلب الشحن', $3, 'APPROVAL')`,
      [
        uuidv4(),
        topup.user_id,
        `تم تأكيد عملية التحويل عبر شام كاش وإضافة ${topup.package_points} نقطة بنجاح إلى رصيدك.`,
      ]
    );

    // Admin activity log
    await logAdminActivity(
      client,
      req.user.userId,
      req.user.email,
      'موافقة شحن نقاط',
      `تمت الموافقة على شحن ${topup.package_points} نقطة للمستخدم ${topup.user_name} (${topup.user_id}) بمبلغ $${topup.package_price_usd}`,
      id,
      req.ip
    );

    await client.query('COMMIT');
    res.json({ success: true, message: 'تمت الموافقة على طلب الشحن وإضافة النقاط بنجاح' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error approving topup:', err);
    res.status(500).json({ success: false, error: 'فشل في إتمام الموافقة على طلب الشحن' });
  } finally {
    client.release();
  }
});

/**
 * 8. Reject TopUp Request
 */
router.post('/topup-requests/:id/reject', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { reason } = req.body;

    await client.query('BEGIN');

    const topupRes = await client.query('SELECT * FROM topup_requests WHERE id = $1 FOR UPDATE', [id]);
    if (topupRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'طلب الشحن غير موجود' });
    }

    const topup = topupRes.rows[0];
    if (topup.status !== 'PENDING') {
      await client.query('ROLLBACK');
      return res.status(400).json({
        success: false,
        error: `تم معالجة هذا الطلب مسبقاً (الحالة الحالية: ${topup.status})`,
      });
    }

    const rejectionReason = reason || 'إشعار الدفع أو رقم الحساب غير متطابق';

    await client.query(
      `UPDATE topup_requests
       SET status = 'REJECTED', rejection_reason = $1, processed_by_admin_id = $2, processed_at = CURRENT_TIMESTAMP
       WHERE id = $3`,
      [rejectionReason, req.user.userId, id]
    );

    // Notification to user
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, '❌ رفض طلب الشحن', $3, 'SYSTEM')`,
      [
        uuidv4(),
        topup.user_id,
        `عذراً، تعذر تأكيد طلب شحن ${topup.package_points} نقطة. السبب: ${rejectionReason}`,
      ]
    );

    await logAdminActivity(
      client,
      req.user.userId,
      req.user.email,
      'رفض طلب شحن نقاط',
      `تم رفض طلب شحن ${topup.package_points} نقطة للمستخدم ${topup.user_name}. السبب: ${rejectionReason}`,
      id,
      req.ip
    );

    await client.query('COMMIT');
    res.json({ success: true, message: 'تم رفض طلب الشحن بنجاح' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error rejecting topup:', err);
    res.status(500).json({ success: false, error: 'فشل في رفض طلب الشحن' });
  } finally {
    client.release();
  }
});

/**
 * 9. Rides Moderation
 */
router.get('/rides', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM rides ORDER BY created_at DESC');
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching admin rides:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

router.delete('/rides/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { reason } = req.body;

    const rideRes = await db.query('SELECT driver_id, start_city, end_city FROM rides WHERE id = $1', [id]);
    if (rideRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'الرحلة غير موجودة' });
    }

    const ride = rideRes.rows[0];
    await db.query("UPDATE rides SET status = 'CANCELLED' WHERE id = $1", [id]);

    // Notify driver
    await db.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'إلغاء الرحلة من قبل الإدارة', $3, 'SYSTEM')`,
      [
        uuidv4(),
        ride.driver_id,
        `تم إلغاء رحلتك من ${ride.start_city} إلى ${ride.end_city} من قبل إدارة التطبيق. السبب: ${reason || 'مخالفة شروط النشر'}.`,
      ]
    );

    await logAdminActivity(
      db,
      req.user.userId,
      req.user.email,
      'إلغاء رحلة إدارياً',
      `تم إلغاء رحلة ${ride.start_city} ➔ ${ride.end_city} (${id}). السبب: ${reason || 'لا يوجد'}`,
      id,
      req.ip
    );

    res.json({ success: true, message: 'تم إلغاء الرحلة إدارياً بنجاح' });
  } catch (err) {
    console.error('Error deleting ride:', err);
    res.status(500).json({ success: false, error: 'Failed to delete ride' });
  }
});

/**
 * 10. Requested Trips Moderation
 */
router.get('/requested-trips', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM requested_trips ORDER BY created_at DESC');
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching admin requested trips:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

router.delete('/requested-trips/:id', async (req, res) => {
  try {
    const { id } = req.params;
    await db.query('DELETE FROM requested_trips WHERE id = $1', [id]);
    await db.query('DELETE FROM rides WHERE id = $1', [`ride_from_req_${id}`]);

    await logAdminActivity(
      db,
      req.user.userId,
      req.user.email,
      'حذف طلب رحلة',
      `تم حذف طلب الرحلة: ${id}`,
      id,
      req.ip
    );

    res.json({ success: true, message: 'تم حذف طلب الرحلة بنجاح' });
  } catch (err) {
    console.error('Error deleting requested trip:', err);
    res.status(500).json({ success: false, error: 'Failed to delete requested trip' });
  }
});

/**
 * 11. Chat Moderation
 */
router.get('/chats', async (req, res) => {
  try {
    const result = await db.query(`
      SELECT r.id as ride_id, r.driver_name, r.start_city, r.end_city, r.departure_date,
             COUNT(m.id) as message_count, MAX(m.created_at) as last_message_at
      FROM rides r
      LEFT JOIN chat_messages m ON r.id = m.ride_id
      GROUP BY r.id, r.driver_name, r.start_city, r.end_city, r.departure_date
      ORDER BY last_message_at DESC NULLS LAST
    `);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching chat rooms:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch chat rooms' });
  }
});

router.get('/chats/:rideId', async (req, res) => {
  try {
    const { rideId } = req.params;
    const result = await db.query('SELECT * FROM chat_messages WHERE ride_id = $1 ORDER BY created_at ASC', [rideId]);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching room messages:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch messages' });
  }
});

router.delete('/chats/:rideId/messages/:messageId', async (req, res) => {
  try {
    const { messageId } = req.params;
    await db.query('DELETE FROM chat_messages WHERE id = $1', [messageId]);
    res.json({ success: true, message: 'تم حذف الرسالة بنجاح' });
  } catch (err) {
    console.error('Error deleting message:', err);
    res.status(500).json({ success: false, error: 'Failed to delete message' });
  }
});

router.post('/chats/:rideId/clear', async (req, res) => {
  try {
    const { rideId } = req.params;
    await db.query('DELETE FROM chat_messages WHERE ride_id = $1', [rideId]);
    res.json({ success: true, message: 'تم مسح محادثات الغرفة بنجاح' });
  } catch (err) {
    console.error('Error clearing chat:', err);
    res.status(500).json({ success: false, error: 'Failed to clear chat' });
  }
});

router.post('/chats/:rideId/admin-message', async (req, res) => {
  try {
    const { rideId } = req.params;
    const { message } = req.body;

    const id = `msg_${uuidv4().substring(0, 8)}`;
    const timestamp = new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });

    const result = await db.query(
      `INSERT INTO chat_messages (id, ride_id, sender_id, sender_name, sender_avatar, message, timestamp, is_driver)
       VALUES ($1, $2, $3, 'إدارة وصلني', '', $4, $5, FALSE) RETURNING *`,
      [id, rideId, req.user.userId, message, timestamp]
    );

    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error posting admin chat message:', err);
    res.status(500).json({ success: false, error: 'Failed to post message' });
  }
});

/**
 * 12. Broadcast Notification (Push/In-App)
 */
router.post('/broadcast', async (req, res) => {
  try {
    const { title, message, targetAudience = 'ALL' } = req.body;

    if (!title || !message) {
      return res.status(400).json({ success: false, error: 'عنوان ونص الإشعار مطلوبان' });
    }

    let userQuery = 'SELECT id FROM users';
    if (targetAudience === 'DRIVERS') {
      userQuery += " WHERE user_role = 'سائق' OR ride_count > 0";
    } else if (targetAudience === 'PASSENGERS') {
      userQuery += " WHERE user_role = 'راكب'";
    }

    const users = await db.query(userQuery);
    for (const u of users.rows) {
      await db.query(
        `INSERT INTO notifications (id, user_id, title, message, type)
         VALUES ($1, $2, $3, $4, 'BROADCAST')`,
        [uuidv4(), u.id, title, message]
      );
    }

    await logAdminActivity(
      db,
      req.user.userId,
      req.user.email,
      'إرسال إشعار جماعي',
      `تم إرسال إشعار "${title}" إلى الفئة (${targetAudience}) - العدد: ${users.rows.length} مستخدم`,
      null,
      req.ip
    );

    res.json({
      success: true,
      message: `تم إرسال الإشعار الجماعي بنجاح إلى ${users.rows.length} مستخدم`,
    });
  } catch (err) {
    console.error('Error broadcasting notification:', err);
    res.status(500).json({ success: false, error: 'Failed to broadcast notification' });
  }
});

/**
 * 13. App Settings (Remote Config)
 */
router.get('/settings', async (req, res) => {
  try {
    const result = await db.query('SELECT setting_key, setting_value FROM app_settings');
    const settings = {};
    result.rows.forEach(row => {
      settings[row.setting_key] = row.setting_value;
    });
    res.json({ success: true, data: settings });
  } catch (err) {
    console.error('Error fetching settings:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch settings' });
  }
});

router.put('/settings', async (req, res) => {
  try {
    const entries = Object.entries(req.body);
    for (const [key, value] of entries) {
      await db.query(
        `INSERT INTO app_settings (setting_key, setting_value, updated_at)
         VALUES ($1, $2, CURRENT_TIMESTAMP)
         ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP`,
        [key, String(value)]
      );
    }

    await logAdminActivity(
      db,
      req.user.userId,
      req.user.email,
      'تعديل إعدادات التطبيق',
      `تم تحديث إعدادات التطبيق العامة: ${Object.keys(req.body).join(', ')}`,
      null,
      req.ip
    );

    res.json({ success: true, message: 'تم حفظ وتطبيق الإعدادات بنجاح' });
  } catch (err) {
    console.error('Error updating settings:', err);
    res.status(500).json({ success: false, error: 'Failed to update settings' });
  }
});

/**
 * 14. Financial and Activity Audit Logs
 */
router.get('/audit', async (req, res) => {
  try {
    const activityLogs = await db.query('SELECT * FROM admin_activity_logs ORDER BY created_at DESC LIMIT 100');
    const walletTransactions = await db.query(`
      SELECT wt.*, u.name as user_name, u.email as user_email
      FROM wallet_transactions wt
      JOIN users u ON wt.user_id = u.id
      ORDER BY wt.created_at DESC LIMIT 100
    `);

    res.json({
      success: true,
      activityLogs: activityLogs.rows,
      walletTransactions: walletTransactions.rows,
    });
  } catch (err) {
    console.error('Error fetching audit logs:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch audit logs' });
  }
});

/**
 * 15. Support Tickets
 */
router.get('/support-tickets', async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM support_tickets ORDER BY created_at DESC');
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching support tickets:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch tickets' });
  }
});

router.post('/support-tickets/:id/reply', async (req, res) => {
  try {
    const { id } = req.params;
    const { reply } = req.body;

    const ticketRes = await db.query('SELECT user_id, subject FROM support_tickets WHERE id = $1', [id]);
    if (ticketRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'التذكرة غير موجودة' });
    }

    const ticket = ticketRes.rows[0];
    await db.query(
      "UPDATE support_tickets SET admin_reply = $1, status = 'RESOLVED', updated_at = CURRENT_TIMESTAMP WHERE id = $2",
      [reply, id]
    );

    if (ticket.user_id) {
      await db.query(
        `INSERT INTO notifications (id, user_id, title, message, type)
         VALUES ($1, $2, 'رد على تذكرة الدعم الفني', $3, 'SYSTEM')`,
        [uuidv4(), ticket.user_id, `تم الرد على تذكرتك (${ticket.subject}): ${reply}`]
      );
    }

    res.json({ success: true, message: 'تم إرسال الرد وإغلاق التذكرة بنجاح' });
  } catch (err) {
    console.error('Error replying to ticket:', err);
    res.status(500).json({ success: false, error: 'Failed to reply to ticket' });
  }
});

// ========================================================
// مسار تعديل النقاط وتسجيل الحركة في wallet_transactions
// POST /api/admin/users/:id/adjust-wallet
// ========================================================
router.post('/users/:id/adjust-wallet', async (req, res) => {
  const { id } = req.params;
  const { amount, type, reason } = req.body; // type: 'ADD' أو 'DEDUCT'

  if (!amount || isNaN(amount) || amount <= 0) {
    return res.status(400).json({ message: 'يرجى إدخال قيمة نقاط صالحة وموجبة' });
  }

  const client = await db.getClient();

  try {
    await client.query('BEGIN');

    const pointsChange = type === 'ADD' ? parseInt(amount, 10) : -parseInt(amount, 10);

    // 1. تحديث إجمالي نقاط المستخدم
    const updateRes = await client.query(
      `UPDATE users 
       SET wallet_points = wallet_points + $1 
       WHERE id = $2 
       RETURNING id, wallet_points`,
      [pointsChange, id]
    );

    if (updateRes.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ message: 'المستخدم غير موجود' });
    }

    // 2. تدوين الحركة في دفتر المعاملات wallet_transactions
    await client.query(
      `INSERT INTO wallet_transactions (user_id, amount, type, reason, created_at)
       VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP)`,
      [id, Math.abs(amount), type, reason || 'تعديل إداري من لوحة التحكم']
    );

    await client.query('COMMIT');

    return res.json({
      success: true,
      message: 'تم تعديل النقاط وتسجيل الحركة بنجاح',
      newWalletPoints: updateRes.rows[0].wallet_points
    });
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Adjust Wallet Error:', error);
    return res.status(500).json({ message: 'حدث خطأ أثناء تعديل النقاط', error: error.message });
  } finally {
    client.release();
  }
});
module.exports = router;
