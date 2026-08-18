package com.example.data.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "accessToken") val accessToken: String? = null,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "user") val user: UserDto? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "rating") val rating: Float? = 5.0f,
    @Json(name = "ride_count") val rideCount: Int? = 0,
    @Json(name = "is_verified") val isVerified: Boolean? = true,
    @Json(name = "wallet_points") val walletPoints: Int? = 50,
    @Json(name = "is_suspended") val isSuspended: Boolean? = false,
    @Json(name = "suspend_reason") val suspendReason: String? = null,
    @Json(name = "role") val role: String? = "USER", // USER, DRIVER, ADMIN, SUPER_ADMIN
    @Json(name = "user_role") val userRole: String? = "راكب وسائق",
    @Json(name = "referral_code") val referralCode: String? = null,
    @Json(name = "isImpersonating") val isImpersonating: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "password") val password: String,
    @Json(name = "referralCode") val referralCode: String? = null,
    @Json(name = "verifyToken") val verifyToken: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "accessToken") val accessToken: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SendOtpRequest(
    @Json(name = "phone") val phone: String
)

@JsonClass(generateAdapter = true)
data class SendOtpResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "devOtp") val devOtp: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "otp") val otp: String
)

@JsonClass(generateAdapter = true)
data class VerifyOtpResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "verifyToken") val verifyToken: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "otp") val otp: String,
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "phone") val phone: String? = null
)

@JsonClass(generateAdapter = true)
data class FcmTokenRequest(
    @Json(name = "fcmToken") val fcmToken: String
)

@JsonClass(generateAdapter = true)
data class RideDto(
    @Json(name = "id") val id: String,
    @Json(name = "driver_id") val driverId: String,
    @Json(name = "driver_name") val driverName: String,
    @Json(name = "driver_avatar") val driverAvatar: String? = "",
    @Json(name = "driver_rating") val driverRating: Float? = 5.0f,
    @Json(name = "driver_trip_count") val driverTripCount: Int? = 0,
    @Json(name = "driver_verified") val driverVerified: Boolean? = true,
    @Json(name = "start_city") val startCity: String,
    @Json(name = "end_city") val endCity: String,
    @Json(name = "departure_date") val departureDate: String,
    @Json(name = "departure_time") val departureTime: String,
    @Json(name = "duration") val duration: String? = "2 سا",
    @Json(name = "price_per_seat") val pricePerSeat: Double = 5.0,
    @Json(name = "available_seats") val availableSeats: Int = 3,
    @Json(name = "total_seats") val totalSeats: Int = 4,
    @Json(name = "car_model") val carModel: String? = "تويوتا كامري",
    @Json(name = "car_color") val carColor: String? = "فضي",
    @Json(name = "car_plate") val carPlate: String? = "دمشق 123456",
    @Json(name = "allows_luggage") val allowsLuggage: Boolean? = true,
    @Json(name = "accept_cash") val acceptCash: Boolean? = true,
    @Json(name = "accept_wallet") val acceptWallet: Boolean? = true,
    @Json(name = "is_women_only") val isWomenOnly: Boolean? = false,
    @Json(name = "status") val status: String? = "UPCOMING"
)

@JsonClass(generateAdapter = true)
data class PublishRideRequest(
    @Json(name = "startCity") val startCity: String,
    @Json(name = "endCity") val endCity: String,
    @Json(name = "departureDate") val departureDate: String,
    @Json(name = "departureTime") val departureTime: String,
    @Json(name = "duration") val duration: String = "2 سا 30 د",
    @Json(name = "pricePerSeat") val pricePerSeat: Double = 5.0,
    @Json(name = "availableSeats") val availableSeats: Int = 3,
    @Json(name = "totalSeats") val totalSeats: Int = 4,
    @Json(name = "carModel") val carModel: String = "تويوتا كامري",
    @Json(name = "carColor") val carColor: String = "فضي",
    @Json(name = "carPlate") val carPlate: String = "دمشق 123456",
    @Json(name = "allowsLuggage") val allowsLuggage: Boolean = true,
    @Json(name = "acceptCash") val acceptCash: Boolean = true,
    @Json(name = "acceptWallet") val acceptWallet: Boolean = true,
    @Json(name = "isWomenOnly") val isWomenOnly: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BookRideRequest(
    @Json(name = "seats") val seats: Int = 1,
    @Json(name = "useWallet") val useWallet: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CancelRideRequest(
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class RequestedTripDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "user_phone") val userPhone: String,
    @Json(name = "user_avatar") val userAvatar: String? = "",
    @Json(name = "start_city") val startCity: String,
    @Json(name = "end_city") val endCity: String,
    @Json(name = "departure_date") val departureDate: String,
    @Json(name = "departure_time") val departureTime: String,
    @Json(name = "men_count") val menCount: Int = 1,
    @Json(name = "women_count") val womenCount: Int = 0,
    @Json(name = "children_count") val childrenCount: Int = 0,
    @Json(name = "status") val status: String = "OPEN",
    @Json(name = "accepted_by_driver_id") val acceptedByDriverId: String? = null,
    @Json(name = "accepted_by_driver_name") val acceptedByDriverName: String? = null
)

@JsonClass(generateAdapter = true)
data class PublishRequestedTripRequest(
    @Json(name = "startCity") val startCity: String,
    @Json(name = "endCity") val endCity: String,
    @Json(name = "departureDate") val departureDate: String,
    @Json(name = "departureTime") val departureTime: String,
    @Json(name = "menCount") val menCount: Int = 1,
    @Json(name = "womenCount") val womenCount: Int = 0,
    @Json(name = "childrenCount") val childrenCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class AcceptRequestedTripRequest(
    @Json(name = "carModel") val carModel: String? = "تويوتا كامري",
    @Json(name = "carColor") val carColor: String? = "فضي",
    @Json(name = "carPlate") val carPlate: String? = "دمشق 123456"
)

@JsonClass(generateAdapter = true)
data class WalletResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "walletPoints") val walletPoints: Int = 50,
    @Json(name = "transactions") val transactions: List<WalletTransactionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WalletTransactionDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "type") val type: String,
    @Json(name = "points") val points: Int,
    @Json(name = "amount_usd") val amountUsd: Double? = 0.0,
    @Json(name = "description") val description: String,
    @Json(name = "status") val status: String = "COMPLETED",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TopUpRequestPayload(
    @Json(name = "packagePoints") val packagePoints: Int,
    @Json(name = "packagePriceUsd") val packagePriceUsd: Double,
    @Json(name = "receiptImagePath") val receiptImagePath: String
)

@JsonClass(generateAdapter = true)
data class TopUpRequestDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "package_points") val packagePoints: Int,
    @Json(name = "package_price_usd") val packagePriceUsd: Double,
    @Json(name = "receipt_image_path") val receiptImagePath: String? = "",
    @Json(name = "status") val status: String = "PENDING",
    @Json(name = "rejection_reason") val rejectionReason: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "ride_id") val rideId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "sender_avatar") val senderAvatar: String? = "",
    @Json(name = "message") val message: String,
    @Json(name = "timestamp") val timestamp: String,
    @Json(name = "is_driver") val isDriver: Boolean = false,
    @Json(name = "image_uri") val imageUri: String? = null,
    @Json(name = "is_location") val isLocation: Boolean = false,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null
)

@JsonClass(generateAdapter = true)
data class SendChatMessageRequest(
    @Json(name = "message") val message: String,
    @Json(name = "imageUri") val imageUri: String? = null,
    @Json(name = "isLocation") val isLocation: Boolean = false,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "type") val type: String = "SYSTEM",
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AppSettingsDto(
    @Json(name = "appName") val appName: String = "وصلني",
    @Json(name = "appTagline") val appTagline: String = "نسافر معاً، نوصل بأمان",
    @Json(name = "appLogoUrl") val appLogoUrl: String = "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=300",
    @Json(name = "shamCashAccount") val shamCashAccount: String = "ba64858e96d4ad9c6096948bc2dbc970",
    @Json(name = "isMaintenanceMode") val isMaintenanceMode: Boolean = false,
    @Json(name = "ridePublishCost") val ridePublishCost: Int = 50,
    @Json(name = "appCommissionPercent") val appCommissionPercent: Double = 5.0,
    @Json(name = "cancellationRefundPercent") val cancellationRefundPercent: Int = 100,
    @Json(name = "appDownloadUrl") val appDownloadUrl: String = "https://wasalni.app/download"
)

@JsonClass(generateAdapter = true)
data class AdjustWalletRequest(
    @Json(name = "points") val points: Int,
    @Json(name = "reason") val reason: String
)

@JsonClass(generateAdapter = true)
data class ToggleSuspendRequest(
    @Json(name = "suspendReason") val suspendReason: String? = null
)

@JsonClass(generateAdapter = true)
data class RejectTopUpRequest(
    @Json(name = "reason") val reason: String
)

@JsonClass(generateAdapter = true)
data class BroadcastRequest(
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "targetAudience") val targetAudience: String = "ALL"
)

@JsonClass(generateAdapter = true)
data class SupportTicketDto(
    @Json(name = "id") val id: String,
    @Json(name = "user_name") val userName: String,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "subject") val subject: String,
    @Json(name = "message_text") val messageText: String,
    @Json(name = "priority") val priority: String = "MEDIUM",
    @Json(name = "status") val status: String = "OPEN",
    @Json(name = "admin_reply") val adminReply: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ReplyTicketRequest(
    @Json(name = "reply") val reply: String
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordEmailRequest(
    @Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordEmailRequest(
    @Json(name = "email") val email: String,
    @Json(name = "otp") val otp: String,
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class ImpersonateResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String? = null,
    @Json(name = "impersonatedToken") val impersonatedToken: String? = null,
    @Json(name = "user") val user: UserDto? = null
)
