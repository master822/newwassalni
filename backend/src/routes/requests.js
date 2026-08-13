const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');

// 1. Get all requested trips
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

// 2. Publish new requested trip
router.post('/', async (req, res) => {
  try {
    const {
      userId,
      userName,
      userPhone,
      userAvatar,
      startCity,
      endCity,
      departureDate,
      departureTime,
      menCount = 1,
      womenCount = 0,
      childrenCount = 0,
    } = req.body;

    const id = `req_${uuidv4().substring(0, 8)}`;
    const query = `
      INSERT INTO requested_trips 
      (id, user_id, user_name, user_phone, user_avatar, start_city, end_city, departure_date, departure_time, men_count, women_count, children_count, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 'OPEN')
      RETURNING *
    `;
    const values = [
      id,
      userId,
      userName,
      userPhone,
      userAvatar || '',
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

// 3. Driver accepts requested trip
router.post('/:id/accept', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { driverId, driverName, driverAvatar, carModel, carColor, carPlate } = req.body;

    await client.query('BEGIN');

    // Check if trip is OPEN
    const checkRes = await client.query('SELECT * FROM requested_trips WHERE id = $1 FOR UPDATE', [id]);
    if (checkRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'Requested trip not found' });
    }

    const trip = checkRes.rows[0];
    if (trip.status !== 'OPEN') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'Trip is already accepted or closed' });
    }

    // Update trip to ACCEPTED
    await client.query(
      'UPDATE requested_trips SET status = $1, accepted_by_driver_id = $2, accepted_by_driver_name = $3 WHERE id = $4',
      ['ACCEPTED', driverId, driverName, id]
    );

    // Create corresponding driver ride
    const rideId = `ride_from_req_${id}`;
    const rideQuery = `
      INSERT INTO rides 
      (id, driver_id, driver_name, driver_avatar, start_city, end_city, departure_date, departure_time, duration, price_per_seat, available_seats, total_seats, car_model, car_color, car_plate, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, 'UPCOMING')
      ON CONFLICT (id) DO UPDATE SET status = 'UPCOMING'
    `;
    await client.query(rideQuery, [
      rideId,
      driverId,
      driverName,
      driverAvatar || '',
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
    const notifId = uuidv4();
    await client.query(
      'INSERT INTO notifications (id, user_id, title, message, type) VALUES ($1, $2, $3, $4, $5)',
      [
        notifId,
        trip.user_id,
        '🚗 تم قبول طلب رحلتك',
        `قام السائق ${driverName} بقبول طلب رحلتك من ${trip.start_city} إلى ${trip.end_city}.`,
        'APPROVAL',
      ]
    );

    await client.query('COMMIT');
    res.json({ success: true, message: 'Requested trip accepted successfully' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error accepting requested trip:', err);
    res.status(500).json({ success: false, error: 'Transaction failed' });
  } finally {
    client.release();
  }
});

// 4. Driver cancels accepted trip (Reopens to all other drivers)
router.post('/:id/cancel-acceptance', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { driverId } = req.body;

    await client.query('BEGIN');

    const checkRes = await client.query('SELECT * FROM requested_trips WHERE id = $1 FOR UPDATE', [id]);
    if (checkRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'Requested trip not found' });
    }

    const trip = checkRes.rows[0];

    // Reopen requested trip to OPEN
    await client.query(
      'UPDATE requested_trips SET status = $1, accepted_by_driver_id = NULL, accepted_by_driver_name = NULL WHERE id = $2',
      ['OPEN', id]
    );

    // Delete or cancel the driver's ride
    await client.query('DELETE FROM rides WHERE id = $1', [`ride_from_req_${id}`]);

    // Send Notification to Passenger
    const notifId = uuidv4();
    await client.query(
      'INSERT INTO notifications (id, user_id, title, message, type) VALUES ($1, $2, $3, $4, $5)',
      [
        notifId,
        trip.user_id,
        '🔄 إعادة إتاحة طلب رحلتك',
        `اعتذر السائق عن الرحلة، وتمت إعادة فتح طلبك (${trip.start_city} ➔ ${trip.end_city}) ليقبله سائق آخر فوراً.`,
        'SYSTEM',
      ]
    );

    await client.query('COMMIT');
    res.json({
      success: true,
      message: 'Trip acceptance cancelled and reopened for other drivers successfully',
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error cancelling accepted requested trip:', err);
    res.status(500).json({ success: false, error: 'Cancellation transaction failed' });
  } finally {
    client.release();
  }
});

// 5. Delete requested trip by passenger
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    await db.query('DELETE FROM requested_trips WHERE id = $1', [id]);
    await db.query('DELETE FROM rides WHERE id = $1', [`ride_from_req_${id}`]);
    res.json({ success: true, message: 'Requested trip deleted successfully' });
  } catch (err) {
    console.error('Error deleting requested trip:', err);
    res.status(500).json({ success: false, error: 'Failed to delete requested trip' });
  }
});

module.exports = router;
