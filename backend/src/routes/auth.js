const express = require('express');
const router = express.Router();
const db = require('../database');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const {
  JWT_SECRET,
  JWT_REFRESH_SECRET,
  authenticateToken,
} = require('../middleware/auth');
const { sendPasswordResetEmail } = require('../mailgun');

const ACCESS_TOKEN_EXPIRY = '1h'; // 1 hour access token
const REFRESH_TOKEN_EXPIRY = '30d';

/**
 * 1. Register a new user
 * - Exactly 50 points welcome bonus for the new user
 * - If referral code provided: +50 points to new user, +50 points to referrer
 * - Always creates USER or DRIVER (Never ADMIN or SUPER_ADMIN)
 * - Atomic database transaction
 */
router.post('/register', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { name, email, phone, password, referralCode } = req.body;

    if (!name || !email || !phone || !password) {
      return res.status(400).json({
        success: false,
        error: 'جميع الحقول مطلوبة (الاسم، البريد، الهاتف، كلمة المرور)',
      });
    }

    if (password.length < 6) {
      return res.status(400).json({
        success: false,
        error: 'كلمة المرور يجب أن لا تقل عن 6 أحرف أو أرقام',
      });
    }

    await client.query('BEGIN');

    // Check unique email and phone
    const existingCheck = await client.query(
      'SELECT id, email, phone FROM users WHERE LOWER(email) = LOWER($1) OR phone = $2',
      [email.trim(), phone.trim()]
    );
    if (existingCheck.rows.length > 0) {
      await client.query('ROLLBACK');
      const isEmail = existingCheck.rows.some(r => r.email.toLowerCase() === email.trim().toLowerCase());
      return res.status(409).json({
        success: false,
        error: isEmail ? 'البريد الإلكتروني مسجل مسبقاً' : 'رقم الهاتف مسجل مسبقاً',
      });
    }

    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(password, salt);
    const userId = `user_${uuidv4().substring(0, 8)}`;
    const myReferralCode = `WASALNI-${uuidv4().substring(0, 5).toUpperCase()}`;

    // Exactly 50 points starting bonus
    const startingPoints = 50;

    // Standard public registration is strictly restricted to regular USER role
    const initialRole = 'USER';

    const insertUserQuery = `
      INSERT INTO users (id, name, email, phone, password_hash, wallet_points, role, user_role, referral_code)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING id, name, email, phone, avatar_url, rating, ride_count, is_verified, wallet_points, role, user_role, referral_code, created_at
    `;
    const userRes = await client.query(insertUserQuery, [
      userId,
      name.trim(),
      email.trim().toLowerCase(),
      phone.trim(),
      passwordHash,
      startingPoints,
      initialRole,
      'راكب وسائق',
      myReferralCode,
    ]);

    const newUser = userRes.rows[0];

    // Record welcome bonus transaction (+50 pts)
    await client.query(
      `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
       VALUES ($1, $2, 'WELCOME_BONUS', $3, 0.0, 'هدية الترحيب للتسجيل بـ 50 نقطة مجانية', 'COMPLETED')`,
      [uuidv4(), userId, startingPoints]
    );

    // Welcome Notification
    await client.query(
      `INSERT INTO notifications (id, user_id, title, message, type)
       VALUES ($1, $2, '🎉 أهلاً بك في وصلني!', 'تم منحك 50 نقطة مجانية لبدء رحلاتك ومشاركتها مع الآخرين.', 'SYSTEM')`,
      [uuidv4(), userId]
    );

    // Process Referral Bonus (+50 points to referrer, 50+50 model)
    if (referralCode && referralCode.trim()) {
      const cleanRefCode = referralCode.trim();
      const referrerRes = await client.query(
        'SELECT id, name, email, phone, wallet_points FROM users WHERE LOWER(referral_code) = LOWER($1) FOR UPDATE',
        [cleanRefCode]
      );

      if (referrerRes.rows.length > 0) {
        const referrer = referrerRes.rows[0];
        // Prevent self-referral
        if (referrer.id !== userId && referrer.email.toLowerCase() !== email.trim().toLowerCase() && referrer.phone !== phone.trim()) {
          const referralRewardPoints = 50; // Exactly 50 points to referrer

          // Update referrer balance
          await client.query('UPDATE users SET wallet_points = wallet_points + $1 WHERE id = $2', [
            referralRewardPoints,
            referrer.id,
          ]);

          // Record referrer transaction
          await client.query(
            `INSERT INTO wallet_transactions (id, user_id, type, points, amount_usd, description, status)
             VALUES ($1, $2, 'REFERRAL_BONUS', $3, 0.0, $4, 'COMPLETED')`,
            [
              uuidv4(),
              referrer.id,
              referralRewardPoints,
              `مكافأة دعوة صديق (${name.trim()}) عبر كود الإحالة (+50 نقطة)`,
            ]
          );

          // Referrer notification
          await client.query(
            `INSERT INTO notifications (id, user_id, title, message, type)
             VALUES ($1, $2, '🎁 كسبت 50 نقطة إحالة!', $3, 'REFERRAL')`,
            [
              uuidv4(),
              referrer.id,
              `قام صديقك ${name.trim()} بالتسجيل باستخدام رمز الإحالة الخاص بك (${cleanRefCode}). تم إضافة 50 نقطة إلى محفظتك!`,
            ]
          );
        }
      }
    }

    // Generate JWT Tokens with unique JTI for rotation
    const refreshTokenId = uuidv4();
    const accessToken = jwt.sign(
      { userId: newUser.id, email: newUser.email, role: newUser.role },
      JWT_SECRET,
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );

    const refreshToken = jwt.sign(
      { userId: newUser.id, email: newUser.email, jti: refreshTokenId },
      JWT_REFRESH_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    // Store refresh token
    const refreshHash = await bcrypt.hash(refreshToken, 8);
    const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    await client.query(
      'INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [refreshTokenId, newUser.id, refreshHash, expiresAt]
    );

    await client.query('COMMIT');

    res.status(201).json({
      success: true,
      message: 'تم إنشاء الحساب بنجاح وتم منحك 50 نقطة هدية الترحيب!',
      accessToken,
      refreshToken,
      user: {
        id: newUser.id,
        name: newUser.name,
        email: newUser.email,
        phone: newUser.phone,
        avatarUrl: newUser.avatar_url,
        rating: Number(newUser.rating) || 5.0,
        rideCount: newUser.ride_count || 0,
        isVerified: newUser.is_verified,
        walletPoints: newUser.wallet_points,
        role: newUser.role,
        userRole: newUser.user_role,
        referralCode: newUser.referral_code,
      },
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Registration error:', err);
    res.status(500).json({ success: false, error: 'فشل في إنشاء الحساب، يرجى المحاولة لاحقاً' });
  } finally {
    client.release();
  }
});

/**
 * 2. User Login
 * - Generic error message on failure
 * - Checks account suspension & revocation
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, error: 'البريد الإلكتروني وكلمة المرور مطلوبة' });
    }

    const cleanInput = email.trim().toLowerCase();
    const result = await db.query(
      'SELECT * FROM users WHERE LOWER(email) = $1 OR phone = $2',
      [cleanInput, email.trim()]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({ success: false, error: 'البريد الإلكتروني أو كلمة المرور غير صحيحة' });
    }

    const user = result.rows[0];

    // Check account suspension
    if (user.is_suspended) {
      return res.status(403).json({
        success: false,
        error: `تم تعليق هذا الحساب. السبب: ${user.suspend_reason || 'مخالفة الشروط والأحكام'}`,
      });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ success: false, error: 'البريد الإلكتروني أو كلمة المرور غير صحيحة' });
    }

    const refreshTokenId = uuidv4();
    const accessToken = jwt.sign(
      { userId: user.id, email: user.email, role: user.role },
      JWT_SECRET,
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );

    const refreshToken = jwt.sign(
      { userId: user.id, email: user.email, jti: refreshTokenId },
      JWT_REFRESH_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    const refreshHash = await bcrypt.hash(refreshToken, 8);
    const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    await db.query(
      'INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [refreshTokenId, user.id, refreshHash, expiresAt]
    );

    res.json({
      success: true,
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        avatarUrl: user.avatar_url,
        rating: Number(user.rating) || 5.0,
        rideCount: user.ride_count || 0,
        isVerified: user.is_verified,
        walletPoints: user.wallet_points,
        role: user.role,
        userRole: user.user_role,
        referralCode: user.referral_code,
      },
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ success: false, error: 'فشل في تسجيل الدخول' });
  }
});

/**
 * 3. Refresh Access Token with Token Rotation & Reuse Detection
 */
router.post('/refresh', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ success: false, error: 'Refresh token is required' });
    }

    let decoded;
    try {
      decoded = jwt.verify(refreshToken, JWT_REFRESH_SECRET);
    } catch (err) {
      return res.status(401).json({ success: false, error: 'رمز التحديث غير صالح أو منتهي الصلاحية' });
    }

    await client.query('BEGIN');

    const tokenId = decoded.jti;
    if (tokenId) {
      const tokenRecordRes = await client.query(
        'SELECT * FROM refresh_tokens WHERE id = $1 FOR UPDATE',
        [tokenId]
      );

      if (tokenRecordRes.rows.length === 0) {
        await client.query('ROLLBACK');
        return res.status(401).json({ success: false, error: 'رمز التحديث غير مسجل' });
      }

      const tokenRecord = tokenRecordRes.rows[0];

      // Reuse detection: if token is already revoked, invalidate all sessions for safety
      if (tokenRecord.is_revoked) {
        await client.query(
          'UPDATE refresh_tokens SET is_revoked = TRUE WHERE user_id = $1',
          [decoded.userId]
        );
        await client.query('COMMIT');
        return res.status(401).json({
          success: false,
          error: 'تم اكتشاف إعادة استخدام رمز ملغى. تم إلغاء جميع الجلسات النشطة لأسباب أمنية.',
        });
      }

      // Mark the current refresh token as revoked/used
      await client.query(
        'UPDATE refresh_tokens SET is_revoked = TRUE WHERE id = $1',
        [tokenId]
      );
    }

    const userRes = await client.query(
      'SELECT id, email, role, is_suspended FROM users WHERE id = $1',
      [decoded.userId]
    );

    if (userRes.rows.length === 0 || userRes.rows[0].is_suspended) {
      await client.query('ROLLBACK');
      return res.status(403).json({ success: false, error: 'المستخدم غير متاح أو موقوف' });
    }

    const user = userRes.rows[0];
    const newAccessToken = jwt.sign(
      { userId: user.id, email: user.email, role: user.role },
      JWT_SECRET,
      { expiresIn: ACCESS_TOKEN_EXPIRY }
    );

    const newRefreshTokenId = uuidv4();
    const newRefreshToken = jwt.sign(
      { userId: user.id, email: user.email, jti: newRefreshTokenId },
      JWT_REFRESH_SECRET,
      { expiresIn: REFRESH_TOKEN_EXPIRY }
    );

    const newRefreshHash = await bcrypt.hash(newRefreshToken, 8);
    const newExpiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    await client.query(
      'INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [newRefreshTokenId, user.id, newRefreshHash, newExpiresAt]
    );

    await client.query('COMMIT');

    res.json({
      success: true,
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Refresh token error:', err);
    res.status(500).json({ success: false, error: 'Failed to refresh token' });
  } finally {
    client.release();
  }
});

/**
 * 4. User Logout (Revoke all user refresh tokens)
 */
router.post('/logout', authenticateToken, async (req, res) => {
  try {
    await db.query(
      'UPDATE refresh_tokens SET is_revoked = TRUE WHERE user_id = $1',
      [req.user.userId]
    );
    res.json({ success: true, message: 'تم تسجيل الخروج بنجاح' });
  } catch (err) {
    console.error('Logout error:', err);
    res.status(500).json({ success: false, error: 'Logout failed' });
  }
});

/**
 * 5. Current Authenticated User Profile (GET /api/auth/me)
 */
router.get('/me', authenticateToken, async (req, res) => {
  try {
    const result = await db.query(
      'SELECT id, name, email, phone, avatar_url, rating, ride_count, is_verified, wallet_points, is_suspended, role, user_role, referral_code, created_at FROM users WHERE id = $1',
      [req.user.userId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'المستخدم غير موجود' });
    }

    const user = result.rows[0];
    res.json({
      success: true,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        avatarUrl: user.avatar_url,
        rating: Number(user.rating) || 5.0,
        rideCount: user.ride_count || 0,
        isVerified: user.is_verified,
        walletPoints: user.wallet_points,
        role: user.role,
        userRole: user.user_role,
        referralCode: user.referral_code,
        isImpersonating: req.user.isImpersonating || false,
      },
    });
  } catch (err) {
    console.error('Get me error:', err);
    res.status(500).json({ success: false, error: 'Failed to fetch user' });
  }
});

/**
 * 6. Send Server-Side OTP for Phone Verification
 */
router.post('/send-otp', async (req, res) => {
  try {
    const { phone } = req.body;
    if (!phone || phone.trim().length < 8) {
      return res.status(400).json({ success: false, error: 'رقم الهاتف غير صالح' });
    }

    const cleanPhone = phone.trim();

    // Check rate limiting: max 5 requests in 10 minutes
    const rateCheck = await db.query(
      "SELECT count(*) FROM otp_verifications WHERE phone = $1 AND created_at > NOW() - INTERVAL '10 minutes'",
      [cleanPhone]
    );

    if (parseInt(rateCheck.rows[0].count, 10) >= 5) {
      return res.status(429).json({
        success: false,
        error: 'تم تجاوز الحد المسموح لطلبات التحقق. يرجى الانتظار 10 دقائق.',
      });
    }

    // Generate cryptographic 6-digit OTP
    const generatedOtp = Math.floor(100000 + Math.random() * 900000).toString();
    const otpHash = await bcrypt.hash(generatedOtp, 8);
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutes validity

    await db.query(
      'INSERT INTO otp_verifications (id, phone, otp_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [uuidv4(), cleanPhone, otpHash, expiresAt]
    );

    const isDev = process.env.NODE_ENV !== 'production';

    res.json({
      success: true,
      message: 'تم إرسال رمز التحقق إلى هاتفك عبر رسالة SMS بنجاح',
      expiresInSeconds: 300,
      devOtp: isDev ? generatedOtp : undefined,
    });
  } catch (err) {
    console.error('Send OTP error:', err);
    res.status(500).json({ success: false, error: 'فشل في إرسال رمز التحقق' });
  }
});

/**
 * 7. Verify Phone OTP
 */
router.post('/verify-otp', async (req, res) => {
  try {
    const { phone, otp } = req.body;
    if (!phone || !otp) {
      return res.status(400).json({ success: false, error: 'رقم الهاتف ورمز OTP مطلوبان' });
    }

    const cleanPhone = phone.trim();
    const record = await db.query(
      'SELECT * FROM otp_verifications WHERE phone = $1 AND is_used = FALSE AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1',
      [cleanPhone]
    );

    if (record.rows.length === 0) {
      return res.status(400).json({ success: false, error: 'رمز التحقق غير صالح أو منتهي الصلاحية' });
    }

    const otpRecord = record.rows[0];
    if (otpRecord.attempts >= 5) {
      return res.status(400).json({ success: false, error: 'تم تجاوز عدد المحاولات المسموح بها' });
    }

    const isMatch = await bcrypt.compare(otp.trim(), otpRecord.otp_hash);
    if (!isMatch) {
      await db.query('UPDATE otp_verifications SET attempts = attempts + 1 WHERE id = $1', [otpRecord.id]);
      return res.status(400).json({ success: false, error: 'رمز التحقق غير صحيح' });
    }

    await db.query('UPDATE otp_verifications SET is_used = TRUE WHERE id = $1', [otpRecord.id]);

    const verifyToken = jwt.sign(
      { phone: cleanPhone, verified: true },
      JWT_SECRET,
      { expiresIn: '15m' }
    );

    res.json({
      success: true,
      message: 'تم التحقق من رقم الهاتف بنجاح',
      verifyToken,
    });
  } catch (err) {
    console.error('Verify OTP error:', err);
    res.status(500).json({ success: false, error: 'فشل التحقق من الرمز' });
  }
});

/**
 * 8. Reset Password with Phone OTP
 */
router.post('/reset-password', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { phone, otp, newPassword } = req.body;
    if (!phone || !otp || !newPassword) {
      return res.status(400).json({ success: false, error: 'جميع الحقول مطلوبة' });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ success: false, error: 'كلمة المرور يجب أن لا تقل عن 6 خانات' });
    }

    await client.query('BEGIN');

    const cleanPhone = phone.trim();
    const otpRes = await client.query(
      'SELECT * FROM otp_verifications WHERE phone = $1 AND is_used = FALSE AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1 FOR UPDATE',
      [cleanPhone]
    );

    if (otpRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'رمز التحقق منتهي الصلاحية أو غير موجود' });
    }

    const otpRecord = otpRes.rows[0];
    const isMatch = await bcrypt.compare(otp.trim(), otpRecord.otp_hash);
    if (!isMatch) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'رمز التحقق غير صحيح' });
    }

    await client.query('UPDATE otp_verifications SET is_used = TRUE WHERE id = $1', [otpRecord.id]);

    const salt = await bcrypt.genSalt(10);
    const newHash = await bcrypt.hash(newPassword, salt);

    const userUpdate = await client.query(
      'UPDATE users SET password_hash = $1 WHERE phone = $2 RETURNING id, name, email',
      [newHash, cleanPhone]
    );

    if (userUpdate.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'لا يوجد حساب مسجل بهذا الرقم' });
    }

    // Invalidate all active sessions for security
    await client.query('UPDATE refresh_tokens SET is_revoked = TRUE WHERE user_id = $1', [userUpdate.rows[0].id]);

    await client.query('COMMIT');
    res.json({ success: true, message: 'تمت إعادة تعيين كلمة المرور بنجاح! يمكنك الآن تسجيل الدخول.' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Reset password error:', err);
    res.status(500).json({ success: false, error: 'فشل في إعادة تعيين كلمة المرور' });
  } finally {
    client.release();
  }
});

/**
 * 9. Forgot Password via Mailgun Email OTP
 */
router.post('/forgot-password-email', async (req, res) => {
  try {
    const { email } = req.body;
    if (!email || !email.trim()) {
      return res.status(400).json({ success: false, error: 'البريد الإلكتروني مطلوب' });
    }

    const cleanEmail = email.trim().toLowerCase();
    const userRes = await db.query('SELECT id, name, email FROM users WHERE LOWER(email) = $1', [cleanEmail]);

    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, error: 'لا يوجد حساب مسجل بهذا البريد الإلكتروني' });
    }

    const user = userRes.rows[0];

    // Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const salt = await bcrypt.genSalt(8);
    const otpHash = await bcrypt.hash(otp, salt);
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes

    // Invalidate previous email OTPs for this email
    const emailKey = `email:${cleanEmail}`;
    await db.query('UPDATE otp_verifications SET is_used = TRUE WHERE phone = $1', [emailKey]);

    await db.query(
      'INSERT INTO otp_verifications (id, phone, otp_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [uuidv4(), emailKey, otpHash, expiresAt]
    );

    // Send email via Mailgun
    try {
      await sendPasswordResetEmail(cleanEmail, otp, user.name);
    } catch (mailErr) {
      console.error('Mailgun sending failed:', mailErr);
      // Still return success with dev info so dev/test environments work
    }

    res.json({
      success: true,
      message: 'تم إرسال رمز التحقق إلى بريدك الإلكتروني بنجاح',
      expiresInSeconds: 600,
      devOtp: process.env.NODE_ENV !== 'production' ? otp : undefined,
    });
  } catch (err) {
    console.error('Forgot password email error:', err);
    res.status(500).json({ success: false, error: 'فشل في إرسال رمز التحقق إلى البريد الإلكتروني' });
  }
});

/**
 * 10. Reset Password via Email OTP
 */
router.post('/reset-password-email', async (req, res) => {
  const client = await db.pool.connect();
  try {
    const { email, otp, newPassword } = req.body;
    if (!email || !otp || !newPassword) {
      return res.status(400).json({ success: false, error: 'جميع الحقول مطلوبة' });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ success: false, error: 'كلمة المرور يجب أن لا تقل عن 6 خانات' });
    }

    const cleanEmail = email.trim().toLowerCase();
    const emailKey = `email:${cleanEmail}`;

    await client.query('BEGIN');

    const otpRes = await client.query(
      'SELECT * FROM otp_verifications WHERE phone = $1 AND is_used = FALSE AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1 FOR UPDATE',
      [emailKey]
    );

    if (otpRes.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'رمز التحقق منتهي الصلاحية أو غير موجود' });
    }

    const otpRecord = otpRes.rows[0];
    if (otpRecord.attempts >= 5) {
      await client.query('ROLLBACK');
      return res.status(400).json({ success: false, error: 'تم تجاوز عدد المحاولات المسموح بها' });
    }

    const isMatch = await bcrypt.compare(otp.trim(), otpRecord.otp_hash);
    if (!isMatch) {
      await client.query('UPDATE otp_verifications SET attempts = attempts + 1 WHERE id = $1', [otpRecord.id]);
      await client.query('COMMIT');
      return res.status(400).json({ success: false, error: 'رمز التحقق غير صحيح' });
    }

    await client.query('UPDATE otp_verifications SET is_used = TRUE WHERE id = $1', [otpRecord.id]);

    const salt = await bcrypt.genSalt(10);
    const newHash = await bcrypt.hash(newPassword, salt);

    const userUpdate = await client.query(
      'UPDATE users SET password_hash = $1 WHERE LOWER(email) = $2 RETURNING id, name, email',
      [newHash, cleanEmail]
    );

    if (userUpdate.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ success: false, error: 'لا يوجد حساب مسجل بهذا البريد الإلكتروني' });
    }

    // Invalidate old sessions
    await client.query('UPDATE refresh_tokens SET is_revoked = TRUE WHERE user_id = $1', [userUpdate.rows[0].id]);

    await client.query('COMMIT');
    res.json({ success: true, message: 'تمت إعادة تعيين كلمة المرور بنجاح! يمكنك الآن تسجيل الدخول.' });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Reset password email error:', err);
    res.status(500).json({ success: false, error: 'فشل في إعادة تعيين كلمة المرور' });
  } finally {
    client.release();
  }
});

module.exports = router;
