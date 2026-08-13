package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDao
import com.example.data.local.AppDatabase
import com.example.data.model.RequestedTripEntity
import com.example.data.model.RideEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WassalniRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.appDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testNewUserRegistrationStartingPointsIsExactly50() = runBlocking {
        val newUser = UserEntity(
            id = "user_test_50",
            name = "مستخدم تجريبي",
            email = "test50@wasalni.app",
            phone = "+963999111222",
            walletPoints = 50
        )
        dao.insertUser(newUser)

        val retrieved = dao.getUser("user_test_50")
        assertEquals(50, retrieved?.walletPoints)
    }

    @Test
    fun testRequestedTripAcceptanceAndDriverCancellationReopensTrip() = runBlocking {
        val testTrip = RequestedTripEntity(
            id = "req_cancellation_test",
            userId = "passenger_10",
            userName = "محمد السوري",
            userPhone = "+963955443322",
            startCity = "دمشق",
            endCity = "حمص",
            departureDate = "2026-08-10",
            departureTime = "10:00 AM",
            menCount = 2,
            womenCount = 1,
            childrenCount = 0,
            status = "OPEN"
        )
        dao.insertRequestedTrip(testTrip)

        // 1. Initial State: OPEN
        var trips = dao.getAllRequestedTrips().first()
        val initialTrip = trips.find { it.id == "req_cancellation_test" }
        assertEquals("OPEN", initialTrip?.status)
        assertNull(initialTrip?.acceptedByDriverId)

        // 2. Driver Accepts the Trip
        val driverId = "driver_99"
        val driverName = "خالد السائق"
        dao.updateRequestedTripStatus("req_cancellation_test", "ACCEPTED", driverId, driverName)
        val driverRide = RideEntity(
            id = "ride_from_req_req_cancellation_test",
            driverId = driverId,
            driverName = driverName,
            driverAvatar = "",
            driverRating = 5.0f,
            driverTripCount = 10,
            driverVerified = true,
            startCity = "دمشق",
            endCity = "حمص",
            departureDate = "2026-08-10",
            departureTime = "10:00 AM",
            duration = "2 سا",
            pricePerSeat = 5.0,
            availableSeats = 3,
            totalSeats = 4,
            carModel = "تويوتا كامري",
            carColor = "فضي",
            carPlate = "دمشق 123456",
            status = "UPCOMING"
        )
        dao.insertRide(driverRide)

        trips = dao.getAllRequestedTrips().first()
        val acceptedTrip = trips.find { it.id == "req_cancellation_test" }
        assertEquals("ACCEPTED", acceptedTrip?.status)
        assertEquals("driver_99", acceptedTrip?.acceptedByDriverId)

        // 3. Driver cancels accepted trip -> Re-opens for other drivers
        dao.updateRequestedTripStatus("req_cancellation_test", "OPEN", null, null)
        dao.deleteRide("ride_from_req_req_cancellation_test")

        trips = dao.getAllRequestedTrips().first()
        val reopenedTrip = trips.find { it.id == "req_cancellation_test" }
        assertEquals("OPEN", reopenedTrip?.status)
        assertNull(reopenedTrip?.acceptedByDriverId)
        assertNull(reopenedTrip?.acceptedByDriverName)

        // Verify driver ride is removed from driver's schedule
        val rides = dao.getAllRides().first()
        val foundRide = rides.find { it.id == "ride_from_req_req_cancellation_test" }
        assertNull(foundRide)
    }
}
