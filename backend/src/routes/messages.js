const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');

// 1. Get messages for a ride
router.get('/:rideId', async (req, res) => {
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

// 2. Send a new chat message
router.post('/:rideId', async (req, res) => {
  try {
    const { rideId } = req.params;
    const { senderId, senderName, senderAvatar, message, isDriver = false, imageUri = null, isLocation = false } = req.body;

    const id = `msg_${uuidv4().substring(0, 8)}`;
    const timestamp = new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });

    const query = `
      INSERT INTO chat_messages (id, ride_id, sender_id, sender_name, sender_avatar, message, timestamp, is_driver, image_uri, is_location)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
      RETURNING *
    `;
    const result = await db.query(query, [
      id,
      rideId,
      senderId,
      senderName,
      senderAvatar || '',
      message,
      timestamp,
      isDriver,
      imageUri,
      isLocation,
    ]);

    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error sending message:', err);
    res.status(500).json({ success: false, error: 'Failed to send message' });
  }
});

module.exports = router;
