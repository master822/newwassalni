const express = require('express');
const bcrypt = require('bcryptjs');
const router = express.Router();
const db = require('../database');
const { authenticateToken, requireAdmin, requireSuperAdmin } = require('../middleware/auth');
const { v4: uuidv4 } = require('uuid');

router.use(authenticateToken, requireAdmin);

async function audit(req, action, details) {
  await db.query(
    `INSERT INTO admin_activity_logs (id, admin_id, admin_name, action_type, details, ip_address)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [uuidv4(), req.user.userId, req.user.email || 'مدير النظام', action, details, req.ip]
  );
}

router.get('/me', async (req, res) => {
  try {
    const r = await db.query(
      'SELECT id, name, email, phone, role, user_role, is_verified FROM users WHERE id = $1',
      [req.user.userId]
    );
    if (!r.rows.length) return res.status(404).json({ success: false, error: 'حساب المدير غير موجود' });
    res.json({ success: true, data: r.rows[0] });
  } catch (err) {
    console.error('Admin profile error:', err);
    res.status(500).json({ success: false, error: 'تعذر تحميل بيانات المدير' });
  }
});

router.put('/me', async (req, res) => {
  try {
    const { name, email, phone } = req.body;
    if (email !== undefined && !String(email).trim()) {
      return res.status(400).json({ success: false, error: 'البريد الإلكتروني لا يمكن أن يكون فارغاً' });
    }
    const cleanEmail = email === undefined ? null : String(email).trim().toLowerCase();
    const r = await db.query(
      `UPDATE users SET
         name = COALESCE($1, name),
         email = COALESCE($2, email),
         phone = COALESCE($3, phone),
         updated_at = CURRENT_TIMESTAMP
       WHERE id = $4
       RETURNING id, name, email, phone, role, user_role, is_verified`,
      [name === undefined ? null : String(name).trim(), cleanEmail, phone === undefined ? null : String(phone).trim(), req.user.userId]
    );
    if (!r.rows.length) return res.status(404).json({ success: false, error: 'حساب المدير غير موجود' });
    await audit(req, 'تعديل بيانات حساب المدير', `تم تعديل بيانات الحساب ${r.rows[0].email}`);
    res.json({ success: true, data: r.rows[0] });
  } catch (err) {
    console.error('Admin profile update error:', err);
    if (err.code === '23505') return res.status(409).json({ success: false, error: 'البريد أو الهاتف مستخدم مسبقاً' });
    res.status(500).json({ success: false, error: 'تعذر تعديل بيانات المدير' });
  }
});

router.post('/password', async (req, res) => {
  try {
    const { currentPassword, newPassword } = req.body;
    if (!currentPassword || !newPassword || String(newPassword).length < 10) {
      return res.status(400).json({ success: false, error: 'كلمة المرور الجديدة يجب أن تكون 10 محارف على الأقل' });
    }
    const r = await db.query('SELECT password_hash FROM users WHERE id = $1', [req.user.userId]);
    if (!r.rows.length) return res.status(404).json({ success: false, error: 'حساب المدير غير موجود' });
    if (!(await bcrypt.compare(String(currentPassword), r.rows[0].password_hash))) {
      return res.status(401).json({ success: false, error: 'كلمة المرور الحالية غير صحيحة' });
    }
    const hash = await bcrypt.hash(String(newPassword), 12);
    await db.query('UPDATE users SET password_hash = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2', [hash, req.user.userId]);
    await db.query('UPDATE refresh_tokens SET is_revoked = TRUE WHERE user_id = $1', [req.user.userId]);
    await audit(req, 'تغيير كلمة مرور المدير', 'تم تغيير كلمة مرور حساب الإدارة وإبطال جلسات التحديث السابقة');
    res.json({ success: true, message: 'تم تغيير كلمة المرور. سجّل الدخول مجدداً.' });
  } catch (err) {
    console.error('Admin password error:', err);
    res.status(500).json({ success: false, error: 'تعذر تغيير كلمة المرور' });
  }
});

router.put('/super-admin', requireSuperAdmin, async (req, res) => {
  try {
    const { userId, name, email, phone, password } = req.body;
    if (!userId) return res.status(400).json({ success: false, error: 'userId مطلوب' });
    const existing = await db.query('SELECT id FROM users WHERE id = $1', [userId]);
    if (!existing.rows.length) return res.status(404).json({ success: false, error: 'حساب المدير غير موجود' });
    let hash = null;
    if (password !== undefined && String(password).length > 0) {
      if (String(password).length < 10) return res.status(400).json({ success: false, error: 'كلمة المرور يجب أن تكون 10 محارف على الأقل' });
      hash = await bcrypt.hash(String(password), 12);
    }
    await db.query(
      `UPDATE users SET name = COALESCE($1,name), email = COALESCE($2,email), phone = COALESCE($3,phone),
       password_hash = COALESCE($4,password_hash), role = 'SUPER_ADMIN', is_verified = TRUE,
       is_suspended = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = $5`,
      [name || null, email ? String(email).trim().toLowerCase() : null, phone || null, hash, userId]
    );
    await audit(req, 'تعديل حساب مدير أعلى', `تم تحديث حساب المدير الأعلى ${userId}`);
    res.json({ success: true, message: 'تم تحديث حساب المدير الأعلى' });
  } catch (err) {
    console.error('Super admin update error:', err);
    if (err.code === '23505') return res.status(409).json({ success: false, error: 'البريد أو الهاتف مستخدم مسبقاً' });
    res.status(500).json({ success: false, error: 'تعذر تحديث حساب المدير الأعلى' });
  }
});

module.exports = router;
