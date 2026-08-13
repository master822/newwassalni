# Wassalni (وصلني) – Production Ride-Sharing & Intercity Carpooling Platform

> **الشعار:** نسافر معاً، نوصل بأمان  
> **Package Name:** `com.aistudio.wassalni.app`

---

## 🌟 Overview

**Wassalni (وصلني)** is a production-grade, RTL-first Android mobile application built with **Kotlin** and **Jetpack Compose**, accompanied by a robust **Node.js/PostgreSQL** backend. It enables seamless intercity carpooling, custom ride requests, digital wallet transactions, referral rewards, and in-app communication.

---

## 🚗 Core Features & Rules

1. **New User Welcome Gift**: Every newly registered user automatically receives **50 free wallet points** (points = 50, no more, no less).
2. **Referral Reward**: When a new user registers using a friend's referral code, the referrer receives **+100 wallet points**.
3. **Trip Request & Auto-Reopen**:
   - Passengers can pin customized trip requests specifying passenger breakdown (Men, Women, Children).
   - Drivers can accept open requests, which instantly moves the trip to their driving schedule.
   - **Cancellation & Auto-Reopening**: If a driver cancels their acceptance of a requested trip, the trip status **automatically reverts to `OPEN`** in the requested trips feed, allowing another driver to accept it immediately.
4. **Car & Ride Specifications**:
   - Publishing rides includes vehicle details: **Car Model**, **Color**, **License Plate Number**, and **Available Seats**.
5. **Private In-App Wallet & Sham Cash**:
   - Local and remote wallet ledger with points for booking rides.
   - Top-up requests submitted with payment proof receipts and reviewed by Admin.
6. **Administrative Dashboard**:
   - Admin credentials: `mastersniper823@gmail.com` / `sniper927MUHAMMAD`.
   - Manage user suspensions, approve/reject top-up requests with custom feedback, and view real-time activity logs.

---

## 📱 Tech Stack & Design System

- **Android Client**: Kotlin, Jetpack Compose, Material 3, Coroutines & Flow, Room Database, Coil, Retrofit.
- **Backend API**: Node.js, Express, PostgreSQL with SSL, bcrypt, JWT authentication.
- **Brand Palette**:
  - Primary Green: `#1E7A5F`
  - Dark Green: `#0F3D31`
  - Gold Accent: `#D4AF37`
  - Clean Light Surface: `#FFFFFF`
  - Soft Light Background: `#F5F7F6`
  - Text: `#202525`

---

## 🛠️ Build & Release Commands

### 1. Build Debug APK
```bash
bash scripts/build-debug.sh
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 2. Build Production Release APK
```bash
bash scripts/build-release.sh
# Output: app/build/outputs/apk/release/app-release.apk
```

### 3. Build Google Play App Bundle (AAB)
```bash
bash scripts/build-aab.sh
# Output: app/build/outputs/bundle/release/app-release.aab
```

### 4. Run Automated Tests
```bash
bash scripts/validate-project.sh
# Runs Robolectric unit and Critical User Journey tests
```

---

## 🚀 Backend Deployment & Telegram Bot Ready

The backend located in `/backend` is fully structured and ready to deploy on **Render**, **Heroku**, or any Linux VPS. It exposes REST endpoints consumed by both the Android application and future Telegram bot integrations.
