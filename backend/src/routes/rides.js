const express = require('express');
const router = express.Router();
const db = require('../database');
const { v4: uuidv4 } = require('uuid');
const { authenticateToken } = require('../middleware/auth');

/**
 * 1. Search and list rides
 */
router.get('/', async (req, res) => {
  try {
    const { from, to, date, womenOnly, verifiedOnly } = req.query;
    let query = `
      SELECT r.*,
             COALESCE(NULLIF(u.avatar_url, ''), r.driver_avatar) AS driver_avatar,
             COALESCE(NULLIF(u.name, ''), r.driver_name) AS driver_name
      FROM rides r
      LEFT JOIN users u ON r.driver_id = u.id
      WHERE r.status = 'UPCOMING'
    `;
    const params = [];

    if (from && from.trim()) {
      params.push(`%${from.trim()}%`);
      query += ` AND r.start_city ILIKE $${params.length}`;
    }
    if (to && to.trim()) {
      params.push(`%${to.trim()}%`);
      query += ` AND r.end_city ILIKE $${params.length}`;
    }
    if (date && date.trim()) {
      params.push(date.trim());
      query += ` AND r.departure_date = $${params.length}`;
    }
    if (womenOnly === 'true' || womenOnly === true) {
      query += ` AND r.is_women_only = TRUE`;
    }
    if (verifiedOnly === 'true' || verifiedOnly === true) {
      query += ` AND r.driver_verified = TRUE`;
    }

    query += ' ORDER BY r.departure_date ASC, r.departure_time ASC';
    const result = await db.query(query, params);
    res.json({ success: true, data: result.rows });
  } catch (err) {
    console.error('Error fetching rides:', err);
    res.status(500).json({ success: false, error: 'Database search query failed' });
  }
});

/**
 * 2. Get single ride details
 */
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const rideRes = await db.query(
      `SELECT r.*,
              COALESCE(NULLIF(u.avatar_url, ''), r.driver_avatar) AS driver_avatar,
              COALESCE(NULLIF(u.name, ''), r.driver_name) AS driver_name
       FROM rides r
       LEFT JOIN users u ON r.driver_id = u.id
       WHERE r.id = $1`,
      [id]
    );
    if (rideRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'الرحلة غير موجودة' });
    }

    const ride = rideRes.rows[0];
    const bookingsRes = await db.query(
      'SELECT id, passenger_id, passenger_name, seats_booked, status, booked_at FROM ride_bookings WHERE ride_id = $1 AND status != \'CANCELLED\'',
      [id]
    );

    res.json({
      success: true,
      data: {
        ...ride,
        bookings: bookingsRes.rows,
      },
    });
  } catch (err) {
    console.error('Error fetching ride by id:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch ride' });
  }
});

/**
 * 3. Publish new ride by driver
 */
router.post('/', authenticateToken, async (req, res) => {
  try {
    const {
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

    if (!startCity || !endCity || !departureDate || !departureTime) {
      return res.status(400).json({ success: false, error: 'جميع تفاصيل انطلاق ومسار الرحلة مطلوبة' });
    }

    // Get Driver details
    const userRes = await db.query('SELECT name, avatar_url, rating, ride_count, is_verified FROM users WHERE id = $1', [req.user.userId]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }
    const driver = userRes.rows[0];

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
      req.user.userId,
      driver.name,
      driver.avatar_url || '',
      Number(driver.rating) || 5.0,
      driver.ride_count || 0,
      driver.is_verified,
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

    // Increment driver ride count
    await db.query('UPDATE users SET ride_count = ride_count + 1 WHERE id = $1', [req.user.userId]);

    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Error creating ride:', err);
    res.status(500).json({ success: false, error: 'Failed to publish ride' });
  }
});

/**
 * 4. Book a ride (passenger)
 * - Atomic seat reservation with row locking
 * - 100% Cash Payment to driver directly: NO wallet/points deduction from passenger
 */
router.post('/:id/book', authenticateToken, async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const { seats = 1 } = req.body;
    const passengerId = req.user.userId;

    if (seats < 1) {
      return res.status(400).json({ success: false, error: 'عدد المقاعد غير صالح' });
    }

    await client.query('BEGIN');

    // Fetch and lock ride row
    const rideRes = await client.query('SELECT * FROM rides WHERE id = $1 FOR UPDATE', [id]);
    if (rideRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'الرحلة غير متوفرة' });
    }

    const ride = rideRes.rows[0];

    // Prevent driver booking their own ride
    if (ride.driver_id === passengerId) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'لا يمكنك حجز مقعد في رحلتك الخاصة' });
    }

    if (ride.status !== 'UPCOMING') {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'هذه الرحلة لم تعد متاحة للحجز' });
    }

    if (ride.available_seats < seats) {
      await client.query('ROLLBACK');
      return res.status(400).json({
        success: false,
        error: `المقاعد المتبقية (${ride.available_seats}) لا تكفي لطلبك (${seats} مقاعد)`,
      });
    }

    const passengerRes = await client.query('SELECT name FROM users WHERE id = $1', [passengerId]);
    if (passengerRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }
    const passenger = passengerRes.rows[0];

    // Decrement available seats atomically
    await client.query('UPDATE rides SET available_seats = available_seats - $1 WHERE id = $2', [seats, id]);

    // Insert booking (Cash payment directly to driver)
    const bookingId = `book_${uuidv4().substring(0, 8)}`;
    await client.query(
      `INSERT INTO ride_bookings (id, ride_id, passenger_id, passenger_name, seats_booked, status)
       VALUES ($1, $2, $3, $4, $5, 'UPCOMING')`,
      [bookingId, id, passengerId, passenger.name, seats]
    );

    const totalCashAmount = (Number(ride.price_per_seat) * seats).toFixed(2);

    // Notify passenger
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'تم تأكيد حجز الرحلة بنجاح', $3, 'BOOKING')`,
      [
        uuidv4(),
        passengerId,
        `تم تأكيد حجز ${seats} مقاعد في رحلة ${ride.start_city} ➔ ${ride.end_city} مع الكابتن ${ride.driver_name}. طريقة الدفع: نقدًا للسائق مباشرة ($${totalCashAmount}).`,
      ]
    );

    // Notify driver
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, 'حجز جديد في رحلتك', $3, 'BOOKING')`,
      [
        uuidv4(),
        ride.driver_id,
        `قام الراكب ${passenger.name} بحجز ${seats} مقاعد في رحلتك ${ride.start_city} ➔ ${ride.end_city}. المبلغ المستحق نقدًا: $${totalCashAmount}.`,
      ]
    );

    await client.query('COMMIT');
    res.json({
      success: true,
      message: `تم تأكيد الحجز بنجاح. الدفع نقدًا للسائق مباشرة عند الرحلة ($${totalCashAmount}).`,
      bookingId,
      remainingSeats: ride.available_seats - seats,
      paymentMethod: 'CASH_TO_DRIVER',
      totalAmount: totalCashAmount,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error booking ride:', err);
    res.status(500).json({ success: false, error: 'فشل في إتمام عملية الحجز' });
  } finally {
    client.release();
  }
});

/**
 * 5. Cancel Ride (Driver or Passenger)
 */
router.post('/:id/cancel', authenticateToken, async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { id } = req.params;
    const userId = req.user.userId;

    await client.query('BEGIN');

    const rideRes = await client.query('SELECT * FROM rides WHERE id = $1 FOR UPDATE', [id]);
    if (rideRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'الرحلة غير موجودة' });
    }

    const ride = rideRes.rows[0];

    // Check authorization: Driver, Passenger with booking, or Admin
    const isDriver = ride.driver_id === userId;
    const bookingRes = await client.query(
      'SELECT * FROM ride_bookings WHERE ride_id = $1 AND passenger_id = $2 AND status = \'UPCOMING\'',
      [id, userId]
    );
    const isPassenger = bookingRes.rows.length > 0;
    const isAdmin = req.user.role === 'ADMIN' || req.user.role === 'SUPER_ADMIN';

    if (!isDriver && !isPassenger && !isAdmin) {
      await client.query('ROLLBACK');
      return res.status(403).json({ success: false, error: 'غير مصرح لك بإلغاء هذه الرحلة' });
    }

    if (isDriver || isAdmin) {
      // Driver cancels entire ride
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

      // Notify all booked passengers
      const passengers = await client.query('SELECT passenger_id FROM ride_bookings WHERE ride_id = $1', [id]);
      for (const p of passengers.rows) {
        await client.query(
          `INSERT INTO notifications (id, user_id, title, message, type)
           VALUES ($1, $2, 'إلغاء الرحلة', $3, 'SYSTEM')`,
          [
            uuidv4(),
            p.passenger_id,
            `تم إلغاء الرحلة المقررة من ${ride.start_city} إلى ${ride.end_city} من قبل السائق.`,
          ]
        );
      }
    } else if (isPassenger) {
      // Passenger cancels their booking
      const booking = bookingRes.rows[0];
      await client.query("UPDATE ride_bookings SET status = 'CANCELLED' WHERE id = $1", [booking.id]);
      await client.query('UPDATE rides SET available_seats = available_seats + $1 WHERE id = $2', [
        booking.seats_booked,
        id,
      ]);

      // Notify driver
      await client.query(
        `INSERT INTO notifications (id, user_id, title, message, type)
         VALUES ($1, $2, 'إلغاء حجز من قبل الراكب', $3, 'BOOKING')`,
        [
          uuidv4(),
          ride.driver_id,
          `قام الراكب ${booking.passenger_name} بإلغاء حجز ${booking.seats_booked} مقاعد في رحلتك.`,
        ]
      );
    }

    await client.query('COMMIT');
    res.json({ success: true, message: 'تم إلغاء الرحلة بنجاح' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error cancelling ride:', err);
    res.status(500).json({ success: false, error: 'فشل في إلغاء الرحلة' });
  } finally {
    client.release();
  }
});

/**
 * 6. Delete/Archive old, completed, or cancelled booking by passenger
 */
router.delete('/bookings/:id', authenticateToken, async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user.userId;

    const checkRes = await db.query('SELECT * FROM ride_bookings WHERE id = $1', [id]);
    if (checkRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'سجل الحجز غير موجود' });
    }

    const booking = checkRes.rows[0];
    if (booking.passenger_id !== userId && req.user.role !== 'ADMIN' && req.user.role !== 'SUPER_ADMIN') {
      return res.status(403).json({ success: false, error: 'غير مصرح لك بحذف هذا الحجز' });
    }

    await db.query('DELETE FROM ride_bookings WHERE id = $1', [id]);
    res.json({ success: true, message: 'تم حذف الرحلة من السجل بنجاح' });
  } catch (err) {
    console.error('Error deleting booking:', err);
    res.status(500).json({ success: false, error: 'فشل في حذف سجل الرحلة' });
  }
});

/**
 * 7. Delete booking by rideId for the authenticated passenger
 */
router.delete('/:rideId/my-booking', authenticateToken, async (req, res) => {
  try {
    const { rideId } = req.params;
    const userId = req.user.userId;

    await db.query('DELETE FROM ride_bookings WHERE ride_id = $1 AND passenger_id = $2', [rideId, userId]);
    res.json({ success: true, message: 'تم حذف الرحلة من السجل بنجاح' });
  } catch (err) {
    console.error('Error deleting passenger booking by rideId:', err);
    res.status(500).json({ success: false, error: 'فشل في حذف سجل الرحلة' });
  }
});

module.exports = router;
