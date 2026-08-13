package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "user_default",
    val name: String = "أحمد المحمد",
    val email: String = "ahmed.m@wasalni.app",
    val phone: String = "+963 988 123 456",
    val avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
    val rating: Float = 4.9f,
    val rideCount: Int = 38,
    val isVerified: Boolean = true,
    val walletPoints: Int = 50,
    val isSuspended: Boolean = false,
    val suspendReason: String? = null,
    val registrationDate: String = "2026-01-15",
    val userRole: String = "راكب وسائق", // "سائق", "راكب", "راكب وسائق"
    val referralCode: String = "WASALNI-100"
)

data class AdminActivityLog(
    val id: String,
    val actionName: String,
    val details: String,
    val timestamp: String
)

data class AdminLoginLog(
    val id: String,
    val timestamp: String,
    val ipAddress: String,
    val deviceBrowser: String
)

data class SupportTicket(
    val id: String,
    val userName: String,
    val userEmail: String,
    val subject: String,
    val messageText: String,
    val priority: String, // HIGH, MEDIUM, LOW
    val status: String, // OPEN, RESOLVED
    val adminReply: String? = null,
    val dateText: String
)

data class HomeBannerItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val targetRoute: String,
    val isActive: Boolean = true
)

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val driverId: String,
    val driverName: String,
    val driverAvatar: String,
    val driverRating: Float,
    val driverTripCount: Int,
    val driverVerified: Boolean,
    val startCity: String,
    val endCity: String,
    val departureDate: String,
    val departureTime: String,
    val duration: String,
    val pricePerSeat: Double,
    val priceCurrency: String = "USD",
    val availableSeats: Int,
    val totalSeats: Int,
    val carModel: String,
    val carColor: String,
    val carPlate: String,
    val allowsLuggage: Boolean = true,
    val acceptCash: Boolean = true,
    val acceptWallet: Boolean = true,
    val isWomenOnly: Boolean = false,
    val status: String = "UPCOMING", // UPCOMING, COMPLETED, CANCELLED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ride_bookings")
data class RideBookingEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val passengerId: String,
    val passengerName: String,
    val seatsBooked: Int,
    val status: String = "UPCOMING",
    val bookingTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // TOP_UP, DEDUCTION
    val points: Int,
    val amountUsd: Double,
    val description: String,
    val status: String = "COMPLETED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "topup_requests")
data class TopUpRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val packagePoints: Int,
    val packagePriceUsd: Double,
    val receiptImagePath: String,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val senderId: String,
    val receiverId: String,
    val messageText: String,
    val imageUri: String? = null,
    val isLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isPaymentReminder: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "requested_trips")
data class RequestedTripEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val userAvatar: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
    val startCity: String,
    val endCity: String,
    val departureDate: String,
    val departureTime: String,
    val menCount: Int = 1,
    val womenCount: Int = 0,
    val childrenCount: Int = 0,
    val status: String = "OPEN", // OPEN, ACCEPTED, CANCELLED
    val acceptedByDriverId: String? = null,
    val acceptedByDriverName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

