const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const usersRoutes = require('./routes/users');
const ridesRoutes = require('./routes/rides');
const requestsRoutes = require('./routes/requests');
const walletRoutes = require('./routes/wallet');
const messagesRoutes = require('./routes/messages');
const notificationsRoutes = require('./routes/notifications');
const settingsRoutes = require('./routes/settings');
const adminRoutes = require('./routes/admin');
const db = require('./database');
const runMigration = require('./migrations/migrate');

const app = express();
const PORT = process.env.PORT || 3000;

// Security & Middleware
app.use(helmet());

const isProduction = process.env.NODE_ENV === 'production';
const allowedOrigins = process.env.CORS_ORIGINS
  ? process.env.CORS_ORIGINS.split(',').map(s => s.trim())
  : (isProduction ? ['https://api.wassalni.sy', 'https://wassalni.sy'] : '*');

app.use(cors({
  origin: (origin, callback) => {
    if (!origin || allowedOrigins === '*' || allowedOrigins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error('Blocked by CORS policy'));
  },
  credentials: true,
}));

if (process.env.NODE_ENV !== 'test') {
  app.use(morgan('dev'));
}
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Health Check
app.get('/health', async (req, res) => {
  try {
    const dbRes = await db.query('SELECT NOW()');
    res.json({
      status: 'HEALTHY',
      service: 'Wassalni Backend API',
      timestamp: new Date().toISOString(),
      database: 'CONNECTED',
      serverTime: dbRes.rows[0].now,
    });
  } catch (err) {
    res.status(500).json({
      status: 'DEGRADED',
      service: 'Wassalni Backend API',
      error: err.message,
    });
  }
});

// App Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/rides', ridesRoutes);
app.use('/api/requests', requestsRoutes);
app.use('/api/wallet', walletRoutes);
app.use('/api/messages', messagesRoutes);
app.use('/api/notifications', notificationsRoutes);
app.use('/api/settings', settingsRoutes);
app.use('/api/admin', adminRoutes);

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('Unhandled server error:', err);
  res.status(500).json({
    success: false,
    error: 'حدث خطأ في الخادم، يرجى المحاولة لاحقاً',
    message: process.env.NODE_ENV === 'production' ? undefined : err.message,
  });
});

if (process.env.NODE_ENV !== 'test') {
  // Run migration on startup then listen
  runMigration()
    .then(() => {
      app.listen(PORT, () => {
        console.log(`=========================================`);
        console.log(`🚗 Wassalni API Server running on port ${PORT}`);
        console.log(`📍 Health Check: http://localhost:${PORT}/health`);
        console.log(`=========================================`);
      });
    })
    .catch(err => {
      console.warn('⚠️ Starting server without automatic migration execution:', err.message);
      app.listen(PORT, () => {
        console.log(`🚗 Wassalni API Server running on port ${PORT}`);
      });
    });
}

module.exports = app;
