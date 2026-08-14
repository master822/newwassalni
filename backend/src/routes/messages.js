const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');

/**
 * Helper: Check if user has permission to access ride chat room
 */
async function canAccessRideChat(userId, userRole, rideId) {
  if (userRole === 'ADMIN' || userRole === 'SUPER_ADMIN') {
    return true;
  }

  // Check if driver
  const rideRes = await db.query('SELECT driver_id FROM rides WHERE id = $1', [rideId]);
  if (rideRes.rows.length > 0 && rideRes.rows[0].driver_id === userId) {
    return true;
  }

  // Check if booked passenger
  const bookingRes = await db.query(
    "SELECT 1 FROM ride_bookings WHERE ride_id = $1 AND passenger_id = $2 AND status != 'CANCELLED'",
    [rideId, userId]
  );
  if (bookingRes.rows.length > 0) {
    return true;
  }

  // Check if requested trip user
  if (rideId.startsWith('ride_from_req_')) {
    const reqId = rideId.replace('ride_from_req_', '');
    const reqRes = await db.query('SELECT user_id, accepted_by_driver_id FROM requested_trips WHERE id = $1', [reqId]);
    if (reqRes.rows.length > 0) {
      if (reqRes.rows[0].user_id === userId || reqRes.rows[0].accepted_by_driver_id === userId) {
        return true;
      }
    }
  }

  return false;
}

/**
 * 1. Get messages for a ride (Scoped to ride participants)
 */
router.get('/:rideId', authenticateToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    const userId = req.user.userId;
    const userRole = req.user.role;

    const allowed = await canAccessRideChat(userId, userRole, rideId);
    if (!allowed) {
      return res.status(403).json({
        success: false,
        error: 'ليس لديك صلاحية الوصول إلى محادثة هذه الرحلة',
      });
    }

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
 * 2. Send a new chat message (Scoped to ride participants)
 */
router.post('/:rideId', authenticateToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    const { message, imageUri = null, isLocation = false, latitude = null, longitude = null } = req.body;
    const senderId = req.user.userId;
    const userRole = req.user.role;

    if (!message && !imageUri && !isLocation) {
      return res.status(400).json({ success: false, error: 'محتوى الرسالة مطلوب' });
    }

    const allowed = await canAccessRideChat(senderId, userRole, rideId);
    if (!allowed) {
      return res.status(403).json({
        success: false,
        error: 'ليس لديك صلاحية إرسال رسائل في محادثة هذه الرحلة',
      });
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
