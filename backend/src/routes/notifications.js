const express = require('express');
const router = express.Router();
const db = require('../database');
const { authenticateToken } = require('../middleware/auth');

/**
 * 1. Get notifications for authenticated user
 */
router.get('/', authenticateToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM notifications WHERE user_id = $1 ORDER BY created_at DESC LIMIT 50',
      [req.user.userId]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching notifications:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 2. Mark single notification as read
 */
router.put('/:id/read', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    await db.query('UPDATE notifications SET is_read = TRUE WHERE id = $1 AND user_id = $2', [
      id,
      req.user.userId,
    ]);
    res.json({ success: true, message: 'Notification marked as read' });
  } catch (err) {
    console.error('Error marking notification read:', err);
    res.status(500).json({ success: false, error: 'Failed to update notification' });
  }
});

/**
 * 3. Mark all notifications as read
 */
router.put('/read-all', authenticateToken, async (req, res) => {
  try {
    await db.query('UPDATE notifications SET is_read = TRUE WHERE user_id = $1', [req.user.userId]);
    res.json({ success: true, message: 'All notifications marked as read' });
  } catch (err) {
    console.error('Error marking all notifications read:', err);
    res.status(500).json({ success: false, error: 'Failed to update notifications' });
  }
});

module.exports = router;
