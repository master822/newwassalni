package com.example.data.network

import com.example.data.network.model.*
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.DELETE

interface ApiService {

    // ==========================================
    // 1. Authentication Endpoints
    // ==========================================
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("api/auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): Response<SendOtpResponse>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ApiResponse<Unit>>

    @POST("api/auth/forgot-password-email")
    suspend fun forgotPasswordEmail(@Body request: ForgotPasswordEmailRequest): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any?>>>

    @POST("api/auth/reset-password-email")
    suspend fun resetPasswordEmail(@Body request: ResetPasswordEmailRequest): Response<ApiResponse<Unit>>

    // ==========================================
    // 2. Users Endpoints
    // ==========================================
    @GET("api/users/profile")
    suspend fun getProfile(): Response<ApiResponse<UserDto>>

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<UserDto>>

    @POST("api/users/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<ApiResponse<Unit>>

    @DELETE("api/users/me")
    suspend fun deleteAccount(): Response<ApiResponse<Unit>>

    // ==========================================
    // 3. Rides Endpoints
    // ==========================================
    @GET("api/rides")
    suspend fun searchRides(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("date") date: String? = null,
        @Query("womenOnly") womenOnly: Boolean? = null,
        @Query("verifiedOnly") verifiedOnly: Boolean? = null
    ): Response<ApiResponse<List<RideDto>>>

    @GET("api/rides/{id}")
    suspend fun getRideById(@Path("id") rideId: String): Response<ApiResponse<RideDto>>

    @POST("api/rides")
    suspend fun publishRide(@Body request: PublishRideRequest): Response<ApiResponse<RideDto>>

    @POST("api/rides/{id}/book")
    suspend fun bookRide(
        @Path("id") rideId: String,
        @Body request: BookRideRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/rides/{id}/cancel")
    suspend fun cancelRide(
        @Path("id") rideId: String,
        @Body request: CancelRideRequest
    ): Response<ApiResponse<Unit>>

    // ==========================================
    // 4. Requested Trips Endpoints
    // ==========================================
    @GET("api/requests")
    suspend fun getRequestedTrips(@Query("status") status: String? = null): Response<ApiResponse<List<RequestedTripDto>>>

    @POST("api/requests")
    suspend fun publishRequestedTrip(@Body request: PublishRequestedTripRequest): Response<ApiResponse<RequestedTripDto>>

    @POST("api/requests/{id}/accept")
    suspend fun acceptRequestedTrip(
        @Path("id") tripId: String,
        @Body request: AcceptRequestedTripRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/requests/{id}/cancel-acceptance")
    suspend fun cancelTripAcceptance(@Path("id") tripId: String): Response<ApiResponse<Unit>>

    @DELETE("api/requests/{id}")
    suspend fun deleteRequestedTrip(@Path("id") tripId: String): Response<ApiResponse<Unit>>

    // ==========================================
    // 5. Wallet Endpoints
    // ==========================================
    @GET("api/wallet")
    suspend fun getWalletData(): Response<WalletResponse>

    @POST("api/wallet/topup")
    suspend fun submitTopUp(@Body request: TopUpRequestPayload): Response<ApiResponse<TopUpRequestDto>>

    // ==========================================
    // 6. Messages / Chat Endpoints
    // ==========================================
    @GET("api/messages/sync/all")
    suspend fun getAllChatMessages(): Response<ApiResponse<List<ChatMessageDto>>>

    @GET("api/messages/{rideId}")
    suspend fun getChatMessages(@Path("rideId") rideId: String): Response<ApiResponse<List<ChatMessageDto>>>

    @POST("api/messages/{rideId}")
    suspend fun sendChatMessage(
        @Path("rideId") rideId: String,
        @Body request: SendChatMessageRequest
    ): Response<ApiResponse<ChatMessageDto>>

    @DELETE("api/messages/{rideId}")
    suspend fun deleteChatConversation(
        @Path("rideId") rideId: String
    ): Response<ApiResponse<Unit>>

    @DELETE("api/messages/item/{messageId}")
    suspend fun deleteChatMessage(
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Unit>>

    @PUT("api/messages/read/{rideId}")
    suspend fun markChatMessagesAsRead(
        @Path("rideId") rideId: String
    ): Response<ApiResponse<Unit>>

    @PUT("api/messages/read/all")
    suspend fun markAllChatMessagesAsRead(): Response<ApiResponse<Unit>>

    // ==========================================
    // 7. Notifications Endpoints
    // ==========================================
    @GET("api/notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationDto>>>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") notificationId: String): Response<ApiResponse<Unit>>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Unit>>

    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") notificationId: String
    ): Response<ApiResponse<Unit>>

    @DELETE("api/notifications")
    suspend fun deleteAllNotifications(): Response<ApiResponse<Unit>>

    // ==========================================
    // 8. Public App Settings Endpoints
    // ==========================================
    @GET("api/settings")
    suspend fun getAppSettings(): Response<ApiResponse<AppSettingsDto>>

    // ==========================================
    // 9. Admin Endpoints
    // ==========================================
    @POST("api/admin/users")
    suspend fun createAdminUser(
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<UserDto>>

    @GET("api/admin/users")
    suspend fun getAdminUsers(
        @Query("search") search: String? = null,
        @Query("role") role: String? = null,
        @Query("suspended") suspended: Boolean? = null
    ): Response<ApiResponse<List<UserDto>>>

    @PUT("api/admin/users/{id}")
    suspend fun updateAdminUser(
        @Path("id") userId: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<UserDto>>

    @DELETE("api/admin/users/{id}")
    suspend fun deleteAdminUser(
        @Path("id") userId: String
    ): Response<ApiResponse<Unit>>

    @POST("api/admin/users/{id}/toggle-suspend")
    suspend fun toggleUserSuspension(
        @Path("id") userId: String,
        @Body request: ToggleSuspendRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/admin/users/{id}/adjust-wallet")
    suspend fun adjustUserWallet(
        @Path("id") userId: String,
        @Body request: AdjustWalletRequest
    ): Response<AdjustWalletResponse>

    @POST("api/admin/impersonate/{userId}")
    suspend fun startImpersonation(@Path("userId") userId: String): Response<ImpersonateResponse>

    @GET("api/admin/topup-requests")
    suspend fun getAdminTopUps(@Query("status") status: String? = null): Response<ApiResponse<List<TopUpRequestDto>>>

    @POST("api/admin/topup-requests/{id}/approve")
    suspend fun approveTopUp(@Path("id") requestId: String): Response<ApproveTopUpResponse>

    @POST("api/admin/topup-requests/{id}/reject")
    suspend fun rejectTopUp(
        @Path("id") requestId: String,
        @Body request: RejectTopUpRequest
    ): Response<ApiResponse<Unit>>

    @GET("api/admin/rides")
    suspend fun getAdminRides(): Response<ApiResponse<List<RideDto>>>

    @PUT("api/admin/rides/{id}")
    suspend fun updateAdminRide(
        @Path("id") rideId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<RideDto>>

    @DELETE("api/admin/rides/{id}")
    suspend fun deleteAdminRide(
        @Path("id") rideId: String,
        @Query("reason") reason: String? = null
    ): Response<ApiResponse<Unit>>

    @GET("api/admin/requested-trips")
    suspend fun getAdminRequestedTrips(): Response<ApiResponse<List<RequestedTripDto>>>

    @PUT("api/admin/requested-trips/{id}")
    suspend fun updateAdminRequestedTrip(
        @Path("id") tripId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<RequestedTripDto>>

    @POST("api/admin/requested-trips/{id}/reopen")
    suspend fun reopenAdminRequestedTrip(@Path("id") tripId: String): Response<ApiResponse<RequestedTripDto>>

    @DELETE("api/admin/requested-trips/{id}")
    suspend fun deleteAdminRequestedTrip(@Path("id") tripId: String): Response<ApiResponse<Unit>>

    @GET("api/admin/chats")
    suspend fun getAdminChatRooms(): Response<ApiResponse<List<Map<String, @JvmSuppressWildcards Any?>>>>

    @PUT("api/admin/chats/{rideId}/messages/{messageId}")
    suspend fun editAdminChatMessage(
        @Path("rideId") rideId: String,
        @Path("messageId") messageId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<ChatMessageDto>>

    @DELETE("api/admin/chats/{rideId}/messages/{messageId}")
    suspend fun deleteAdminChatMessage(
        @Path("rideId") rideId: String,
        @Path("messageId") messageId: String
    ): Response<ApiResponse<Unit>>

    @POST("api/admin/chats/{rideId}/clear")
    suspend fun clearAdminChatRoom(@Path("rideId") rideId: String): Response<ApiResponse<Unit>>

    @POST("api/admin/chats/{rideId}/admin-message")
    suspend fun sendAdminBroadcastChatMessage(
        @Path("rideId") rideId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<ChatMessageDto>>

    @POST("api/admin/broadcast")
    suspend fun broadcastNotification(@Body request: BroadcastRequest): Response<ApiResponse<Unit>>

    @PUT("api/admin/settings")
    suspend fun updateAdminSettings(@Body settings: Map<String, String>): Response<ApiResponse<Unit>>

    @GET("api/admin/audit")
    suspend fun getAdminAuditLogs(): Response<ApiResponse<Map<String, @JvmSuppressWildcards Any?>>>

    @GET("api/admin/support-tickets")
    suspend fun getSupportTickets(): Response<ApiResponse<List<SupportTicketDto>>>

    @POST("api/admin/support-tickets/{id}/reply")
    suspend fun replySupportTicket(
        @Path("id") ticketId: String,
        @Body request: ReplyTicketRequest
    ): Response<ApiResponse<Unit>>
}
