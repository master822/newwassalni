package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDao
import com.example.data.model.*
import com.example.data.network.ApiClient
import com.example.data.network.ApiService
import com.example.data.network.TokenManager
import com.example.data.network.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WassalniRepository(
    context: Context,
    private val dao: AppDao,
    private val api: ApiService = ApiClient.getService(context),
    private val tokenManager: TokenManager = TokenManager.getInstance(context)
) {

    val tokenMgr: TokenManager get() = tokenManager

    // ==========================================================
    // 1. AUTHENTICATION & PROFILE
    // ==========================================================

    suspend fun login(emailOrPhone: String, pass: String): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val res = api.login(LoginRequest(emailOrPhone.trim(), pass))
            if (res.isSuccessful && res.body()?.success == true) {
                val body = res.body()!!
                val user = body.user!!
                tokenManager.saveAuthTokens(
                    accessToken = body.accessToken ?: "",
                    refreshToken = body.refreshToken,
                    userId = user.id,
                    userName = user.name,
                    userEmail = user.email,
                    userPhone = user.phone,
                    userRole = user.role ?: "USER",
                    isImpersonating = false
                )
                // Cache user in Room
                dao.insertUser(
                    UserEntity(
                        id = user.id,
                        name = user.name,
                        email = user.email,
                        phone = user.phone,
                        avatarUrl = user.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                        rating = user.rating ?: 5.0f,
                        rideCount = user.rideCount ?: 0,
                        isVerified = user.isVerified ?: true,
                        walletPoints = user.walletPoints ?: 50,
                        isSuspended = user.isSuspended ?: false,
                        suspendReason = user.suspendReason,
                        userRole = user.userRole ?: "راكب وسائق",
                        referralCode = user.referralCode ?: "WASALNI-100"
                    )
                )
                Result.success(user)
            } else {
                val errorMsg = res.body()?.error ?: res.body()?.message ?: "فشل في تسجيل الدخول. تأكد من صحة البيانات."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        pass: String,
        referralCode: String?,
        verifyToken: String?
    ): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val res = api.register(
                RegisterRequest(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    password = pass,
                    referralCode = referralCode?.trim()?.ifBlank { null },
                    verifyToken = verifyToken?.trim()?.ifBlank { null }
                )
            )
            if (res.isSuccessful && res.body()?.success == true) {
                val body = res.body()!!
                val user = body.user!!
                tokenManager.saveAuthTokens(
                    accessToken = body.accessToken ?: "",
                    refreshToken = body.refreshToken,
                    userId = user.id,
                    userName = user.name,
                    userEmail = user.email,
                    userPhone = user.phone,
                    userRole = user.role ?: "USER",
                    isImpersonating = false
                )
                dao.insertUser(
                    UserEntity(
                        id = user.id,
                        name = user.name,
                        email = user.email,
                        phone = user.phone,
                        avatarUrl = user.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                        rating = user.rating ?: 5.0f,
                        rideCount = user.rideCount ?: 0,
                        isVerified = user.isVerified ?: true,
                        walletPoints = user.walletPoints ?: 50,
                        userRole = user.userRole ?: "راكب وسائق",
                        referralCode = user.referralCode ?: "WASALNI-100"
                    )
                )
                Result.success(user)
            } else {
                val errorMsg = res.body()?.error ?: res.body()?.message ?: "فشل في إنشاء الحساب"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendOtp(phone: String): Result<SendOtpResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.sendOtp(SendOtpRequest(phone.trim()))
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(res.body()!!)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في إرسال رمز التحقق"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(
        phone: String,
        otp: String,
        otpId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = api.verifyOtp(
                VerifyOtpRequest(
                    phone = phone.trim(),
                    otp = otp.trim(),
                    otpId = otpId?.trim()?.ifBlank { null }
                )
            )

            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(res.body()?.verifyToken ?: "")
            } else {
                val errorMsg = res.body()?.error ?: "رمز التحقق غير صحيح"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(phone: String, otp: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.resetPassword(ResetPasswordRequest(phone.trim(), otp.trim(), newPass))
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في إعادة تعيين كلمة المرور"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentUserProfile(): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val res = api.getProfile()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val user = res.body()!!.data!!
                dao.insertUser(
                    UserEntity(
                        id = user.id,
                        name = user.name,
                        email = user.email,
                        phone = user.phone,
                        avatarUrl = user.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                        rating = user.rating ?: 5.0f,
                        rideCount = user.rideCount ?: 0,
                        isVerified = user.isVerified ?: true,
                        walletPoints = user.walletPoints ?: 50,
                        isSuspended = user.isSuspended ?: false,
                        suspendReason = user.suspendReason,
                        userRole = user.userRole ?: "راكب وسائق",
                        referralCode = user.referralCode ?: "WASALNI-100"
                    )
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to fetch profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<UserDto> = fetchCurrentUserProfile()

    fun logout() {
        tokenManager.clear()
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteAccount()
            if (res.isSuccessful) {
                tokenManager.clear()
                Result.success(Unit)
            } else {
                Result.failure(Exception("فشل في حذف الحساب"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================
    // 2. RIDES
    // ==========================================================

    suspend fun syncRides(from: String? = null, to: String? = null, date: String? = null): Result<List<RideEntity>> = withContext(Dispatchers.IO) {
        try {
            val res = api.searchRides(from, to, date)
            if (res.isSuccessful && res.body()?.success == true) {
                val dtoList = res.body()?.data ?: emptyList()
                val entities = dtoList.map { dto ->
                    RideEntity(
                        id = dto.id,
                        driverId = dto.driverId,
                        driverName = dto.driverName,
                        driverAvatar = dto.driverAvatar ?: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
                        driverRating = dto.driverRating ?: 5.0f,
                        driverTripCount = dto.driverTripCount ?: 0,
                        driverVerified = dto.driverVerified ?: true,
                        startCity = dto.startCity,
                        endCity = dto.endCity,
                        departureDate = dto.departureDate,
                        departureTime = dto.departureTime,
                        duration = dto.duration ?: "2 سا",
                        pricePerSeat = dto.pricePerSeat,
                        availableSeats = dto.availableSeats,
                        totalSeats = dto.totalSeats,
                        carModel = dto.carModel ?: "تويوتا كامري",
                        carColor = dto.carColor ?: "فضي",
                        carPlate = dto.carPlate ?: "دمشق 123456",
                        allowsLuggage = dto.allowsLuggage ?: true,
                        acceptCash = dto.acceptCash ?: true,
                        acceptWallet = dto.acceptWallet ?: true,
                        isWomenOnly = dto.isWomenOnly ?: false,
                        status = dto.status ?: "UPCOMING"
                    )
                }
                dao.insertRides(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to fetch rides from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRides(from: String? = null, to: String? = null, date: String? = null): Result<List<RideEntity>> = syncRides(from, to, date)

    suspend fun publishRide(
        startCity: String,
        endCity: String,
        date: String,
        time: String,
        pricePerSeat: Double,
        availableSeats: Int,
        carModel: String = "تويوتا كامري 2022",
        carColor: String = "فضي (Silver)",
        carPlate: String = "دمشق 892103",
        isWomenOnly: Boolean = false,
        allowsLuggage: Boolean = true
    ): Result<RideEntity> = withContext(Dispatchers.IO) {
        try {
            val req = PublishRideRequest(
                startCity = startCity,
                endCity = endCity,
                departureDate = date,
                departureTime = time,
                duration = "2 سا 30 د",
                pricePerSeat = pricePerSeat,
                availableSeats = availableSeats,
                totalSeats = availableSeats,
                carModel = carModel,
                carColor = carColor,
                carPlate = carPlate,
                allowsLuggage = allowsLuggage,
                acceptCash = true,
                acceptWallet = true,
                isWomenOnly = isWomenOnly
            )
            val res = api.publishRide(req)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val dto = res.body()!!.data!!
                val entity = RideEntity(
                    id = dto.id,
                    driverId = dto.driverId,
                    driverName = dto.driverName,
                    driverAvatar = dto.driverAvatar ?: "",
                    driverRating = dto.driverRating ?: 5.0f,
                    driverTripCount = dto.driverTripCount ?: 0,
                    driverVerified = dto.driverVerified ?: true,
                    startCity = dto.startCity,
                    endCity = dto.endCity,
                    departureDate = dto.departureDate,
                    departureTime = dto.departureTime,
                    duration = dto.duration ?: "2 سا",
                    pricePerSeat = dto.pricePerSeat,
                    availableSeats = dto.availableSeats,
                    totalSeats = dto.totalSeats,
                    carModel = dto.carModel ?: "تويوتا كامري",
                    carColor = dto.carColor ?: "فضي",
                    carPlate = dto.carPlate ?: "دمشق 123456",
                    allowsLuggage = dto.allowsLuggage ?: true,
                    acceptCash = dto.acceptCash ?: true,
                    acceptWallet = dto.acceptWallet ?: true,
                    isWomenOnly = dto.isWomenOnly ?: false,
                    status = dto.status ?: "UPCOMING"
                )
                dao.insertRide(entity)
                // Also deduct 50 points locally
                dao.deductWalletPoints(tokenManager.getUserId() ?: "", 50)
                Result.success(entity)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في نشر الرحلة"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bookRide(rideId: String, seats: Int = 1, useWallet: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.bookRide(rideId, BookRideRequest(seats, useWallet))
            if (res.isSuccessful && res.body()?.success == true) {
                syncRides()
                fetchCurrentUserProfile()
                Result.success(Unit)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في حجز الرحلة"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelRide(rideId: String, reason: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.cancelRide(rideId, CancelRideRequest(reason))
            if (res.isSuccessful && res.body()?.success == true) {
                dao.deleteRide(rideId)
                syncRides()
                Result.success(Unit)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في إلغاء الرحلة"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================
    // 3. REQUESTED TRIPS
    // ==========================================================

    suspend fun syncRequestedTrips(): Result<List<RequestedTripEntity>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getRequestedTrips()
            if (res.isSuccessful && res.body()?.success == true) {
                val dtoList = res.body()?.data ?: emptyList()
                val entities = dtoList.map { dto ->
                    RequestedTripEntity(
                        id = dto.id,
                        userId = dto.userId,
                        userName = dto.userName,
                        userPhone = dto.userPhone,
                        userAvatar = dto.userAvatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                        startCity = dto.startCity,
                        endCity = dto.endCity,
                        departureDate = dto.departureDate,
                        departureTime = dto.departureTime,
                        menCount = dto.menCount,
                        womenCount = dto.womenCount,
                        childrenCount = dto.childrenCount,
                        status = dto.status,
                        acceptedByDriverId = dto.acceptedByDriverId,
                        acceptedByDriverName = dto.acceptedByDriverName
                    )
                }
                dao.insertRequestedTrips(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to fetch requested trips"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRequestedTrips(): Result<List<RequestedTripEntity>> = syncRequestedTrips()

    suspend fun publishRequestedTrip(
        startCity: String,
        endCity: String,
        departureDate: String,
        departureTime: String,
        menCount: Int,
        womenCount: Int,
        childrenCount: Int
    ): Result<RequestedTripEntity> = withContext(Dispatchers.IO) {
        try {
            val req = PublishRequestedTripRequest(
                startCity = startCity,
                endCity = endCity,
                departureDate = departureDate,
                departureTime = departureTime,
                menCount = menCount,
                womenCount = womenCount,
                childrenCount = childrenCount
            )
            val res = api.publishRequestedTrip(req)
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val dto = res.body()!!.data!!
                val entity = RequestedTripEntity(
                    id = dto.id,
                    userId = dto.userId,
                    userName = dto.userName,
                    userPhone = dto.userPhone,
                    userAvatar = dto.userAvatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                    startCity = dto.startCity,
                    endCity = dto.endCity,
                    departureDate = dto.departureDate,
                    departureTime = dto.departureTime,
                    menCount = dto.menCount,
                    womenCount = dto.womenCount,
                    childrenCount = dto.childrenCount,
                    status = dto.status
                )
                dao.insertRequestedTrip(entity)
                Result.success(entity)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في نشر طلب الرحلة"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptRequestedTrip(
        tripId: String,
        carModel: String = "تويوتا كامري 2022",
        carColor: String = "فضي (Silver)",
        carPlate: String = "دمشق 892103"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.acceptRequestedTrip(tripId, AcceptRequestedTripRequest(carModel, carColor, carPlate))
            if (res.isSuccessful && res.body()?.success == true) {
                syncRequestedTrips()
                syncRides()
                Result.success(Unit)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في قبول طلب الرحلة"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelAcceptedRequestedTrip(tripId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.cancelTripAcceptance(tripId)
            if (res.isSuccessful && res.body()?.success == true) {
                syncRequestedTrips()
                syncRides()
                Result.success(Unit)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في إلغاء قبول الرحلة"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRequestedTrip(tripId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteRequestedTrip(tripId)
            if (res.isSuccessful) {
                dao.deleteRequestedTrip(tripId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("فشل في حذف طلب الرحلة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================
    // 4. WALLET & TOPUP
    // ==========================================================

    suspend fun syncWalletData(): Result<WalletResponse> = withContext(Dispatchers.IO) {
        try {
            val res = api.getWalletData()
            if (res.isSuccessful && res.body()?.success == true) {
                val data = res.body()!!
                val entities = data.transactions.map { dto ->
                    WalletTransactionEntity(
                        id = dto.id,
                        userId = dto.userId,
                        type = dto.type,
                        points = dto.points,
                        amountUsd = dto.amountUsd ?: 0.0,
                        description = dto.description,
                        status = dto.status
                    )
                }
                dao.insertWalletTransactions(entities)
                Result.success(data)
            } else {
                Result.failure(Exception("Failed to fetch wallet data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchWalletTransactions(): Result<WalletResponse> = syncWalletData()

    suspend fun submitTopUpRequest(packagePoints: Int, packagePriceUsd: Double, receiptImagePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.submitTopUp(TopUpRequestPayload(packagePoints, packagePriceUsd, receiptImagePath))
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = res.body()?.error ?: "فشل في إرسال طلب الشحن"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================
    // 5. CHAT / MESSAGES
    // ==========================================================

    suspend fun syncChatMessages(rideId: String): Result<List<ChatMessageEntity>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getChatMessages(rideId)
            if (res.isSuccessful && res.body()?.success == true) {
                val dtoList = res.body()?.data ?: emptyList()
                val entities = dtoList.map { dto ->
                    ChatMessageEntity(
                        id = dto.id,
                        rideId = dto.rideId,
                        senderId = dto.senderId,
                        receiverId = "",
                        messageText = dto.message,
                        imageUri = dto.imageUri,
                        isLocation = dto.isLocation,
                        latitude = dto.latitude,
                        longitude = dto.longitude
                    )
                }
                dao.insertChatMessages(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to fetch chat messages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        rideId: String,
        text: String,
        imageUri: String? = null,
        isLocation: Boolean = false,
        receiverId: String = ""
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        try {
            val res = api.sendChatMessage(
                rideId,
                SendChatMessageRequest(text, imageUri, isLocation, null, null)
            )
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                val dto = res.body()!!.data!!
                val entity = ChatMessageEntity(
                    id = dto.id,
                    rideId = dto.rideId,
                    senderId = dto.senderId,
                    receiverId = receiverId,
                    messageText = dto.message,
                    imageUri = dto.imageUri,
                    isLocation = dto.isLocation,
                    latitude = dto.latitude,
                    longitude = dto.longitude
                )
                dao.insertChatMessage(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception("فشل في إرسال الرسالة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================
    // 6. NOTIFICATIONS
    // ==========================================================

    suspend fun syncNotifications(): Result<List<NotificationEntity>> = withContext(Dispatchers.IO) {
        try {
            val res = api.getNotifications()
            if (res.isSuccessful && res.body()?.success == true) {
                val dtoList = res.body()?.data ?: emptyList()
                val entities = dtoList.map { dto ->
                    NotificationEntity(
                        id = dto.id,
                        userId = dto.userId,
                        title = dto.title,
                        message = dto.message,
                        type = dto.type,
                        isRead = dto.isRead
                    )
                }
                dao.insertNotifications(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to fetch notifications"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchNotifications(): Result<List<NotificationEntity>> = syncNotifications()

    // ==========================================================
    // 7. APP SETTINGS & CONFIG
    // ==========================================================

    suspend fun fetchRemoteSettings(): Result<AppSettingsDto> = withContext(Dispatchers.IO) {
        try {
            val res = api.getAppSettings()
            if (res.isSuccessful && res.body()?.success == true && res.body()?.data != null) {
                Result.success(res.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to fetch settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRemoteSettings(
        appName: String? = null,
        appTagline: String? = null,
        appLogoUrl: String? = null,
        dynamicIconVariant: String? = null,
        isMaintenanceMode: Boolean? = null,
        shamCashAccount: String? = null,
        appDownloadUrl: String? = null,
        ridePublishCost: Int? = null,
        appCommissionPercent: Double? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val map = mutableMapOf<String, String>()
            appName?.let { map["appName"] = it }
            appTagline?.let { map["appTagline"] = it }
            appLogoUrl?.let { map["appLogoUrl"] = it }
            dynamicIconVariant?.let { map["dynamicIconVariant"] = it }
            isMaintenanceMode?.let { map["isMaintenanceMode"] = it.toString() }
            shamCashAccount?.let { map["shamCashAccount"] = it }
            appDownloadUrl?.let { map["appDownloadUrl"] = it }
            ridePublishCost?.let { map["ridePublishCost"] = it.toString() }
            appCommissionPercent?.let { map["appCommissionPercent"] = it.toString() }

            val res = api.updateAdminSettings(map)
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================================
    // 8. ADMIN OPERATIONS
    // ==========================================================

    suspend fun adminApproveTopUp(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.approveTopUp(requestId)
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.body()?.error ?: "Failed to approve topup"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminRejectTopUp(requestId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.rejectTopUp(requestId, RejectTopUpRequest(reason))
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.body()?.error ?: "Failed to reject topup"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminToggleSuspend(userId: String, isSuspended: Boolean, reason: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.toggleUserSuspension(userId, ToggleSuspendRequest(reason))
            if (res.isSuccessful && res.body()?.success == true) {
                dao.updateUserSuspension(userId, isSuspended, reason)
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.body()?.error ?: "Failed to update user suspension"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminAdjustWallet(userId: String, points: Int, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.adjustUserWallet(userId, AdjustWalletRequest(points, reason))
            if (res.isSuccessful && res.body()?.success == true) {
                if (points >= 0) {
                    dao.addWalletPoints(userId, points)
                } else {
                    dao.deductWalletPoints(userId, -points)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.body()?.error ?: "Failed to adjust wallet"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminBroadcast(title: String, message: String, targetAudience: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.broadcastNotification(BroadcastRequest(title, message, targetAudience))
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.body()?.error ?: "Failed to broadcast"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
