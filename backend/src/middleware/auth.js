const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || (process.env.NODE_ENV === 'production' ? null : 'wassalni_dev_jwt_secret_token_secure_2026');
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || (process.env.NODE_ENV === 'production' ? null : 'wassalni_dev_refresh_token_secret_secure_2026');

if (process.env.NODE_ENV === 'production' && (!process.env.JWT_SECRET || !process.env.JWT_REFRESH_SECRET)) {
  console.error('FATAL: JWT_SECRET and JWT_REFRESH_SECRET environment variables are required in production.');
}

/**
 * Middleware: Verify Bearer JWT Access Token
 */
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : null;

  if (!token) {
    return res.status(401).json({
      success: false,
      error: 'لم يتم توفير رمز المصادقة (Unauthorized: Token missing)',
    });
  }

  const secret = JWT_SECRET || 'wassalni_dev_jwt_secret_token_secure_2026';

  jwt.verify(token, secret, (err, decoded) => {
    if (err) {
      const isExpired = err.name === 'TokenExpiredError';
      return res.status(401).json({
        success: false,
        error: isExpired ? 'انتهت صلاحية الجلسة، يرجى تحديث الرمز (Token Expired)' : 'رمز الدخول غير صالح (Invalid Token)',
        code: isExpired ? 'TOKEN_EXPIRED' : 'INVALID_TOKEN',
      });
    }

    req.user = {
      userId: decoded.userId,
      email: decoded.email,
      role: decoded.role || 'USER',
      isImpersonating: decoded.isImpersonating || false,
      realAdminId: decoded.realAdminId || null,
    };
    next();
  });
}

/**
 * Middleware: Role-Based Authorization
 */
function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, error: 'غير مصرح (Unauthorized)' });
    }

    // Impersonated tokens are explicitly blocked from executing Admin privileges
    if (req.user.isImpersonating && (allowedRoles.includes('ADMIN') || allowedRoles.includes('SUPER_ADMIN'))) {
      return res.status(403).json({
        success: false,
        error: 'محظور: لا يمكن استخدام صلاحيات الأدمن أثناء تقمص هوية مستخدم عادي',
      });
    }

    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        error: 'ليس لديك الصلاحيات الكافية للوصول إلى هذا المورد (Forbidden: Insufficient Permissions)',
      });
    }
    next();
  };
}

const requireAdmin = requireRole('ADMIN', 'SUPER_ADMIN');
const requireSuperAdmin = requireRole('SUPER_ADMIN');

module.exports = {
  JWT_SECRET,
  JWT_REFRESH_SECRET,
  authenticateToken,
  requireRole,
  requireAdmin,
  requireSuperAdmin,
};
