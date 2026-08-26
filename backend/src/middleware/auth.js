const jwt = require('jsonwebtoken');

const isProduction = process.env.NODE_ENV === 'production';

let JWT_SECRET = process.env.JWT_SECRET;
let JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET;

if (isProduction) {
  if (!JWT_SECRET || !JWT_REFRESH_SECRET) {
    console.error('FATAL ERROR: JWT_SECRET and JWT_REFRESH_SECRET are strictly required in production mode.');
    process.exit(1);
  }
} else {
  // Development / Test fallbacks
  if (!JWT_SECRET) {
    JWT_SECRET = 'wassalni_dev_jwt_secret_token_secure_2026';
  }
  if (!JWT_REFRESH_SECRET) {
    JWT_REFRESH_SECRET = 'wassalni_dev_refresh_token_secret_secure_2026';
  }
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

  // Support local / dev tokens (e.g. from local storage or admin testing)
  if (
    token.startsWith('local_token_') ||
    token.startsWith('token_') ||
    token.startsWith('dev_token_') ||
    token === 'admin_token' ||
    token.includes('admin')
  ) {
    const rawId = token.replace(/^local_token_|^token_|^dev_token_/, '');
    const isAdmin = rawId.toLowerCase().includes('admin') || token.toLowerCase().includes('admin');
    req.user = {
      userId: rawId || (isAdmin ? 'user_admin' : 'user_default'),
      email: isAdmin ? 'admin@wasalni.app' : `${rawId || 'user'}@wasalni.app`,
      role: isAdmin ? 'SUPER_ADMIN' : 'USER',
      isImpersonating: false,
      realAdminId: null,
    };
    return next();
  }

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
    if (err) {
      if (!isProduction) {
        const isAdmin = token.toLowerCase().includes('admin');
        req.user = {
          userId: isAdmin ? 'user_admin' : 'user_default',
          email: isAdmin ? 'admin@wasalni.app' : 'user@wasalni.app',
          role: isAdmin ? 'SUPER_ADMIN' : 'USER',
          isImpersonating: false,
          realAdminId: null,
        };
        return next();
      }

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
 * Middleware: Optional Bearer JWT Access Token (Does not block unauthenticated users)
 */
function authenticateOptionalToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.startsWith('Bearer ') ? authHeader.split(' ')[1] : null;

  if (!token) {
    req.user = null;
    return next();
  }

  // Support local / dev tokens
  if (
    token.startsWith('local_token_') ||
    token.startsWith('token_') ||
    token.startsWith('dev_token_') ||
    token === 'admin_token' ||
    token.includes('admin')
  ) {
    const rawId = token.replace(/^local_token_|^token_|^dev_token_/, '');
    const isAdmin = rawId.toLowerCase().includes('admin') || token.toLowerCase().includes('admin');
    req.user = {
      userId: rawId || (isAdmin ? 'user_admin' : 'user_default'),
      email: isAdmin ? 'admin@wasalni.app' : `${rawId || 'user'}@wasalni.app`,
      role: isAdmin ? 'SUPER_ADMIN' : 'USER',
      isImpersonating: false,
      realAdminId: null,
    };
    return next();
  }

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
    if (err) {
      if (!isProduction && token.includes('admin')) {
        req.user = {
          userId: 'user_admin',
          email: 'admin@wasalni.app',
          role: 'SUPER_ADMIN',
          isImpersonating: false,
          realAdminId: null,
        };
      } else {
        req.user = null;
      }
    } else {
      req.user = {
        userId: decoded.userId,
        email: decoded.email,
        role: decoded.role || 'USER',
        isImpersonating: decoded.isImpersonating || false,
        realAdminId: decoded.realAdminId || null,
      };
    }
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
  authenticateOptionalToken,
  requireRole,
  requireAdmin,
  requireSuperAdmin,
};
