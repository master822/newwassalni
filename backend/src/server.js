require("dotenv").config({
  path: require("path").join(
    __dirname,
    "..",
    ".env"
  )
});

const express = require("express");
const cors = require("cors");

const db =
  require("./database");

const app = express();

app.use(
  cors()
);

app.use(
  express.json({
    limit: "2mb"
  })
);

app.get(
  "/",
  (req, res) => {
    res.json({
      name: "Wasalni API",
      status: "online",
      version: "1.0.0"
    });
  }
);

app.get(
  "/health",
  (req, res) => {
    res.json({
      ok: true,
      service: "wasalni-api",
      timestamp: Date.now()
    });
  }
);

app.get(
  "/api/stats",
  (req, res) => {
    res.json({
      ok: true,
      data: db.getStats()
    });
  }
);

app.get(
  "/api/users/telegram/:telegramId",
  (req, res) => {
    const user =
      db.getUserByTelegramId(
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
  }
);

app.post(
  "/api/users",
  (req, res) => {
    try {
      const {
        telegramId,
        name,
        phone,
        email,
        role,
        referralCode
      } = req.body;

      const user =
        db.getOrCreateTelegramUser({
          telegram_id:
            telegramId || null,
          name,
          phone,
          email,
          role,
          referral_code:
            referralCode || null
        });

      res.json({
        ok: true,
        data: user
      });
    } catch (error) {
      console.error(error);

      res.status(500).json({
        ok: false,
        error: "CREATE_USER_FAILED"
      });
    }
  }
);

app.patch(
  "/api/users/:id",
  (req, res) => {
    try {
      const user =
        db.updateUser(
          Number(req.params.id),
          req.body
        );

      res.json({
        ok: true,
        data: user
      });
    } catch (error) {
      console.error(error);

      res.status(500).json({
        ok: false,
        error: "UPDATE_USER_FAILED"
      });
    }
  }
);

app.get(
  "/api/rides",
  (req, res) => {
    const rides =
      db.searchRides(
        req.query.from || "",
        req.query.to || "",
        req.query.date || ""
      );

    res.json({
      ok: true,
      data: rides
    });
  }
);

app.get(
  "/api/rides/:id",
  (req, res) => {
    const ride =
      db.getRideById(
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
  }
);

app.post(
  "/api/rides",
  (req, res) => {
    try {
      const ride =
        db.createRide(
          req.body
        );

      res.status(201).json({
        ok: true,
        data: ride
      });
    } catch (error) {
      console.error(error);

      res.status(500).json({
        ok: false,
        error: "CREATE_RIDE_FAILED"
      });
    }
  }
);

app.get(
  "/api/users/:id/rides",
  (req, res) => {
    res.json({
      ok: true,
      data: db.getRidesByDriver(
        Number(req.params.id)
      )
    });
  }
);

app.post(
  "/api/bookings",
  (req, res) => {
    try {
      const booking =
        db.createBooking(
          req.body.rideId,
          Number(
            req.body.passengerId
          ),
          Number(
            req.body.seats || 1
          )
        );

      res.status(201).json({
        ok: true,
        data: booking
      });
    } catch (error) {
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
  }
);

app.get(
  "/api/users/:id/bookings",
  (req, res) => {
    res.json({
      ok: true,
      data:
        db.getBookingsByPassenger(
          Number(req.params.id)
        )
    });
  }
);

app.get(
  "/api/users/:id/wallet",
  (req, res) => {
    const user =
      db.getUserById(
        Number(req.params.id)
      );

    if (!user) {
      return res.status(404).json({
        ok: false,
        error: "USER_NOT_FOUND"
      });
    }

    res.json({
      ok: true,
      data: {
        points: user.wallet_points,
        transactions:
          db.getWalletTransactions(
            user.id
          )
      }
    });
  }
);

app.post(
  "/api/topups",
  (req, res) => {
    try {
      const request =
        db.createTopupRequest(
          Number(req.body.userId),
          Number(req.body.points),
          Number(
            req.body.amountUsd || 0
          )
        );

      res.status(201).json({
        ok: true,
        data: request
      });
    } catch (error) {
      console.error(error);

      res.status(500).json({
        ok: false,
        error: "CREATE_TOPUP_FAILED"
      });
    }
  }
);

app.get(
  "/api/requests",
  (req, res) => {
    res.json({
      ok: true,
      data:
        db.getRequestedTrips()
    });
  }
);

app.post(
  "/api/requests",
  (req, res) => {
    try {
      const request =
        db.createRequestedTrip(
          req.body
        );

      res.status(201).json({
        ok: true,
        data: request
      });
    } catch (error) {
      console.error(error);

      res.status(500).json({
        ok: false,
        error: "CREATE_REQUEST_FAILED"
      });
    }
  }
);

app.get(
  "/api/rides/:rideId/messages",
  (req, res) => {
    res.json({
      ok: true,
      data:
        db.getChatMessages(
          req.params.rideId
        )
    });
  }
);

app.post(
  "/api/messages",
  (req, res) => {
    try {
      const message =
        db.createChatMessage(
          req.body
        );

      res.status(201).json({
        ok: true,
        data: message
      });
    } catch (error) {
      console.error(error);

      res.status(500).json({
        ok: false,
        error: "CREATE_MESSAGE_FAILED"
      });
    }
  }
);

const port =
  Number(process.env.PORT || 8080);

app.listen(
  port,
  "0.0.0.0",
  () => {
    console.log("");
    console.log(
      "===================================="
    );
    console.log(
      "      WASALNI API IS ONLINE"
    );
    console.log(
      "===================================="
    );
    console.log(
      `http://0.0.0.0:${port}`
    );
    console.log(
      `Health: http://127.0.0.1:${port}/health`
    );
    console.log(
      "===================================="
    );
    console.log("");
  }
);
