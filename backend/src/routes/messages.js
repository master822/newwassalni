const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');

/**
 * 1. Get messages for a ride
 */
router.get('/:rideId', authenticateToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    const result = await db.query(
      'SELECT * FROM chat_messages WHERE ride_id = $1 ORDER BY created_at ASC',
      [rideId]
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching chat messages:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 2. Send a new chat message
 */
router.post('/:rideId', authenticateToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    const { message, imageUri = null, isLocation = false, latitude = null, longitude = null } = req.body;
    const senderId = req.user.userId;

    if (!message && !imageUri && !isLocation) {
      return res.status(400).json({ success: false, error: 'محتوى الرسالة مطلوب' });
    }

    const userRes = await db.query('SELECT name, avatar_url FROM users WHERE id = $1', [senderId]);
    const sender = userRes.rows[0] || { name: 'مستخدم', avatar_url: '' };

    // Check if sender is driver of this ride
    const rideRes = await db.query('SELECT driver_id FROM rides WHERE id = $1', [rideId]);
    const isDriver = rideRes.rows.length > 0 && rideRes.rows[0].driver_id === senderId;

    const id = `msg_${uuidv4().substring(0, 8)}`;
    const timestamp = new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });

    const query = `
      INSERT INTO chat_messages (id, ride_id, sender_id, sender_name, sender_avatar, message, timestamp, is_driver, image_uri, is_location, latitude, longitude)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
      RETURNING *
    `;
    const result = await db.query(query, [
      id,
      rideId,
      senderId,
      sender.name,
      sender.avatar_url || '',
      message || '',
      timestamp,
      isDriver,
      imageUri,
      isLocation,
      latitude,
      longitude,
    ]);

    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error sending message:', err);
    res.status(500).json({ success: false, error: 'Failed to send message' });
  }
});

module.exports = router;
