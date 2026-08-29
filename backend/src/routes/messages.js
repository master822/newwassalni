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
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS is_read BOOLEAN DEFAULT FALSE;
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
    is_read: Boolean(row.is_read),
    created_at: row.created_at ? new Date(row.created_at).toISOString() : new Date().toISOString(),
  };
}

/**
 * 1. Get all recent messages across conversations (for multi-user synchronization)
 */
router.get('/sync/all', authenticateOptionalToken, async (req, res) => {
  try {
    const result = await db.query(
      `SELECT m.*, COALESCE(NULLIF(u.avatar_url, ''), m.sender_avatar) AS sender_avatar,
              COALESCE(NULLIF(u.name, ''), m.sender_name) AS sender_name
       FROM chat_messages m
       LEFT JOIN users u ON m.sender_id = u.id
       ORDER BY m.created_at ASC LIMIT 1000`
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
      `SELECT m.*, COALESCE(NULLIF(u.avatar_url, ''), m.sender_avatar) AS sender_avatar,
              COALESCE(NULLIF(u.name, ''), m.sender_name) AS sender_name
       FROM chat_messages m
       LEFT JOIN users u ON m.sender_id = u.id
       WHERE m.ride_id = $1
       ORDER BY m.created_at ASC`,
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

    // Determine all recipients to notify
    const targetReceivers = new Set();
    if (receiverId && receiverId !== senderId && receiverId !== 'all') {
      targetReceivers.add(receiverId);
    } else if (rideId.startsWith('chat_user_')) {
      const otherUid = rideId.replace('chat_user_', '');
      if (otherUid !== senderId) targetReceivers.add(otherUid);
    } else {
      try {
        const rideRow = await db.query('SELECT driver_id FROM rides WHERE id = $1', [rideId]);
        if (rideRow.rows.length > 0) {
          const driverId = rideRow.rows[0].driver_id;
          if (senderId === driverId) {
            const bookings = await db.query('SELECT DISTINCT passenger_id FROM ride_bookings WHERE ride_id = $1', [rideId]);
            bookings.rows.forEach(b => {
              if (b.passenger_id && b.passenger_id !== senderId) targetReceivers.add(b.passenger_id);
            });
          } else {
            if (driverId && driverId !== senderId) targetReceivers.add(driverId);
          }
        }
      } catch (_) {}
    }

    const notifTitle = `رسالة جديدة من ${senderName} 💬`;
    const notifBody = audioUri ? `أرسل لك تسجيلاً صوتياً 🎙️ (${audioDuration} ث)` : (imageUri ? `أرسل لك صورة مرفقة 📷` : (message || 'رسالة جديدة'));

    for (const recId of targetReceivers) {
      try {
        const notifId = `notif_${uuidv4().substring(0, 8)}`;
        await db.query(
          `INSERT INTO notifications (id, user_id, title, message, type, is_read)
           VALUES ($1, $2, $3, $4, 'CHAT', FALSE)
           ON CONFLICT (id) DO NOTHING`,
          [notifId, recId, notifTitle, notifBody]
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
router.delete('/:rideId', authenticateOptionalToken, async (req, res) => {
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
router.delete('/item/:messageId', authenticateOptionalToken, async (req, res) => {
  try {
    const { messageId } = req.params;
    await db.query('DELETE FROM chat_messages WHERE id = $1', [messageId]);
    res.json({ success: true, message: 'تم حذف الرسالة بنجاح' });
  } catch (err) {
    console.error('Error deleting message:', err);
    res.status(500).json({ success: false, error: 'Failed to delete message' });
  }
});

/**
 * 6. Mark chat messages as read for a ride or conversation
 */
router.put('/read/:rideId', authenticateOptionalToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    const userId = req.user?.userId || req.headers['x-user-id'] || '';
    if (userId) {
      await db.query(
        'UPDATE chat_messages SET is_read = TRUE WHERE ride_id = $1 AND sender_id != $2',
        [rideId, userId]
      );
      await db.query(
        'UPDATE notifications SET is_read = TRUE WHERE user_id = $1 AND type = $2',
        [userId, 'CHAT']
      );
    } else {
      await db.query(
        'UPDATE chat_messages SET is_read = TRUE WHERE ride_id = $1',
        [rideId]
      );
    }
    res.json({ success: true, message: 'Messages marked as read' });
  } catch (err) {
    console.error('Error marking messages as read:', err);
    res.status(500).json({ success: false, error: 'Database error' });
  }
});

/**
 * 7. Mark all chat messages as read across all conversations
 */
router.put('/read/all', authenticateOptionalToken, async (req, res) => {
  try {
    const userId = req.user?.userId || req.headers['x-user-id'] || '';
    if (userId) {
      await db.query(
        'UPDATE chat_messages SET is_read = TRUE WHERE sender_id != $1',
        [userId]
      );
      await db.query(
        'UPDATE notifications SET is_read = TRUE WHERE user_id = $1 AND type = $2',
        [userId, 'CHAT']
      );
    } else {
      await db.query('UPDATE chat_messages SET is_read = TRUE');
    }
    res.json({ success: true, message: 'All messages marked as read' });
  } catch (err) {
    console.error('Error marking all messages as read:', err);
    res.status(500).json({ success: false, error: 'Database error' });
  }
});

module.exports = router;
