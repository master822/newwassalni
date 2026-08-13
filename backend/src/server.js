'use strict';

require("dotenv").config({
  path: require("path").join(__dirname, "..", ".env")
});

const express = require("express");
const cors = require("cors");

const db = require("./database");

const app = express();

app.use(cors());
app.use(express.json({ limit: "2mb" }));

// ===============================
// ROOT
// ===============================

app.get("/", (req, res) => {
  res.json({
    name: "Wasalni API",
    status: "online",
    version: "1.0.0"
  });
});

// ===============================
// HEALTH
// ===============================

app.get("/health", (req, res) => {
  res.json({
    ok: true,
    service: "wasalni-api",
    timestamp: Date.now()
  });
});

// ===============================
// STATS
// ===============================

app.get("/api/stats", async (req, res) => {
  try {
    const stats = await db.getStats();

    res.json({
      ok: true,
      data: stats
    });
  } catch (error) {
    console.error("GET STATS ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "GET_STATS_FAILED"
    });
  }
});

// ===============================
// GET USER BY TELEGRAM ID
// ===============================

app.get("/api/users/telegram/:telegramId", async (req, res) => {
  try {
    const user = await db.getUserByTelegramId(
      req.params.telegramId
    );

    if (!user) {
      return res.status(404).json({
        ok: false,
        error: "USER_NOT_FOUND"
      });
    }

    res.json({
      ok: true,
      data: user
    });
  } catch (error) {
    console.error("GET TELEGRAM USER ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "GET_USER_FAILED"
    });
  }
});

// ===============================
// CREATE / GET USER
// ===============================

app.post("/api/users", async (req, res) => {
  try {
    const {
      telegramId,
      name,
      phone,
      email,
      role,
      referralCode
    } = req.body;

    const user = await db.getOrCreateTelegramUser({
      telegram_id: telegramId || null,
      name,
      phone,
      email,
      role,
      referral_code: referralCode || null
    });

    res.json({
      ok: true,
      data: user
    });
  } catch (error) {
    console.error("CREATE USER ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "CREATE_USER_FAILED"
    });
  }
});

// ===============================
// UPDATE USER
// ===============================

app.patch("/api/users/:id", async (req, res) => {
  try {
    const user = await db.updateUser(
      Number(req.params.id),
      req.body
    );

    res.json({
      ok: true,
      data: user
    });
  } catch (error) {
    console.error("UPDATE USER ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "UPDATE_USER_FAILED"
    });
  }
});

// ===============================
// SEARCH RIDES
// ===============================

app.get("/api/rides", async (req, res) => {
  try {
    const rides = await db.searchRides(
      req.query.from || "",
      req.query.to || "",
      req.query.date || ""
    );

    res.json({
      ok: true,
      data: rides
    });
  } catch (error) {
    console.error("SEARCH RIDES ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "SEARCH_RIDES_FAILED"
    });
  }
});

// ===============================
// GET RIDE
// ===============================

app.get("/api/rides/:id", async (req, res) => {
  try {
    const ride = await db.getRideById(
      req.params.id
    );

    if (!ride) {
      return res.status(404).json({
        ok: false,
        error: "RIDE_NOT_FOUND"
      });
    }

    res.json({
      ok: true,
      data: ride
    });
  } catch (error) {
    console.error("GET RIDE ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "GET_RIDE_FAILED"
    });
  }
});

// ===============================
// CREATE RIDE
// ===============================

app.post("/api/rides", async (req, res) => {
  try {
    const ride = await db.createRide(
      req.body
    );

    res.status(201).json({
      ok: true,
      data: ride
    });
  } catch (error) {
    console.error("CREATE RIDE ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "CREATE_RIDE_FAILED"
    });
  }
});

// ===============================
// DRIVER RIDES
// ===============================

app.get("/api/users/:id/rides", async (req, res) => {
  try {
    const rides = await db.getRidesByDriver(
      Number(req.params.id)
    );

    res.json({
      ok: true,
      data: rides
    });
  } catch (error) {
    console.error("GET DRIVER RIDES ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "GET_DRIVER_RIDES_FAILED"
    });
  }
});

// ===============================
// CREATE BOOKING
// ===============================

app.post("/api/bookings", async (req, res) => {
  try {
    const booking = await db.createBooking(
      req.body.rideId,
      Number(req.body.passengerId),
      Number(req.body.seats || 1)
    );

    res.status(201).json({
      ok: true,
      data: booking
    });
  } catch (error) {
    console.error("CREATE BOOKING ERROR:", error);

    const errors = {
      RIDE_NOT_FOUND: 404,
      NOT_ENOUGH_SEATS: 409,
      SELF_BOOKING: 400,
      ALREADY_BOOKED: 409,
      INSUFFICIENT_BALANCE: 402
    };

    res.status(
      errors[error.message] || 500
    ).json({
      ok: false,
      error: error.message
    });
  }
});

// ===============================
// PASSENGER BOOKINGS
// ===============================

app.get("/api/users/:id/bookings", async (req, res) => {
  try {
    const bookings =
      await db.getBookingsByPassenger(
        Number(req.params.id)
      );

    res.json({
      ok: true,
      data: bookings
    });
  } catch (error) {
    console.error("GET BOOKINGS ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "GET_BOOKINGS_FAILED"
    });
  }
});

// ===============================
// WALLET
// ===============================

app.get("/api/users/:id/wallet", async (req, res) => {
  try {
    const user = await db.getUserById(
      Number(req.params.id)
    );

    if (!user) {
      return res.status(404).json({
        ok: false,
        error: "USER_NOT_FOUND"
      });
    }

    const transactions =
      await db.getWalletTransactions(
        user.id
      );

    res.json({
      ok: true,
      data: {
        points: user.wallet_points,
        transactions
      }
    });
  } catch (error) {
    console.error("GET WALLET ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "GET_WALLET_FAILED"
    });
  }
});

// ===============================
// TOPUP
// ===============================

app.post("/api/topups", async (req, res) => {
  try {
    const request =
      await db.createTopupRequest(
        Number(req.body.userId),
        Number(req.body.points),
        Number(req.body.amountUsd || 0)
      );

    res.status(201).json({
      ok: true,
      data: request
    });
  } catch (error) {
    console.error("CREATE TOPUP ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "CREATE_TOPUP_FAILED"
    });
  }
});

// ===============================
// REQUESTED TRIPS
// ===============================

app.get("/api/requests", async (req, res) => {
  try {
    const requests =
      await db.getRequestedTrips();

    res.json({
      ok: true,
      data: requests
    });
  } catch (error) {
    console.error("GET REQUESTS ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "GET_REQUESTS_FAILED"
    });
  }
});

app.post("/api/requests", async (req, res) => {
  try {
    const request =
      await db.createRequestedTrip(
        req.body
      );

    res.status(201).json({
      ok: true,
      data: request
    });
  } catch (error) {
    console.error("CREATE REQUEST ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "CREATE_REQUEST_FAILED"
    });
  }
});

// ===============================
// CHAT
// ===============================

app.get(
  "/api/rides/:rideId/messages",
  async (req, res) => {
    try {
      const messages =
        await db.getChatMessages(
          req.params.rideId
        );

      res.json({
        ok: true,
        data: messages
      });
    } catch (error) {
      console.error("GET MESSAGES ERROR:", error);

      res.status(500).json({
        ok: false,
        error: "GET_MESSAGES_FAILED"
      });
    }
  }
);

app.post("/api/messages", async (req, res) => {
  try {
    const message =
      await db.createChatMessage(
        req.body
      );

    res.status(201).json({
      ok: true,
      data: message
    });
  } catch (error) {
    console.error("CREATE MESSAGE ERROR:", error);

    res.status(500).json({
      ok: false,
      error: "CREATE_MESSAGE_FAILED"
    });
  }
});

// ===============================
// ERROR HANDLER
// ===============================

app.use((error, req, res, next) => {
  console.error("UNHANDLED ERROR:", error);

  if (res.headersSent) {
    return next(error);
  }

  res.status(500).json({
    ok: false,
    error: "INTERNAL_SERVER_ERROR"
  });
});

// ===============================
// START SERVER
// ===============================

const port = Number(
  process.env.PORT || 8080
);

app.listen(
  port,
  "0.0.0.0",
  () => {
    console.log("");
    console.log("====================================");
    console.log("      WASALNI API IS ONLINE");
    console.log("====================================");
    console.log(`Port: ${port}`);
    console.log("====================================");
    console.log("");
  }
);
