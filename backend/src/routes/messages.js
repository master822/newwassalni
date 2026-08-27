const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');
const { authenticateOptionalToken, authenticateToken } = require('../middleware/auth');

// Automatically ensure schema flexibility for chat_messages (supporting direct chats, admin chats, rides)
(async () => {
  try {
    // Drop all foreign key constraints on chat_messages so direct chats (chat_user_...) and unlisted user IDs work seamlessly
    await db.query(`
      DO $$
      DECLARE
        r RECORD;
      BEGIN
        FOR r IN (
          SELECT constraint_name 
          FROM information_schema.table_constraints 
          WHERE table_name = 'chat_messages' AND constraint_type = 'FOREIGN KEY'
        ) LOOP
          EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
        END LOOP;

        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS receiver_id VARCHAR(64) DEFAULT '';
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS image_uri TEXT DEFAULT NULL;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS audio_uri TEXT DEFAULT NULL;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS audio_duration INT DEFAULT 0;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS is_location BOOLEAN DEFAULT FALSE;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION DEFAULT NULL;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION DEFAULT NULL;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS is_driver BOOLEAN DEFAULT FALSE;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
      EXCEPTION
        WHEN OTHERS THEN NULL;
      END $$;
    `);
  } catch (e) {
    console.warn('Chat messages table schema adjustment info:', e.message);
  }
})();

function formatChatMessageRow(row) {
  if (!row) return null;
  return {
    id: String(row.id || ''),
    ride_id: String(row.ride_id || ''),
    sender_id: String(row.sender_id || ''),
    sender_name: row.sender_name || 'مستخدم',
    sender_avatar: row.sender_avatar || '',
    message: row.message || '',
    timestamp: row.timestamp || '',
    is_driver: Boolean(row.is_driver),
    image_uri: row.image_uri || null,
    audio_uri: row.audio_uri || null,
    audio_duration: row.audio_duration !== null && row.audio_duration !== undefined ? parseInt(row.audio_duration, 10) || 0 : 0,
    is_location: Boolean(row.is_location),
    latitude: row.latitude !== null && row.latitude !== undefined && !isNaN(parseFloat(row.latitude)) ? parseFloat(row.latitude) : null,
    longitude: row.longitude !== null && row.longitude !== undefined && !isNaN(parseFloat(row.longitude)) ? parseFloat(row.longitude) : null,
    receiver_id: row.receiver_id || '',
    created_at: row.created_at ? new Date(row.created_at).toISOString() : new Date().toISOString(),
  };
}

/**
 * 1. Get all recent messages across conversations (for multi-user synchronization)
 */
router.get('/sync/all', authenticateOptionalToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT * FROM chat_messages ORDER BY created_at ASC LIMIT 1000'
    );
    const formatted = result.rows.map(formatChatMessageRow);
    res.json({ success: true, data: formatted });
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
    const formatted = result.rows.map(formatChatMessageRow);
    res.json({ success: true, data: formatted });
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
      id: bodyId = null,
      message = '',
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
        senderName = bodySenderName || 'إدارة التطبيق 🛡️';
      }
    } catch (_) {}

    // Check if sender is driver of this ride
    let isDriver = false;
    try {
      const rideRes = await db.query('SELECT driver_id FROM rides WHERE id = $1', [rideId]);
      isDriver = rideRes.rows.length > 0 && rideRes.rows[0].driver_id === senderId;
    } catch (_) {}

    const id = bodyId || `msg_${uuidv4().substring(0, 8)}`;
    const timestamp = new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });

    const query = `
      INSERT INTO chat_messages (id, ride_id, sender_id, sender_name, sender_avatar, message, timestamp, is_driver, image_uri, audio_uri, audio_duration, is_location, latitude, longitude, receiver_id)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
      ON CONFLICT (id) DO UPDATE SET
        message = EXCLUDED.message,
        image_uri = COALESCE(EXCLUDED.image_uri, chat_messages.image_uri),
        audio_uri = COALESCE(EXCLUDED.audio_uri, chat_messages.audio_uri),
        audio_duration = EXCLUDED.audio_duration,
        timestamp = EXCLUDED.timestamp
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

    res.status(201).json({ success: true, data: formatChatMessageRow(result.rows[0]) });
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
