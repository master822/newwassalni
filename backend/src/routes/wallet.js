const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');

// 1. Get Wallet Transactions
router.get('/transactions/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const result = await db.query(
      'SELECT * FROM wallet_transactions WHERE user_id = $1 ORDER BY created_at DESC',
      [userId]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching transactions:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

// 2. Submit Cham Cash TopUp Request
router.post('/topup', async (req, res) => {
  try {
    const { userId, userName, packagePoints, packagePriceUsd, receiptImagePath } = req.body;
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
        `تم استلام طلب شراء ${packagePoints} نقطة وهو الآن قيد المراجعة من الإدارة.`,
      ]
    );

    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error creating topup request:', err);
    res.status(500).json({ success: false, error: 'Failed to submit topup request' });
  }
});

module.exports = router;
