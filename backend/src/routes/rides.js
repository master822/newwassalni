const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');

// 1. Search and list rides
router.get('/', async (req, res) => {
  try {
    const { from, to, date } = req.query;
    let query = "SELECT * FROM rides WHERE status = 'UPCOMING'";
    const params = [];

    if (from && from.trim()) {
      params.push(`%${from.trim()}%`);
      query += ` AND start_city ILIKE $${params.length}`;
    }
    if (to && to.trim()) {
      params.push(`%${to.trim()}%`);
      query += ` AND end_city ILIKE $${params.length}`;
    }
    if (date && date.trim()) {
      params.push(date.trim());
      query += ` AND departure_date = $${params.length}`;
    }

    query += ' ORDER BY departure_date ASC, departure_time ASC';
    const result = await db.query(query, params);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching rides:', err);
    res.status(500).json({ success: false, error: 'Database search query failed' });
  }
});

// 2. Publish new ride by driver
router.post('/', async (req, res) => {
  try {
    const {
      driverId,
      driverName,
      driverAvatar,
      driverRating = 5.0,
      driverTripCount = 0,
      driverVerified = true,
      startCity,
      endCity,
      departureDate,
      departureTime,
      duration = '2 سا 30 د',
      pricePerSeat = 5.0,
      availableSeats = 3,
      totalSeats = 4,
      carModel = 'تويوتا كامري',
      carColor = 'فضي',
      carPlate = 'دمشق 123456',
      allowsLuggage = true,
      acceptCash = true,
      acceptWallet = true,
      isWomenOnly = false,
    } = req.body;

    const id = `ride_${uuidv4().substring(0, 8)}`;
    const query = `
      INSERT INTO rides 
      (id, driver_id, driver_name, driver_avatar, driver_rating, driver_trip_count, driver_verified,
       start_city, end_city, departure_date, departure_time, duration, price_per_seat, available_seats,
       total_seats, car_model, car_color, car_plate, allows_luggage, accept_cash, accept_wallet, is_women_only, status)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, 'UPCOMING')
      RETURNING *
    `;
    const values = [
      id,
      driverId,
      driverName,
      driverAvatar || '',
      driverRating,
      driverTripCount,
      driverVerified,
      startCity,
      endCity,
      departureDate,
      departureTime,
      duration,
      pricePerSeat,
      availableSeats,
      totalSeats,
      carModel,
      carColor,
      carPlate,
      allowsLuggage,
      acceptCash,
      acceptWallet,
      isWomenOnly,
    ];

    const result = await db.query(query, values);
    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error creating ride:', err);
    res.status(500).json({ success: false, error: 'Failed to publish ride' });
  }
});

// 3. Book a ride (passenger)
router.post('/:id/book', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { passengerId, passengerName, seats = 1, useWallet = true } = req.body;

    await client.query('BEGIN');

    // Fetch and lock ride
    const rideRes = await client.query('SELECT * FROM rides WHERE id = $1 FOR UPDATE', [id]);
    if (rideRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'Ride not found' });
    }

    const ride = rideRes.rows[0];
    if (ride.available_seats < seats) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'Not enough seats available' });
    }

    // If paying by wallet points
    if (useWallet) {
      const totalPointsNeeded = Math.round(Number(ride.price_per_seat) * 10 * seats);
      const userRes = await client.query('SELECT wallet_points FROM users WHERE id = $1 FOR UPDATE', [passengerId]);
      if (userRes.rows.length === 0 || userRes.rows[0].wallet_points < totalPointsNeeded) {
        await client.query('ROLLBACK');
        return res.status(402).json({
          success: false,
          error: 'رصيد المحفظة غير كافٍ لإتمام حجز الرحلة',
          requiredPoints: totalPointsNeeded,
        });
      }

      // Deduct points
      await client.query('UPDATE users SET wallet_points = wallet_points - $1 WHERE id = $2', [
        totalPointsNeeded,
        passengerId,
      ]);
      await client.query(
        `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
         VALUES ($1, $2, 'TRANSFER', $3, $4, $5, 'COMPLETED')`,
        [
          uuidv4(),
          passengerId,
          -totalPointsNeeded,
          ride.price_per_seat * seats,
          `دفع حجز رحلة ${ride.start_city} ➔ ${ride.end_city} (${seats} مقاعد)`,
        ]
      );
    }

    // Decrement seats
    await client.query('UPDATE rides SET available_seats = available_seats - $1 WHERE id = $2', [seats, id]);

    // Insert booking
    const bookingId = `book_${uuidv4().substring(0, 8)}`;
    await client.query(
      `INSERT INTO ride_bookings (id, ride_id, passenger_id, passenger_name, seats_booked, status)
       VALUES ($1, $2, $3, $4, $5, 'UPCOMING')`,
      [bookingId, id, passengerId, passengerName, seats]
    );

    // Notification
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'تم تأكيد حجز الرحلة', $3, 'BOOKING')`,
      [
        uuidv4(),
        passengerId,
        `حجزك لرحلة ${ride.start_city} ➔ ${ride.end_city} مؤكد مع السائق ${ride.driver_name}.`,
      ]
    );

    await client.query('COMMIT');
    res.json({ success: true, message: 'Ride booked successfully', bookingId });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error booking ride:', err);
    res.status(500).json({ success: false, error: 'Booking transaction failed' });
  } finally {
    client.release();
  }
});

// 4. Cancel Ride (Driver or Passenger)
router.post('/:id/cancel', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { userId } = req.body;

    await client.query('BEGIN');

    // If this ride is created from a requested trip (starts with ride_from_req_)
    if (id.startsWith('ride_from_req_')) {
      const reqId = id.replace('ride_from_req_', '');
      await client.query(
        'UPDATE requested_trips SET status = $1, accepted_by_driver_id = NULL, accepted_by_driver_name = NULL WHERE id = $2',
        ['OPEN', reqId]
      );
      await client.query('DELETE FROM rides WHERE id = $1', [id]);
    } else {
      await client.query("UPDATE rides SET status = 'CANCELLED' WHERE id = $1", [id]);
    }

    await client.query('COMMIT');
    res.json({ success: true, message: 'Ride cancelled successfully' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error cancelling ride:', err);
    res.status(500).json({ success: false, error: 'Cancellation failed' });
  } finally {
    client.release();
  }
});

module.exports = router;
