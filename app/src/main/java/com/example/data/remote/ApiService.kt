package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("api/rides")
    suspend fun getRides(): Response<ApiResponse<List<RideDto>>>

    @POST("api/users")
    suspend fun createOrGetUser(@Body request: CreateUserRequest): Response<ApiResponse<BackendUserDto>>

    @GET("api/requests")
    suspend fun getRequestedTrips(): Response<ApiResponse<List<RequestedTripDto>>>

    @POST("api/requests")
    suspend fun createRequestedTrip(@Body request: CreateRequestedTripRequest): Response<ApiResponse<RequestedTripDto>>

    @POST("api/requests/{requestId}/accept")
    suspend fun acceptRequestedTrip(
        @Path("requestId") requestId: String,
        @Body request: AcceptRequestedTripRequest
    ): Response<ApiResponse<RequestedTripDto>>
}

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val ok: Boolean,
    val service: String?,
    val timestamp: Long?
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T?
)

@JsonClass(generateAdapter = true)
data class CreateUserRequest(
    val telegramId: String? = null,
    val name: String?,
    val phone: String?,
    val email: String?,
    val role: String? = null,
    val referralCode: String? = null
)

@JsonClass(generateAdapter = true)
data class BackendUserDto(
    val id: Long,
    @Json(name = "telegram_id") val telegramId: String?,
    val name: String?,
    val email: String?,
    val phone: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    val rating: Float?,
    @Json(name = "rating_count") val ratingCount: Int?,
    @Json(name = "ride_count") val rideCount: Int?,
    @Json(name = "wallet_points") val walletPoints: Int?,
    @Json(name = "is_verified") val isVerified: Int?,
    @Json(name = "is_suspended") val isSuspended: Int?,
    @Json(name = "referral_code") val referralCode: String?
)

@JsonClass(generateAdapter = true)
data class RideDto(
    val id: String,
    @Json(name = "driver_id") val driverId: String?,
    @Json(name = "driver_name") val driverName: String?,
    @Json(name = "driver_avatar") val driverAvatar: String?,
    @Json(name = "driver_rating") val driverRating: Float?,
    @Json(name = "driver_trip_count") val driverTripCount: Int?,
    @Json(name = "driver_verified") val driverVerified: Boolean?,
    @Json(name = "start_city") val startCity: String?,
    @Json(name = "end_city") val endCity: String?,
    @Json(name = "departure_date") val departureDate: String?,
    @Json(name = "departure_time") val departureTime: String?,
    val duration: String?,
    @Json(name = "price_per_seat") val pricePerSeat: Double?,
    @Json(name = "available_seats") val availableSeats: Int?,
    @Json(name = "total_seats") val totalSeats: Int?,
    @Json(name = "car_model") val carModel: String?,
    @Json(name = "car_color") val carColor: String?,
    @Json(name = "car_plate") val carPlate: String?,
    @Json(name = "allows_luggage") val allowsLuggage: Boolean?,
    @Json(name = "accept_cash") val acceptCash: Boolean?,
    @Json(name = "accept_wallet") val acceptWallet: Boolean?,
    @Json(name = "women_only") val isWomenOnly: Boolean?,
    val status: String?
)

@JsonClass(generateAdapter = true)
data class RequestedTripDto(
    val id: String,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "user_name") val userName: String?,
    @Json(name = "user_phone") val userPhone: String?,
    @Json(name = "start_city") val startCity: String,
    @Json(name = "end_city") val endCity: String,
    @Json(name = "departure_date") val departureDate: String,
    @Json(name = "departure_time") val departureTime: String,
    @Json(name = "men_count") val menCount: Int,
    @Json(name = "women_count") val womenCount: Int,
    @Json(name = "children_count") val childrenCount: Int,
    val status: String,
    @Json(name = "accepted_by_driver_id") val acceptedByDriverId: Long?,
    @Json(name = "accepted_by_driver_name") val acceptedByDriverName: String?,
    @Json(name = "created_at") val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class CreateRequestedTripRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "start_city") val startCity: String,
    @Json(name = "end_city") val endCity: String,
    @Json(name = "departure_date") val departureDate: String,
    @Json(name = "departure_time") val departureTime: String,
    @Json(name = "men_count") val menCount: Int,
    @Json(name = "women_count") val womenCount: Int,
    @Json(name = "children_count") val childrenCount: Int
)

@JsonClass(generateAdapter = true)
data class AcceptRequestedTripRequest(
    @Json(name = "driver_id") val driverId: Long,
    @Json(name = "driver_name") val driverName: String
)
