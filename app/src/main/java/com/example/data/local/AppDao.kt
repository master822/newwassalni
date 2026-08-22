package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.data.model.*

@Dao
interface AppDao {

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY registrationDate DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserEntity? // ملاحظة: تم الحفاظ على البنية الأصلية

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isSuspended = :isSuspended, suspendReason = :reason WHERE id = :userId")
    suspend fun updateUserSuspension(userId: String, isSuspended: Boolean, reason: String?)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM rides WHERE id = :rideId")
    suspend fun deleteRide(rideId: String)

    @Query("UPDATE users SET walletPoints = walletPoints + :points WHERE id = :userId")
    suspend fun addWalletPoints(userId: String, points: Int)

    @Query("UPDATE users SET walletPoints = walletPoints - :points WHERE id = :userId")
    suspend fun deductWalletPoints(userId: String, points: Int)

    @Query("SELECT * FROM rides")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE driverId = :driverId")
    fun getRidesByDriver(driverId: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideById(rideId: String): RideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRides(rides: List<RideEntity>)

    @Query("UPDATE rides SET status = :status WHERE id = :rideId")
    suspend fun updateRideStatus(rideId: String, status: String)

    @Query("UPDATE rides SET availableSeats = availableSeats - :seats WHERE id = :rideId")
    suspend fun decrementAvailableSeats(rideId: String, seats: Int)

    @Query("SELECT * FROM ride_bookings WHERE passengerId = :passengerId")
    fun getBookingsByPassenger(passengerId: String): Flow<List<RideBookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: RideBookingEntity)

    @Query("UPDATE ride_bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: String, status: String)

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getWalletTransactions(userId: String): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAt DESC")
    fun getAllWalletTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransaction(tx: WalletTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransactions(transactions: List<WalletTransactionEntity>)

    @Query("SELECT * FROM topup_requests ORDER BY createdAt DESC")
    fun getAllTopUpRequests(): Flow<List<TopUpRequestEntity>>

    @Query("SELECT * FROM topup_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTopUpRequestsByUser(userId: String): Flow<List<TopUpRequestEntity>>

    @Query("SELECT * FROM topup_requests WHERE id = :requestId")
    suspend fun getTopUpRequestById(requestId: String): TopUpRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopUpRequest(request: TopUpRequestEntity)

    @Query("UPDATE topup_requests SET status = :status, rejectionReason = :reason WHERE id = :requestId")
    suspend fun updateTopUpRequestStatus(requestId: String, status: String, reason: String?)

    // تمت الإضافة لحذف طلب الشحن فوراً عند القبول/الرفض
    @Query("DELETE FROM topup_requests WHERE id = :requestId")
    suspend fun deleteTopUpRequest(requestId: String)

    @Query("SELECT * FROM chat_messages WHERE rideId = :rideId ORDER BY timestamp ASC")
    fun getChatMessages(rideId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET messageText = :newText WHERE id = :messageId")
    suspend fun updateChatMessageText(messageId: String, newText: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteChatMessage(messageId: String)

    @Query("DELETE FROM chat_messages WHERE rideId = :rideId")
    suspend fun deleteChatMessagesForRide(rideId: String)

    @Query("UPDATE users SET name = :name, phone = :phone, userRole = :role, walletPoints = :points WHERE id = :userId")
    suspend fun updateUserData(userId: String, name: String, phone: String, role: String, points: Int)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotifications(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: String)

    // تمت الإضافة لحذف الإشعارات القديمة نهائياً
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: String)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: String)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun clearUserNotifications(userId: String)

    @Query("SELECT * FROM requested_trips")
    fun getAllRequestedTrips(): Flow<List<RequestedTripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequestedTrip(trip: RequestedTripEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequestedTrips(trips: List<RequestedTripEntity>)

    @Query("UPDATE requested_trips SET status = :status, acceptedByDriverId = :driverId, acceptedByDriverName = :driverName WHERE id = :requestId")
    suspend fun updateRequestedTripStatus(requestId: String, status: String, driverId: String?, driverName: String?)

    @Query("UPDATE requested_trips SET startCity = :start, endCity = :end, departureDate = :date, departureTime = :time, menCount = :men, womenCount = :women, childrenCount = :children WHERE id = :requestId")
    suspend fun updateRequestedTripDetails(requestId: String, start: String, end: String, date: String, time: String, men: Int, women: Int, children: Int)

    @Query("DELETE FROM requested_trips WHERE id = :requestId")
    suspend fun deleteRequestedTrip(requestId: String)

    @Query("UPDATE rides SET startCity = :start, endCity = :end, departureDate = :date, departureTime = :time, pricePerSeat = :price, availableSeats = :seats WHERE id = :rideId")
    suspend fun updateRideDetails(rideId: String, start: String, end: String, date: String, time: String, price: Double, seats: Int)

    @Query("DELETE FROM wallet_transactions WHERE id = :txId")
    suspend fun deleteWalletTransaction(txId: String)


    @Query("UPDATE users SET walletPoints = :points WHERE id = :userId")
    suspend fun updateUserWalletPoints(
        userId: String,
        points: Int
    )
}
