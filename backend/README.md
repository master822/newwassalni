# Wassalni Backend API (وصلني)

Production-grade Node.js / Express / PostgreSQL backend service for the Wassalni ride-sharing platform.

## Features
- **Authentication**: Secure bcrypt hashing, unique email/phone validation, 50-point welcome bonus on registration, and referral code rewards (+100 points).
- **Rides Engine**: Create, search, filter rides by route/date/time, manage car specs (model, color, plate, seats).
- **Trip Requests with Auto-Reopen**: Passengers pin travel requests. Drivers accept them. If a driver cancels, the request automatically reverts to `OPEN` status so another driver can accept it immediately.
- **Wallet & Points**: Real-time balance updates, transactions ledger, and Sham Cash top-up approval flow with receipts.
- **Chat & Notifications**: Direct messaging on rides and transactional notifications.
- **Admin Control**: Moderate users, approve/reject top-up requests with custom notes.

## Setup & Running

```bash
cd backend
npm install
cp .env.example .env
# Edit .env with your PostgreSQL credentials
node src/server.js
```
