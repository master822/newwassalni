const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');

/**
 * 1. Get all requested trips
 */
router.get('/', async (req, res) => {
  try {
    const { status } = req.query;
    let query = 'SELECT * FROM requested_trips';
    const params = [];
    if (status) {
      query += ' WHERE status = $1';
      params.push(status);
    }
    query += ' ORDER BY created_at DESC';
    const result = await db.query(query, params);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching requested trips:', err);
    res.status(500).json({ success: false, error: 'Database query failed' });
  }
});

/**
 * 2. Publish new requested trip by passenger
 */
router.post('/', authenticateToken, async (req, res) => {
  try {
    const {
      startCity,
      endCity,
      departureDate,
      departureTime,
      menCount = 1,
      womenCount = 0,
      childrenCount = 0,
    } = req.body;

    if (!startCity || !endCity || !departureDate || !departureTime) {
      return res.status(400).json({ success: false, error: 'جميع تفاصيل طلب الرحلة مطلوبة' });
    }

    const userRes = await db.query('SELECT name, phone, avatar_url FROM users WHERE id = $1', [req.user.userId]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }
    const user = userRes.rows[0];

    const id = `req_${uuidv4().substring(0, 8)}`;
    const query = `
      INSERT INTO requested_trips 
      (id, user_id, user_name, user_phone, user_avatar, start_city, end_city, departure_date, departure_time, men_count, women_count, children_count, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 'OPEN')
      RETURNING *
    `;
    const values = [
      id,
      req.user.userId,
      user.name,
      user.phone,
      user.avatar_url || '',
      startCity,
      endCity,
      departureDate,
      departureTime,
      menCount,
      womenCount,
      childrenCount,
    ];
    const result = await db.query(query, values);
    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error publishing requested trip:', err);
    res.status(500).json({ success: false, error: 'Failed to publish trip request' });
  }
});

/**
 * 3. Driver accepts requested trip
 * - Atomic transaction with row locking
 * - Creates matching driver ride
 * - Sends notification to passenger
 */
router.post('/:id/accept', authenticateToken, async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { carModel, carColor, carPlate } = req.body;
    const driverId = req.user.userId;

    await client.query('BEGIN');

    // Lock requested trip row
    const checkRes = await client.query('SELECT * FROM requested_trips WHERE id = $1 FOR UPDATE', [id]);
    if (checkRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'طلب الرحلة غير موجود' });
    }

    const trip = checkRes.rows[0];

    // Prevent driver accepting their own requested trip
    if (trip.user_id === driverId) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'لا يمكنك قبول طلب رحلة أنشأته بنفسك' });
    }

    if (trip.status !== 'OPEN') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'طلب الرحلة تم قبوله مسبقاً من سائق آخر' });
    }

    // Get Driver details
    const driverRes = await client.query('SELECT name, avatar_url, rating, ride_count FROM users WHERE id = $1', [driverId]);
    const driver = driverRes.rows[0];

    // Update trip to ACCEPTED
    await client.query(
      'UPDATE requested_trips SET status = $1, accepted_by_driver_id = $2, accepted_by_driver_name = $3 WHERE id = $4',
      ['ACCEPTED', driverId, driver.name, id]
    );

    // Create corresponding driver ride
    const rideId = `ride_from_req_${id}`;
    const rideQuery = `
      INSERT INTO rides 
      (id, driver_id, driver_name, driver_avatar, start_city, end_city, departure_date, departure_time, duration, price_per_seat, available_seats, total_seats, car_model, car_color, car_plate, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, 'UPCOMING')
      ON CONFLICT (id) DO UPDATE SET 
        driver_id = EXCLUDED.driver_id,
        driver_name = EXCLUDED.driver_name,
        driver_avatar = EXCLUDED.driver_avatar,
        status = 'UPCOMING'
    `;
    await client.query(rideQuery, [
      rideId,
      driverId,
      driver.name,
      driver.avatar_url || '',
      trip.start_city,
      trip.end_city,
      trip.departure_date,
      trip.departure_time,
      '2 سا 30 د',
      5.0,
      trip.men_count + trip.women_count + trip.children_count,
      4,
      carModel || 'تويوتا كامري',
      carColor || 'فضي',
      carPlate || 'دمشق 123456',
    ]);

    // Send Notification to Passenger
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, '🚗 تم قبول طلب رحلتك!', $3, 'APPROVAL')`,
      [
        uuidv4(),
        trip.user_id,
        `قام الكابتن ${driver.name} بقبول طلب رحلتك من ${trip.start_city} إلى ${trip.end_city}. تفقد جدول رحلاتك للتواصل معه.`,
      ]
    );

    await client.query('COMMIT');
    res.json({ success: true, message: 'تم قبول طلب الرحلة بنجاح وإضافته إلى جدول رحلاتك' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error accepting requested trip:', err);
    res.status(500).json({ success: false, error: 'فشل في قبول طلب الرحلة' });
  } finally {
    client.release();
  }
});

/**
 * 4. Driver cancels accepted trip (Reopens to all other drivers)
 */
router.post('/:id/cancel-acceptance', authenticateToken, async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const driverId = req.user.userId;

    await client.query('BEGIN');

    const checkRes = await client.query('SELECT * FROM requested_trips WHERE id = $1 FOR UPDATE', [id]);
    if (checkRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'طلب الرحلة غير موجود' });
    }

    const trip = checkRes.rows[0];

    if (trip.accepted_by_driver_id !== driverId && req.user.role !== 'ADMIN' && req.user.role !== 'SUPER_ADMIN') {
      await client.query('ROLLBACK');
      return res.status(403).json({ success: false, error: 'غير مصرح لك بإلغاء قبول هذه الرحلة' });
    }

    // Reopen requested trip to OPEN
    await client.query(
      'UPDATE requested_trips SET status = $1, accepted_by_driver_id = NULL, accepted_by_driver_name = NULL WHERE id = $2',
      ['OPEN', id]
    );

    // Delete or cancel the driver's ride
    await client.query('DELETE FROM rides WHERE id = $1', [`ride_from_req_${id}`]);

    // Send Notification to Passenger
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, '🔄 إعادة إتاحة طلب رحلتك', $3, 'SYSTEM')`,
      [
        uuidv4(),
        trip.user_id,
        `اعتذر السائق عن الرحلة، وتمت إعادة فتح طلبك (${trip.start_city} ➔ ${trip.end_city}) ليقبله سائق آخر فوراً.`,
      ]
    );

    await client.query('COMMIT');
    res.json({
      success: true,
      message: 'تم إلغاء القبول وإعادة فتح الطلب لبقية السائقين بنجاح',
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error cancelling accepted requested trip:', err);
    res.status(500).json({ success: false, error: 'فشل في إلغاء قبول الرحلة' });
  } finally {
    client.release();
  }
});

/**
 * 5. Delete requested trip by passenger (or Admin)
 */
router.delete('/:id', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user.userId;

    const checkRes = await db.query('SELECT user_id FROM requested_trips WHERE id = $1', [id]);
    if (checkRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'طلب الرحلة غير موجود' });
    }

    const trip = checkRes.rows[0];
    if (trip.user_id !== userId && req.user.role !== 'ADMIN' && req.user.role !== 'SUPER_ADMIN') {
      return res.status(403).json({ success: false, error: 'غير مصرح لك بحذف هذا الطلب' });
    }

    await db.query('DELETE FROM requested_trips WHERE id = $1', [id]);
    await db.query('DELETE FROM rides WHERE id = $1', [`ride_from_req_${id}`]);
    res.json({ success: true, message: 'تم حذف طلب الرحلة بنجاح' });
  } catch (err) {
    console.error('Error deleting requested trip:', err);
    res.status(500).json({ success: false, error: 'فشل في حذف طلب الرحلة' });
  }
});

module.exports = router;
