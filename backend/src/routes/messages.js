const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');
const { authenticateOptionalToken } = require('../middleware/auth');

// Automatically ensure schema flexibility for chat_messages (supporting direct chats, admin chats, rides)
(async () => {
  try {
    // Drop rigid foreign keys if present so direct chats (chat_user_...) and admin messages work seamlessly
    await db.query(`
      DO $$
      BEGIN
        ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_ride_id_fkey;
        ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_sender_id_fkey;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS receiver_id VARCHAR(64) DEFAULT '';
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS audio_uri TEXT DEFAULT NULL;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS audio_duration INT DEFAULT 0;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
      EXCEPTION
        WHEN OTHERS THEN NULL;
      END $$;
    `);
  } catch (e) {
    console.warn('Chat messages table schema adjustment info:', e.message);
  }
})();

/**
 * 1. Get all recent messages across conversations (for multi-user synchronization)
 */
router.get('/sync/all', authenticateOptionalToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM chat_messages ORDER BY created_at ASC LIMIT 1000'
    );
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error syncing all chat messages:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 2. Get messages for a specific ride or direct chat room
 */
router.get('/:rideId', authenticateOptionalToken, async (req, res) => {
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
 * 3. Send a new chat message
 */
router.post('/:rideId', authenticateOptionalToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    const {
      message,
      imageUri = null,
      audioUri = null,
      audioDuration = 0,
      isLocation = false,
      latitude = null,
      longitude = null,
      receiverId = '',
      senderId: bodySenderId = null,
      senderName: bodySenderName = null,
      senderAvatar: bodySenderAvatar = null
    } = req.body;
    
    const senderId = bodySenderId || req.user?.userId || req.headers['x-user-id'] || 'user_me';

    if (!message && !imageUri && !audioUri && !isLocation) {
      return res.status(400).json({ success: false, error: 'محتوى الرسالة مطلوب' });
    }

    let senderName = bodySenderName || 'مستخدم';
    let senderAvatar = bodySenderAvatar || '';
    try {
      const userRes = await db.query('SELECT name, avatar_url, role FROM users WHERE id = $1', [senderId]);
      if (userRes.rows.length > 0) {
        senderName = bodySenderName || userRes.rows[0].name || senderName;
        senderAvatar = bodySenderAvatar || userRes.rows[0].avatar_url || senderAvatar;
      } else if (req.user?.role === 'ADMIN' || req.user?.role === 'SUPER_ADMIN' || senderId.includes('admin')) {
        senderName = bodySenderName || 'إدارة وسلني 🛡️';
      }
    } catch (_) {}

    // Check if sender is driver of this ride
    let isDriver = false;
    try {
      const rideRes = await db.query('SELECT driver_id FROM rides WHERE id = $1', [rideId]);
      isDriver = rideRes.rows.length > 0 && rideRes.rows[0].driver_id === senderId;
    } catch (_) {}

    const id = `msg_${uuidv4().substring(0, 8)}`;
    const timestamp = new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });

    const query = `
      INSERT INTO chat_messages (id, ride_id, sender_id, sender_name, sender_avatar, message, timestamp, is_driver, image_uri, audio_uri, audio_duration, is_location, latitude, longitude, receiver_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
      RETURNING *
    `;
    const result = await db.query(query, [
      id,
      rideId,
      senderId,
      senderName,
      senderAvatar,
      message || '',
      timestamp,
      isDriver,
      imageUri,
      audioUri,
      audioDuration,
      isLocation,
      latitude,
      longitude,
      receiverId || '',
    ]);

    // If a recipient is specified, also insert an in-app notification so the receiver gets notified
    if (receiverId && receiverId !== senderId && receiverId !== 'all') {
      try {
        const notifId = `notif_${uuidv4().substring(0, 8)}`;
        const notifTitle = `رسالة جديدة من ${senderName} 💬`;
        const notifBody = audioUri ? `أرسل لك تسجيلاً صوتياً 🎙️ (${audioDuration} ث)` : (imageUri ? `أرسل لك صورة مرفقة 📷` : (message || 'رسالة جديدة'));
        await db.query(
          `INSERT INTO notifications (id, user_id, title, message, type, is_read)
           VALUES ($1, $2, $3, $4, 'CHAT', FALSE)
           ON CONFLICT (id) DO NOTHING`,
          [notifId, receiverId, notifTitle, notifBody]
        );
      } catch (notifErr) {
        console.warn('Could not insert chat notification:', notifErr.message);
      }
    }

    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error sending message:', err);
    res.status(500).json({ success: false, error: 'Failed to send message: ' + err.message });
  }
});

/**
 * 4. Delete all messages for a ride (Delete conversation)
 */
router.delete('/:rideId', authenticateToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    await db.query('DELETE FROM chat_messages WHERE ride_id = $1', [rideId]);
    res.json({ success: true, message: 'تم حذف المحادثة بنجاح' });
  } catch (err) {
    console.error('Error deleting conversation:', err);
    res.status(500).json({ success: false, error: 'Failed to delete conversation' });
  }
});

/**
 * 5. Delete single message
 */
router.delete('/item/:messageId', authenticateToken, async (req, res) => {
  try {
    const { messageId } = req.params;
    await db.query('DELETE FROM chat_messages WHERE id = $1', [messageId]);
    res.json({ success: true, message: 'تم حذف الرسالة بنجاح' });
  } catch (err) {
    console.error('Error deleting message:', err);
    res.status(500).json({ success: false, error: 'Failed to delete message' });
  }
});

module.exports = router;
