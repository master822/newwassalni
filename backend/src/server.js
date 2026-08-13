const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
require('dotenv').config();

const usersRoutes = require('./routes/users');
const ridesRoutes = require('./routes/rides');
const requestsRoutes = require('./routes/requests');
const walletRoutes = require('./routes/wallet');
const messagesRoutes = require('./routes/messages');
const adminRoutes = require('./routes/admin');
const db = require('./database');

const app = express();
const PORT = process.env.PORT || 3000;

// Security & Middleware
app.use(helmet());
app.use(cors({ origin: '*' }));
app.use(morgan('dev'));
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
app.use('/api/users', usersRoutes);
app.use('/api/rides', ridesRoutes);
app.use('/api/requests', requestsRoutes);
app.use('/api/wallet', walletRoutes);
app.use('/api/messages', messagesRoutes);
app.use('/api/admin', adminRoutes);

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('Unhandled server error:', err);
  res.status(500).json({
    success: false,
    error: 'Internal Server Error',
    message: process.env.NODE_ENV === 'production' ? undefined : err.message,
  });
});

if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, () => {
    console.log(`=========================================`);
    console.log(`🚗 Wassalni API Server running on port ${PORT}`);
    console.log(`📍 Health Check: http://localhost:${PORT}/health`);
    console.log(`=========================================`);
  });
}

module.exports = app;
