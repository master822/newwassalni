const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');

/**
 * 1. Get Wallet Transactions & Balance for authenticated user
 */
router.get('/', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

    const userRes = await db.query('SELECT wallet_points FROM users WHERE id = $1', [userId]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }

    const currentPoints = userRes.rows[0].wallet_points;

    const transactionsRes = await db.query(
      'SELECT * FROM wallet_transactions WHERE user_id = $1 ORDER BY created_at DESC',
      [userId]
    );

    res.json({
      success: true,
      walletPoints: currentPoints,
      transactions: transactionsRes.rows,
    });
  } catch (err) {
    console.error('Error fetching transactions:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 2. Submit Cham Cash TopUp Request
 */
router.post('/topup', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { packagePoints, packagePriceUsd, receiptImagePath } = req.body;

    if (!packagePoints || !packagePriceUsd) {
      return res.status(400).json({ success: false, error: 'تفاصيل الباقة والمبلغ مطلوبة' });
    }

    const userRes = await db.query('SELECT name FROM users WHERE id = $1', [userId]);
    const userName = userRes.rows.length > 0 ? userRes.rows[0].name : 'مستخدم وصلني';

    const id = `topup_${uuidv4().substring(0, 8)}`;
    const query = `
      INSERT INTO topup_requests (id, user_id, user_name, package_points, package_price_usd, receipt_image_path, status)
      VALUES ($1, $2, $3, $4, $5, $6, 'PENDING')
      RETURNING *
    `;
    const result = await db.query(query, [
      id,
      userId,
      userName,
      packagePoints,
      packagePriceUsd,
      receiptImagePath || '',
    ]);

    // Send User Notification
    await db.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'طلب الشحن قيد المراجعة', $3, 'SYSTEM')`,
      [
        uuidv4(),
        userId,
        `تم استلام طلب شحن ${packagePoints} نقطة وهو الآن قيد التدقيق والموافقة من الإدارة.`,
      ]
    );

    res.status(201).json({
      success: true,
      message: 'تم إرسال إشعار التحويل بنجاح، سيتم إضافة النقاط بعد مراجعة الإيصال.',
      data: result.rows[0],
    });
  } catch (err) {
    console.error('Error creating topup request:', err);
    res.status(500).json({ success: false, error: 'Failed to submit topup request' });
  }
});

module.exports = router;
