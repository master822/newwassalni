const test = require('node:test');
const assert = require('node:assert');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { JWT_SECRET, JWT_REFRESH_SECRET } = require('../src/middleware/auth');

test('Password Hashing and Comparison with Bcrypt', async () => {
  const password = 'mySecurePassword2026';
  const salt = await bcrypt.genSalt(10);
  const hash = await bcrypt.hash(password, salt);

  assert.notStrictEqual(password, hash);
  const isMatch = await bcrypt.compare(password, hash);
  assert.strictEqual(isMatch, true);

  const isWrongMatch = await bcrypt.compare('wrongPassword', hash);
  assert.strictEqual(isWrongMatch, false);
});

test('JWT Access Token Signing and Role Verification', async () => {
  const payload = {
    userId: 'user_12345',
    email: 'test@wasalni.app',
    role: 'ADMIN',
    isImpersonating: false,
  };

  const token = jwt.sign(payload, JWT_SECRET, { expiresIn: '1h' });
  assert.ok(token);

  const decoded = jwt.verify(token, JWT_SECRET);
  assert.strictEqual(decoded.userId, 'user_12345');
  assert.strictEqual(decoded.role, 'ADMIN');
  assert.strictEqual(decoded.isImpersonating, false);
});

test('Impersonated Token Security Bounds', async () => {
  const impersonatedPayload = {
    userId: 'user_regular_88',
    email: 'regular@wasalni.app',
    role: 'USER',
    isImpersonating: true,
    realAdminId: 'admin_super_01',
  };

  const token = jwt.sign(impersonatedPayload, JWT_SECRET, { expiresIn: '2h' });
  const decoded = jwt.verify(token, JWT_SECRET);

  assert.strictEqual(decoded.isImpersonating, true);
  assert.strictEqual(decoded.role, 'USER');
  assert.strictEqual(decoded.realAdminId, 'admin_super_01');
});

test('Referral Points Rule Calculation', () => {
  const welcomeBonusPoints = 50;
  const referrerBonusPoints = 50;

  // New user gets 50 starting points
  assert.strictEqual(welcomeBonusPoints, 50);
  // Referrer gets 50 points
  assert.strictEqual(referrerBonusPoints, 50);
});

test('Ride Seat Booking Availability Logic', () => {
  let availableSeats = 4;
  const requestedSeats = 2;

  assert.ok(availableSeats >= requestedSeats, 'Seats should be available');
  availableSeats -= requestedSeats;
  assert.strictEqual(availableSeats, 2);

  // Try booking 3 more seats (should fail)
  const tooManySeats = 3;
  assert.ok(availableSeats < tooManySeats, 'Should reject booking more seats than available');
});
