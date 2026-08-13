const express = require('express');
const router = express.Router();
const db = require('../database');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');

// 1. User Registration with 50 starting points bonus & referral code
router.post('/register', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { name, email, phone, password, referralCode } = req.body;

    if (!name || !email || !phone || !password) {
      return res.status(400).json({ success: false, error: 'All fields are required' });
    }

    await client.query('BEGIN');

    // Check unique email and phone
    const existingCheck = await client.query(
      'SELECT id, email, phone FROM users WHERE LOWER(email) = LOWER($1) OR phone = $2',
      [email.trim(), phone.trim()]
    );
    if (existingCheck.rows.length > 0) {
      await client.query('ROLLBACK');
      return res.status(409).json({
        success: false,
        error: 'البريد الإلكتروني أو رقم الهاتف مستخدم سابقاً للحساب! يمكنك التسجيل لمرة واحدة فقط.',
      });
    }

    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);
    const userId = `user_${uuidv4().substring(0, 6)}`;
    const myReferralCode = `WASALNI-${uuidv4().substring(0, 5).toUpperCase()}`;

    // Exactly 50 points starting bonus
    const initialPoints = 50;

    const insertUserQuery = `
      INSERT INTO users (id, name, email, phone, password_hash, wallet_points, referral_code)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING id, name, email, phone, avatar_url, wallet_points, referral_code, user_role, is_verified
    `;
    const userRes = await client.query(insertUserQuery, [
      userId,
      name.trim(),
      email.trim().toLowerCase(),
      phone.trim(),
      passwordHash,
      initialPoints,
      myReferralCode,
    ]);

    // Record 50 points bonus transaction
    await client.query(
      `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
       VALUES ($1, $2, 'TOP_UP', $3, 0.0, 'هدية الترحيب للتسجيل بـ 50 نقطة مجانية', 'COMPLETED')`,
      [uuidv4(), userId, initialPoints]
    );

    // Process Referral Bonus if code provided
    if (referralCode && referralCode.trim()) {
      const referrerRes = await client.query(
        'SELECT id, name, wallet_points FROM users WHERE LOWER(referral_code) = LOWER($1) FOR UPDATE',
        [referralCode.trim()]
      );
      if (referrerRes.rows.length > 0) {
        const referrer = referrerRes.rows[0];
        const newReferrerPoints = referrer.wallet_points + 100;
        await client.query('UPDATE users SET wallet_points = $1 WHERE id = $2', [
          newReferrerPoints,
          referrer.id,
        ]);
        await client.query(
          `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
           VALUES ($1, $2, 'TOP_UP', 100, 0.0, $3, 'COMPLETED')`,
          [uuidv4(), referrer.id, `مكافأة دعوة صديق (${name}) عبر كود الإحالة`]
        );
        await client.query(
          `INSERT INTO notifications (id, user_id, title, message, type)
           VALUES ($1, $2, '🎁 تم كسب 100 نقطة!', $3, 'REFERRAL')`,
          [
            uuidv4(),
            referrer.id,
            `قام صديقك ${name} بالتسجيل باستخدام رمز الإحالة الخاص بك ${referralCode}! تم إضافة 100 نقطة إلى محفظتك.`,
          ]
        );
      }
    }

    await client.query('COMMIT');
    res.status(201).json({
      success: true,
      message: 'تم إنشاء الحساب بنجاح وتم منحك 50 نقطة هدية الترحيب!',
      user: userRes.rows[0],
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Registration error:', err);
    res.status(500).json({ success: false, error: 'Registration failed' });
  } finally {
    client.release();
  }
});

// 2. User Login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, error: 'Email and password required' });
    }

    // Admin Hardcoded Fallback Check
    if (
      (email.toLowerCase() === 'mastersniper823@gmail.com' || email.toLowerCase() === 'mastersniper823@gmil.com') &&
      password === 'sniper927MUHAMMAD'
    ) {
      return res.json({
        success: true,
        isAdmin: true,
        user: {
          id: 'admin_master',
          name: 'المدير العام',
          email: 'mastersniper823@gmail.com',
          userRole: 'ADMIN',
          walletPoints: 9999,
          isVerified: true,
        },
      });
    }

    const result = await db.query('SELECT * FROM users WHERE LOWER(email) = LOWER($1)', [email.trim()]);
    if (result.rows.length === 0) {
      return res.status(401).json({ success: false, error: 'بيانات الدخول غير صحيحة' });
    }

    const user = result.rows[0];
    const match = await bcrypt.compare(password, user.password_hash);
    if (!match) {
      return res.status(401).json({ success: false, error: 'كلمة المرور غير صحيحة' });
    }

    delete user.password_hash;
    res.json({ success: true, isAdmin: false, user });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ success: false, error: 'Login server error' });
  }
});

// 3. Get User Profile
router.get('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const result = await db.query(
      'SELECT id, name, email, phone, avatar_url, rating, ride_count, is_verified, wallet_points, is_suspended, suspend_reason, user_role, referral_code, created_at FROM users WHERE id = $1',
      [id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'User not found' });
    }
    res.json({ success: true, data: result.rows[0] });
  } catch (err) {
    console.error('Fetch user error:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch user' });
  }
});

module.exports = router;
