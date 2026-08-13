package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()

    val activeUserId = MutableStateFlow("user_default")
    val currentUserId: String get() = activeUserId.value

    val isImpersonating = MutableStateFlow(false)
    val impersonatedUser = MutableStateFlow<UserEntity?>(null)

    // Remote Dynamic App Configuration
    val appName = MutableStateFlow("وصلني")
    val appTagline = MutableStateFlow("نسافر معاً، نوصل بأمان")
    val appLogoUrl = MutableStateFlow("https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=300")
    val dynamicIconVariant = MutableStateFlow("Emerald Green (افتراضي)")
    val isMaintenanceMode = MutableStateFlow(false)

    fun startImpersonation(user: UserEntity) {
        impersonatedUser.value = user
        activeUserId.value = user.id
        isImpersonating.value = true
        _currentScreen.value = "search"
        addAdminActivityLog("تقمص هوية مستخدم", "تم الدخول بحساب المستخدم: ${user.name} (${user.id})")
    }

    fun stopImpersonation() {
        val lastImpersonated = impersonatedUser.value?.name ?: ""
        impersonatedUser.value = null
        activeUserId.value = "user_default"
        isImpersonating.value = false
        _currentScreen.value = "admin"
        addAdminActivityLog("إنهاء تقمص الهوية", "تم الخروج من حساب $lastImpersonated والعودة للوحة الأدمن")
    }

    fun updateRemoteAppConfig(name: String, tagline: String, logoUrl: String, iconVariant: String, maintenance: Boolean) {
        appName.value = name
        appTagline.value = tagline
        appLogoUrl.value = logoUrl
        dynamicIconVariant.value = iconVariant
        isMaintenanceMode.value = maintenance
        addAdminActivityLog("تحديث الإعدادات العامة والتصميم", "تحديث اسم التطبيق: $name | الأيقونة: $iconVariant | وضع الصيانة: $maintenance")
    }

    // Settings State
    private val _appLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Navigation & Screen State
    private val _currentScreen = MutableStateFlow("search") // search, search_results, ride_details, my_rides, publish, messages, wallet, admin
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _selectedRide = MutableStateFlow<RideEntity?>(null)
    val selectedRide: StateFlow<RideEntity?> = _selectedRide.asStateFlow()

    // Dialogs State
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showNotificationsDialog = MutableStateFlow(false)
    val showNotificationsDialog: StateFlow<Boolean> = _showNotificationsDialog.asStateFlow()

    private val _showInsufficientBalanceAlert = MutableStateFlow(false)
    val showInsufficientBalanceAlert: StateFlow<Boolean> = _showInsufficientBalanceAlert.asStateFlow()

    private val _showTopUpModal = MutableStateFlow(false)
    val showTopUpModal: StateFlow<Boolean> = _showTopUpModal.asStateFlow()

    // Search Fields
    val searchFromCity = MutableStateFlow("دمشق")
    val searchToCity = MutableStateFlow("حلب")
    val searchDate = MutableStateFlow("2026-08-08")
    val searchPassengers = MutableStateFlow(1)

    // Filters
    val filterVerifiedOnly = MutableStateFlow(false)
    val filterWomenOnly = MutableStateFlow(false)
    val filterHighRating = MutableStateFlow(false)

    // Admin Authentication & Session State
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _adminPassword = MutableStateFlow("sniper927MUHAMMAD")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    private val _lastAdminActivityTime = MutableStateFlow(System.currentTimeMillis())
    val lastAdminActivityTime: StateFlow<Long> = _lastAdminActivityTime.asStateFlow()

    // User Session & Mandatory Auth State
    val isLoggedIn = MutableStateFlow(true) // Default true for active user session, can be toggled via AuthDialog

    // Admin Settings State
    val ridePublishCost = MutableStateFlow(50)
    val appCommissionPercent = MutableStateFlow(5.0)
    val featureChatEnabled = MutableStateFlow(true)
    val featureRatingsEnabled = MutableStateFlow(true)
    val featureWomenOnlyEnabled = MutableStateFlow(true)
    val cancellationRefundPercent = MutableStateFlow(100)
    val shamCashAccount = MutableStateFlow("ba64858e96d4ad9c6096948bc2dbc970")
    val appDownloadUrl = MutableStateFlow("https://wasalni.app/download")

    fun updateShamCashAccount(acc: String) {
        if (acc.isNotBlank()) {
            shamCashAccount.value = acc
            addAdminActivityLog("تعديل حساب شام كاش الموحد", "تم تحديث رقم حساب شام كاش الموحد إلى: $acc")
        }
    }

    fun updateAppDownloadUrl(url: String) {
        if (url.isNotBlank()) {
            appDownloadUrl.value = url
            addAdminActivityLog("تعديل رابط تنزيل التطبيق", "تم تحديث رابط التنزيل الموحد إلى: $url")
        }
    }

    // Admin Activity & Security Logs
    private val _adminActivityLogs = MutableStateFlow<List<AdminActivityLog>>(
        listOf(
            AdminActivityLog("log_1", "تسجيل دخول أدمن", "تم تسجيل الدخول بنجاح عبر النظام الوحيد Super Admin", "2026-08-07 08:30 ص"),
            AdminActivityLog("log_2", "موافقة شحن نقاط", "تأكيد إضافة 500 نقطة حساب سامر الحمصي", "2026-08-07 09:15 ص"),
            AdminActivityLog("log_3", "تعديل إعدادات", "تعديل كلفة نشر الرحلة إلى 50 نقطة", "2026-08-06 04:20 م")
        )
    )
    val adminActivityLogs: StateFlow<List<AdminActivityLog>> = _adminActivityLogs.asStateFlow()

    private val _adminLoginLogs = MutableStateFlow<List<AdminLoginLog>>(
        listOf(
            AdminLoginLog("l_1", "2026-08-07 08:30:12 ص", "192.168.1.15", "Android Wasalni App / Android 14"),
            AdminLoginLog("l_2", "2026-08-06 10:14:05 ص", "192.168.1.15", "Android Wasalni App / Android 14")
        )
    )
    val adminLoginLogs: StateFlow<List<AdminLoginLog>> = _adminLoginLogs.asStateFlow()

    // Support Tickets State
    private val _supportTickets = MutableStateFlow<List<SupportTicket>>(
        listOf(
            SupportTicket("t_101", "سامر الحمصي", "samer@wasalni.app", "مشكلة في خصم النقاط", "تم خصم 50 نقطة مرتين أثناء محاولة نشر رحلة دمشق - حمص", "HIGH", "OPEN", null, "2026-08-07"),
            SupportTicket("t_102", "مريم الحلبي", "mariam@wasalni.app", "استفسار عن الشحن عبر شام كاش", "هل يمكن تحويل المبلغ من حساب آخر في شام كاش؟", "MEDIUM", "RESOLVED", "نعم يمكنك استخدام أي حساب شام كاش موحد وإرفاق الإشعار.", "2026-08-06")
        )
    )
    val supportTickets: StateFlow<List<SupportTicket>> = _supportTickets.asStateFlow()

    // Banners State
    private val _homeBanners = MutableStateFlow<List<HomeBannerItem>>(
        listOf(
            HomeBannerItem("b_1", "خصم 20% على الرحلات بين دمشق وحلب", "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957", "search"),
            HomeBannerItem("b_2", "رحلات نسائية آمنة وسريعة مع وسلني", "https://images.unsplash.com/photo-1508921912186-1d1a45ebb3c1", "search")
        )
    )
    val homeBanners: StateFlow<List<HomeBannerItem>> = _homeBanners.asStateFlow()

    // DB Flows
    val currentUser: StateFlow<UserEntity?> = activeUserId.flatMapLatest { id ->
        dao.getUserFlow(id)
    }.catch { emit(null) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserEntity>> = dao.getAllUsers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRides: StateFlow<List<RideEntity>> = dao.getAllRides()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletTransactions: StateFlow<List<WalletTransactionEntity>> = activeUserId.flatMapLatest { id ->
        dao.getWalletTransactions(id)
    }.catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWalletTransactions: StateFlow<List<WalletTransactionEntity>> = dao.getAllWalletTransactions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChatMessages: StateFlow<List<ChatMessageEntity>> = dao.getAllChatMessages()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topUpRequests: StateFlow<List<TopUpRequestEntity>> = dao.getAllTopUpRequests()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBookings: StateFlow<List<RideBookingEntity>> = activeUserId.flatMapLatest { id ->
        dao.getBookingsByPassenger(id)
    }.catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = activeUserId.flatMapLatest { id ->
        dao.getNotifications(id)
    }.catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val requestedTrips: StateFlow<List<RequestedTripEntity>> = dao.getAllRequestedTrips()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeChatMessages: Flow<List<ChatMessageEntity>> = _selectedRide.flatMapLatest { ride ->
        if (ride != null) dao.getChatMessages(ride.id) else flowOf(emptyList())
    }

    init {
        // Initialize Default User & Sample Data if empty
        viewModelScope.launch {
            try {
                if (dao.getUser(currentUserId) == null) {
                    dao.insertUser(
                        UserEntity(
                            id = currentUserId,
                            name = "أحمد المحمد",
                            phone = "+963 988 123 456",
                            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                            rating = 4.9f,
                            rideCount = 38,
                            isVerified = true,
                            walletPoints = 50
                        )
                    )
                }
                seedSampleRidesIfEmpty()
                seedSampleRequestedTripsIfEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun seedSampleRidesIfEmpty() {
        try {
            if (dao.getAllRides().first().isEmpty()) {
            val sampleRides = listOf(
                RideEntity(
                    id = "ride_1",
                    driverId = "driver_101",
                    driverName = "سامر الحمصي",
                    driverAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=300",
                    driverRating = 4.9f,
                    driverTripCount = 52,
                    driverVerified = true,
                    startCity = "دمشق",
                    endCity = "حلب",
                    departureDate = "2026-08-08",
                    departureTime = "08:00 AM",
                    duration = "3 سا 30 د",
                    pricePerSeat = 8.50,
                    availableSeats = 3,
                    totalSeats = 4,
                    carModel = "Hyundai Elantra 2022",
                    carColor = "فضي (Silver)",
                    carPlate = "دمشق 849201",
                    allowsLuggage = true,
                    acceptCash = true,
                    acceptWallet = true,
                    isWomenOnly = false
                ),
                RideEntity(
                    id = "ride_2",
                    driverId = "driver_102",
                    driverName = "مريم الحلبي",
                    driverAvatar = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=300",
                    driverRating = 4.8f,
                    driverTripCount = 29,
                    driverVerified = true,
                    startCity = "دمشق",
                    endCity = "حلب",
                    departureDate = "2026-08-08",
                    departureTime = "10:30 AM",
                    duration = "3 سا 45 د",
                    pricePerSeat = 7.00,
                    availableSeats = 2,
                    totalSeats = 3,
                    carModel = "Kia Rio 2021",
                    carColor = "أبيض (White)",
                    carPlate = "حلب 392014",
                    allowsLuggage = true,
                    acceptCash = true,
                    acceptWallet = true,
                    isWomenOnly = true // Women only filter sample
                ),
                RideEntity(
                    id = "ride_3",
                    driverId = "driver_103",
                    driverName = "خالد العلي",
                    driverAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300",
                    driverRating = 4.7f,
                    driverTripCount = 19,
                    driverVerified = true,
                    startCity = "دمشق",
                    endCity = "حمص",
                    departureDate = "2026-08-08",
                    departureTime = "01:00 PM",
                    duration = "1 سا 45 د",
                    pricePerSeat = 4.50,
                    availableSeats = 4,
                    totalSeats = 4,
                    carModel = "Toyota Corolla 2020",
                    carColor = "أسود (Black)",
                    carPlate = "حمص 918204",
                    allowsLuggage = true,
                    acceptCash = true,
                    acceptWallet = true,
                    isWomenOnly = false
                ),
                RideEntity(
                    id = "ride_4",
                    driverId = "driver_104",
                    driverName = "عمر الشامي",
                    driverAvatar = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&q=80&w=300",
                    driverRating = 4.95f,
                    driverTripCount = 84,
                    driverVerified = true,
                    startCity = "دمشق",
                    endCity = "بيروت",
                    departureDate = "2026-08-08",
                    departureTime = "07:00 AM",
                    duration = "2 سا 15 د",
                    pricePerSeat = 15.00,
                    availableSeats = 3,
                    totalSeats = 4,
                    carModel = "Skoda Octavia 2023",
                    carColor = "كحلي (Navy Blue)",
                    carPlate = "دمشق 102948",
                    allowsLuggage = true,
                    acceptCash = true,
                    acceptWallet = true,
                    isWomenOnly = false
                )
            )

            sampleRides.forEach { dao.insertRide(it) }
        }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Actions
    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun selectRide(ride: RideEntity?) {
        _selectedRide.value = ride
    }

    fun swapSearchCities() {
        val temp = searchFromCity.value
        searchFromCity.value = searchToCity.value
        searchToCity.value = temp
    }

    fun updateUserProfile(name: String, avatarUrl: String, phone: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updatedUser = user.copy(
                name = name,
                avatarUrl = avatarUrl,
                phone = phone
            )
            dao.insertUser(updatedUser)

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                title = "تم تحديث بيانات الملف الشخصي",
                message = "تم تحديث الصورة الشخصية ورقم الهاتف الجديد ($phone) بنجاح.",
                type = NotificationType.SYSTEM.name
            )
            dao.insertNotification(notif)
        }
    }

    fun registerUserWithPhone(name: String, email: String, phone: String) {
        viewModelScope.launch {
            val newUser = UserEntity(
                id = currentUserId,
                name = name,
                email = email,
                phone = phone,
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                rating = 5.0f,
                rideCount = 0,
                isVerified = true,
                walletPoints = 100
            )
            dao.insertUser(newUser)
        }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun toggleNotificationsDialog(show: Boolean) {
        _showNotificationsDialog.value = show
        if (show) {
            viewModelScope.launch {
                dao.markAllNotificationsAsRead(currentUserId)
            }
        }
    }

    fun toggleInsufficientBalanceAlert(show: Boolean) {
        _showInsufficientBalanceAlert.value = show
    }

    fun toggleTopUpModal(show: Boolean) {
        _showTopUpModal.value = show
    }

    // Publish Ride Logic
    fun publishRide(
        startCity: String,
        endCity: String,
        date: String,
        time: String,
        availableSeats: Int,
        pricePerSeat: Double,
        carModel: String = "تويوتا كامري 2022",
        carColor: String = "فضي (Silver)",
        carPlate: String = "دمشق 892103",
        isWomenOnly: Boolean = false,
        allowsLuggage: Boolean = true
    ): Boolean {
        val user = currentUser.value ?: return false
        if (user.walletPoints < 50) {
            _showInsufficientBalanceAlert.value = true
            return false
        }

        viewModelScope.launch {
            // Deduct 50 points
            dao.deductWalletPoints(currentUserId, 50)

            // Add wallet transaction record
            val tx = WalletTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                type = TransactionType.DEDUCTION.name,
                points = 50,
                amountUsd = 0.0,
                description = "خصم 50 نقطة - نشر رحلة جديدة ($startCity ➔ $endCity)",
                status = "COMPLETED"
            )
            dao.insertWalletTransaction(tx)

            // Add Notification
            val notification = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                title = "تم نشر الرحلة وخصم النقاط",
                message = "تم نشر رحلتك من $startCity إلى $endCity بنجاح وخصم 50 نقطة من محفظتك.",
                type = NotificationType.DEDUCTION.name
            )
            dao.insertNotification(notification)

            // Insert Ride to DB with car details
            val newRide = RideEntity(
                id = UUID.randomUUID().toString(),
                driverId = currentUserId,
                driverName = user.name,
                driverAvatar = user.avatarUrl,
                driverRating = user.rating,
                driverTripCount = user.rideCount + 1,
                driverVerified = user.isVerified,
                startCity = startCity,
                endCity = endCity,
                departureDate = date,
                departureTime = time,
                duration = "3 سا 00 د",
                pricePerSeat = pricePerSeat,
                availableSeats = availableSeats,
                totalSeats = availableSeats,
                carModel = carModel.ifBlank { "تويوتا كامري 2022" },
                carColor = carColor.ifBlank { "فضي" },
                carPlate = carPlate.ifBlank { "دمشق 123456" },
                allowsLuggage = allowsLuggage,
                acceptCash = true,
                acceptWallet = true,
                isWomenOnly = isWomenOnly,
                status = RideStatus.UPCOMING.name
            )
            dao.insertRide(newRide)

            // Navigate to My Rides
            _currentScreen.value = "my_rides"
        }
        return true
    }

    // Book Ride Logic
    fun bookRide(ride: RideEntity, seats: Int = 1) {
        viewModelScope.launch {
            if (ride.availableSeats >= seats) {
                dao.decrementAvailableSeats(ride.id, seats)

                val booking = RideBookingEntity(
                    id = UUID.randomUUID().toString(),
                    rideId = ride.id,
                    passengerId = currentUserId,
                    passengerName = currentUser.value?.name ?: "راكب",
                    seatsBooked = seats,
                    status = RideStatus.UPCOMING.name
                )
                dao.insertBooking(booking)

                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    title = "تم تأكيد حجز الرحلة",
                    message = "حجزك لرحلة ${ride.startCity} ➔ ${ride.endCity} مؤكد مع السائق ${ride.driverName}.",
                    type = NotificationType.BOOKING.name
                )
                dao.insertNotification(notif)
            }
        }
    }

    // Cancel Ride Logic
    fun cancelRide(rideId: String) {
        viewModelScope.launch {
            if (rideId.startsWith("ride_from_req_")) {
                val reqId = rideId.removePrefix("ride_from_req_")
                // Reopen the requested trip so other drivers can accept it
                dao.updateRequestedTripStatus(reqId, "OPEN", null, null)
                dao.deleteRide(rideId)

                val req = dao.getAllRequestedTrips().first().find { it.id == reqId }
                if (req != null) {
                    val notifPassenger = NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        userId = req.userId,
                        title = "🔄 إعادة إتاحة طلب رحلتك",
                        message = "اعتذر السائق عن الرحلة، وتمت إعادة فتح طلبك (${req.startCity} ➔ ${req.endCity}) ليقبله سائق آخر.",
                        type = NotificationType.SYSTEM.name
                    )
                    dao.insertNotification(notifPassenger)
                }

                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    title = "تم إلغاء الرحلة وإعادة إتاحتها",
                    message = "تم إلغاء قبولك للطلب وإعادة فتحه ليكون متاحاً للسائقين الآخرين.",
                    type = NotificationType.SYSTEM.name
                )
                dao.insertNotification(notif)
                addAdminActivityLog("إلغاء قبول طلب رحلة", "قام السائق بإلغاء قبول طلب الرحلة $reqId وإعادته إلى الحالة المفتوحة (OPEN)")
            } else {
                dao.updateRideStatus(rideId, RideStatus.CANCELLED.name)
                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    title = "تم إلغاء الرحلة",
                    message = "تم إلغاء رحلتك بنجاح.",
                    type = NotificationType.SYSTEM.name
                )
                dao.insertNotification(notif)
            }
        }
    }

    // Cham Cash Top-Up Submit Logic
    fun submitTopUpRequest(packagePoints: Int, packagePriceUsd: Double, receiptImagePath: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val req = TopUpRequestEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                userName = user.name,
                packagePoints = packagePoints,
                packagePriceUsd = packagePriceUsd,
                receiptImagePath = receiptImagePath,
                status = RequestStatus.PENDING.name
            )
            dao.insertTopUpRequest(req)

            // User Notification: Request under review
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                title = "طلب الشحن قيد المراجعة",
                message = "تم استلام طلب شراء $packagePoints نقطة وهو الآن قيد المراجعة من الأدمن.",
                type = NotificationType.SYSTEM.name
            )
            dao.insertNotification(notif)
        }
    }

    // Admin Action: Approve TopUp Request
    fun approveTopUpRequest(requestId: String) {
        viewModelScope.launch {
            val req = dao.getTopUpRequestById(requestId) ?: return@launch
            dao.updateTopUpRequestStatus(requestId, RequestStatus.APPROVED.name, null)

            // Add Points to user
            dao.addWalletPoints(req.userId, req.packagePoints)

            // Record transaction
            val tx = WalletTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = req.userId,
                type = TransactionType.TOP_UP.name,
                points = req.packagePoints,
                amountUsd = req.packagePriceUsd,
                description = "شحن محفظة عبر شام كاش (تأكيد الأدمن)",
                status = "COMPLETED"
            )
            dao.insertWalletTransaction(tx)

            // User notification
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = req.userId,
                title = "تمت الموافقة على طلب الشحن!",
                message = "تمت الموافقة على طلبك وإضافة ${req.packagePoints} نقطة بنجاح إلى محفظتك.",
                type = NotificationType.APPROVAL.name
            )
            dao.insertNotification(notif)
            addAdminActivityLog("موافقة شحن نقاط", "الموافقة على شحن ${req.packagePoints} نقطة للمستخدم ${req.userName}")
        }
    }

    // Admin Action: Reject TopUp Request
    fun rejectTopUpRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            val req = dao.getTopUpRequestById(requestId) ?: return@launch
            dao.updateTopUpRequestStatus(requestId, RequestStatus.REJECTED.name, reason)

            // User notification with reason
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = req.userId,
                title = "تم رفض طلب الشحن",
                message = "تم رفض طلب شراء النقاط الخاص بك. السبب: $reason. يرجى التواصل مع الدعم.",
                type = NotificationType.SYSTEM.name
            )
            dao.insertNotification(notif)
            addAdminActivityLog("رفض شحن نقاط", "رفض طلب ${req.userName} بسبب: $reason")
        }
    }

    // Admin Auth Actions
    fun loginAdmin(emailInput: String, passwordInput: String): Boolean {
        val emailClean = emailInput.trim().lowercase()
        val isValidEmail = emailClean == "mastersniper823@gmail.com" || emailClean == "mastersniper823@gmil.com"
        val isValidPass = passwordInput == _adminPassword.value || passwordInput == "sniper927MUHAMMAD"

        if (isValidEmail && isValidPass) {
            _isAdminLoggedIn.value = true
            isLoggedIn.value = true
            _lastAdminActivityTime.value = System.currentTimeMillis()
            addAdminActivityLog("تسجيل دخول أدمن", "دخول ناجح للوحة التحكم بالأدمن الرئيسية")

            // Add Login Log
            val newLoginLog = AdminLoginLog(
                id = UUID.randomUUID().toString(),
                timestamp = "2026-08-07 " + java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date()),
                ipAddress = "192.168.1.42",
                deviceBrowser = "Android App / Native UI"
            )
            _adminLoginLogs.update { listOf(newLoginLog) + it }
            return true
        }
        return false
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
    }

    fun checkAdminSessionTimeout(): Boolean {
        // 1 hour timeout (3600,000 ms)
        val currentTime = System.currentTimeMillis()
        if (_isAdminLoggedIn.value && (currentTime - _lastAdminActivityTime.value) > 3600000) {
            logoutAdmin()
            return true // Timed out
        }
        if (_isAdminLoggedIn.value) {
            _lastAdminActivityTime.value = currentTime
        }
        return false
    }

    fun changeAdminPassword(oldPass: String, newPass: String): Boolean {
        if (oldPass == _adminPassword.value && newPass.length >= 6) {
            _adminPassword.value = newPass
            addAdminActivityLog("تغيير كلمة المرور", "تم تحديث كلمة مرور الأدمن الرئيسي بنجاح")
            return true
        }
        return false
    }

    // Admin Activity Log Helper
    fun addAdminActivityLog(action: String, details: String) {
        val newLog = AdminActivityLog(
            id = UUID.randomUUID().toString(),
            actionName = action,
            details = details,
            timestamp = "2026-08-07 " + java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        )
        _adminActivityLogs.update { listOf(newLog) + it }
    }

    // Admin User Management
    fun suspendUser(userId: String, reason: String) {
        viewModelScope.launch {
            dao.updateUserSuspension(userId, true, reason)
            addAdminActivityLog("تعليق حساب مستخدم", "تم تعليق المستخدم $userId بسبب: $reason")
        }
    }

    fun reactivateUser(userId: String) {
        viewModelScope.launch {
            dao.updateUserSuspension(userId, false, null)
            addAdminActivityLog("إعادة تفعيل حساب", "تم فك التجميد وتفعيل حساب المستخدم $userId")
        }
    }

    fun deleteUserByAdmin(userId: String) {
        viewModelScope.launch {
            dao.deleteUser(userId)
            addAdminActivityLog("حذف مستخدم", "تم حذف المستخدم $userId بشكل نهائي من النظام")
        }
    }

    // Admin Ride Management
    fun cancelRideByAdmin(rideId: String, reason: String) {
        viewModelScope.launch {
            dao.updateRideStatus(rideId, RideStatus.CANCELLED.name)
            val ride = dao.getRideById(rideId)
            if (ride != null) {
                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = ride.driverId,
                    title = "إلغاء رحلة من قبل الإدارة",
                    message = "تم إلغاء رحلتك المتجهة إلى ${ride.endCity} من قبل الأدمن. السبب: $reason",
                    type = NotificationType.SYSTEM.name
                )
                dao.insertNotification(notif)
            }
            addAdminActivityLog("إلغاء رحلة", "تم إلغاء الرحلة $rideId بواسطة الأدمن بسبب: $reason")
        }
    }

    // Admin Push Notification
    fun sendGlobalNotification(title: String, body: String, target: String) {
        viewModelScope.launch {
            val all = dao.getAllUsers().first()
            all.forEach { u ->
                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = u.id,
                    title = title,
                    message = body,
                    type = NotificationType.SYSTEM.name
                )
                dao.insertNotification(notif)
            }
            addAdminActivityLog("إرسال إشعار عام", "عنوان: $title | المستهدفين: $target")
        }
    }

    // Admin Banner Management
    fun addHomeBanner(title: String, imageUrl: String, route: String) {
        val banner = HomeBannerItem(UUID.randomUUID().toString(), title, imageUrl, route)
        _homeBanners.value = _homeBanners.value + banner
        addAdminActivityLog("إضافة إعلان بنر", "العنوان: $title")
    }

    fun toggleBannerStatus(bannerId: String) {
        _homeBanners.value = _homeBanners.value.map {
            if (it.id == bannerId) it.copy(isActive = !it.isActive) else it
        }
    }

    // Admin Support Ticket Reply
    fun replySupportTicket(ticketId: String, replyText: String) {
        _supportTickets.value = _supportTickets.value.map {
            if (it.id == ticketId) {
                it.copy(status = "RESOLVED", adminReply = replyText)
            } else it
        }
        addAdminActivityLog("الرد على تذكرة دعم", "التذكرة $ticketId - تمت المعالجة والإغلاق")
    }

    // Chat Actions
    fun sendChatMessage(
        rideId: String,
        text: String,
        imageUri: String? = null,
        isLocation: Boolean = false,
        senderId: String = currentUserId,
        receiverId: String = "driver_id"
    ) {
        viewModelScope.launch {
            val msg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                rideId = rideId,
                senderId = senderId,
                receiverId = receiverId,
                messageText = text,
                imageUri = imageUri,
                isLocation = isLocation,
                latitude = if (isLocation) 33.5138 else null,
                longitude = if (isLocation) 36.2765 else null
            )
            dao.insertChatMessage(msg)
        }
    }

    // Trigger Payment Reminder in Chat
    fun sendPaymentReminder(rideId: String) {
        viewModelScope.launch {
            val msg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                rideId = rideId,
                senderId = "system",
                receiverId = currentUserId,
                messageText = "تذكير: بعد انتهاء الرحلة يرجى تحويل/دفع المبلغ للسائق.",
                isPaymentReminder = true
            )
            dao.insertChatMessage(msg)
        }
    }

    private suspend fun seedSampleRequestedTripsIfEmpty() {
        try {
            if (dao.getAllRequestedTrips().first().isEmpty()) {
                val sampleRequests = listOf(
                    RequestedTripEntity(
                        id = "req_101",
                        userId = "user_201",
                        userName = "علاء السيد",
                        userPhone = "+963 933 445 566",
                        startCity = "دمشق",
                        endCity = "طرطوس",
                        departureDate = "2026-08-09",
                        departureTime = "09:00 AM",
                        menCount = 2,
                        womenCount = 1,
                        childrenCount = 0,
                        status = "OPEN"
                    ),
                    RequestedTripEntity(
                        id = "req_102",
                        userId = "user_202",
                        userName = "سارة محمود",
                        userPhone = "+963 944 112 233",
                        startCity = "حمص",
                        endCity = "اللاذقية",
                        departureDate = "2026-08-09",
                        departureTime = "02:30 PM",
                        menCount = 1,
                        womenCount = 2,
                        childrenCount = 1,
                        status = "OPEN"
                    )
                )
                sampleRequests.forEach { dao.insertRequestedTrip(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Requested Trips Logic
    fun publishRequestedTrip(
        startCity: String,
        endCity: String,
        date: String,
        time: String,
        men: Int,
        women: Int,
        children: Int
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            val req = RequestedTripEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                userName = user?.name ?: "راكب",
                userPhone = user?.phone ?: "+963 988 123 456",
                userAvatar = user?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                startCity = startCity,
                endCity = endCity,
                departureDate = date,
                departureTime = time,
                menCount = men,
                womenCount = women,
                childrenCount = children,
                status = "OPEN"
            )
            dao.insertRequestedTrip(req)
            addAdminActivityLog("تثبيت طلب رحلة", "قام ${req.userName} بنشر طلب رحلة من $startCity إلى $endCity")
        }
    }

    fun acceptRequestedTrip(requestId: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val driverName = user?.name ?: "السائق الموثق"
            val driverAvatar = user?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300"
            val reqList = dao.getAllRequestedTrips().first()
            val req = reqList.find { it.id == requestId }

            dao.updateRequestedTripStatus(requestId, "ACCEPTED", currentUserId, driverName)

            if (req != null) {
                // Add to "رحلاتي كسائق"
                val newDriverRide = RideEntity(
                    id = "ride_from_req_$requestId",
                    driverId = currentUserId,
                    driverName = driverName,
                    driverAvatar = driverAvatar,
                    driverRating = user?.rating ?: 5.0f,
                    driverTripCount = (user?.rideCount ?: 0) + 1,
                    driverVerified = true,
                    startCity = req.startCity,
                    endCity = req.endCity,
                    departureDate = req.departureDate,
                    departureTime = req.departureTime,
                    duration = "2 ساعة 30 دقيقة",
                    pricePerSeat = 5.0,
                    availableSeats = req.menCount + req.womenCount + req.childrenCount,
                    totalSeats = 4,
                    carModel = "تويوتا كامري",
                    carColor = "فضي",
                    carPlate = "دمشق 883211",
                    status = "UPCOMING"
                )
                dao.insertRide(newDriverRide)

                val notifPassenger = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = req.userId,
                    title = "🚗 تم قبول طلب رحلتك",
                    message = "قام السائق $driverName بقبول طلب رحلتك من ${req.startCity} إلى ${req.endCity}.",
                    type = NotificationType.APPROVAL.name
                )
                dao.insertNotification(notifPassenger)
            }

            addAdminActivityLog("قبول طلب رحلة", "تم قبول طلب الرحلة $requestId بواسطة $driverName وإضافتها لرحلاته كسائق")
        }
    }

    fun cancelAcceptedRequestedTrip(requestId: String) {
        viewModelScope.launch {
            // Revert requested trip to OPEN so another driver can accept it
            dao.updateRequestedTripStatus(requestId, "OPEN", null, null)
            dao.deleteRide("ride_from_req_$requestId")

            val req = dao.getAllRequestedTrips().first().find { it.id == requestId }
            if (req != null) {
                val notifPassenger = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = req.userId,
                    title = "🔄 إعادة إتاحة طلب رحلتك",
                    message = "اعتذر السائق عن الرحلة، وتمت إعادة فتح طلبك (${req.startCity} ➔ ${req.endCity}) ليتمكن سائق آخر من قبوله.",
                    type = NotificationType.SYSTEM.name
                )
                dao.insertNotification(notifPassenger)
            }

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                title = "تم إلغاء قبول الطلب",
                message = "تم إلغاء قبولك للطلب وإعادة فتحه للسائقين الآخرين.",
                type = NotificationType.SYSTEM.name
            )
            dao.insertNotification(notif)
            addAdminActivityLog("إلغاء قبول طلب رحلة", "تم إلغاء قبول طلب الرحلة $requestId وإعادة إتاحته للسائقين")
        }
    }

    fun deleteRequestedTrip(requestId: String) {
        viewModelScope.launch {
            dao.deleteRequestedTrip(requestId)
            addAdminActivityLog("حذف طلب رحلة", "تم إلغاء وحذف طلب الرحلة $requestId")
        }
    }

    // Registration & Referral System
    suspend fun registerUserAccount(
        name: String,
        email: String,
        phone: String,
        pass: String,
        referralCodeInput: String?
    ): Pair<Boolean, String> {
        val users = dao.getAllUsers().first()

        // Check single usage for email or phone (Requirement 10)
        val existingUser = users.find { it.email.equals(email, ignoreCase = true) || it.phone == phone }
        if (existingUser != null) {
            return Pair(false, "البريد الإلكتروني أو رقم الهاتف مستخدم سابقاً للحساب! يمكنك التسجيل لمرة واحدة فقط.")
        }

        val newUserId = "user_${UUID.randomUUID().toString().take(6)}"
        val myReferralCode = "WASALNI-${UUID.randomUUID().toString().take(5).uppercase()}"

        // New user bonus: 50 points (Requirement 9)
        val newUser = UserEntity(
            id = newUserId,
            name = name,
            email = email,
            phone = phone,
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=300",
            walletPoints = 50,
            referralCode = myReferralCode
        )
        dao.insertUser(newUser)

        // Add 50 bonus transaction
        dao.insertWalletTransaction(
            WalletTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = newUserId,
                type = "TOP_UP",
                points = 50,
                amountUsd = 0.0,
                description = "هدية الترحيب للتسجيل بـ 50 نقطة مجانية"
            )
        )

        // Referral reward: Check if referral code matches an existing user
        if (!referralCodeInput.isNullOrBlank()) {
            val referrer = users.find { it.referralCode.equals(referralCodeInput.trim(), ignoreCase = true) }
            if (referrer != null) {
                val updatedPoints = referrer.walletPoints + 50
                dao.insertUser(referrer.copy(walletPoints = updatedPoints))
                dao.insertWalletTransaction(
                    WalletTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = referrer.id,
                        type = "TOP_UP",
                        points = 50,
                        amountUsd = 0.0,
                        description = "مكافأة دعوة صديق ($name) عبر كود الإحالة (+50 نقطة)"
                    )
                )
                // Send notification to referrer
                dao.insertNotification(
                    NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        userId = referrer.id,
                        title = "🎁 تم كسب 50 نقطة!",
                        message = "قام صديقك $name بالتسجيل باستخدام رمز الإحالة الخاص بك! تم إضافة 50 نقطة إلى محفظتك بنجاح.",
                        type = "REFERRAL"
                    )
                )
            }
        }

        isLoggedIn.value = true
        return Pair(true, "تم إنشاء الحساب بنجاح وتم منحك 50 نقطة ترحيبية في محفظتك!")
    }

    fun loginUser(email: String, name: String) {
        isLoggedIn.value = true
        viewModelScope.launch {
            val existing = dao.getAllUsers().first().find { it.email.equals(email, ignoreCase = true) }
            if (existing != null) {
                dao.insertUser(existing)
            }
        }
    }

    fun logoutUser() {
        isLoggedIn.value = false
        _isAdminLoggedIn.value = false
        _currentScreen.value = "search"
    }

    // Comprehensive Super Admin Chat Controls
    fun editChatMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            dao.updateChatMessageText(messageId, newText)
            addAdminActivityLog("تعديل رسالة محادثة", "تم تعديل نص الرسالة $messageId")
        }
    }

    fun deleteChatMessage(messageId: String) {
        viewModelScope.launch {
            dao.deleteChatMessage(messageId)
            addAdminActivityLog("حذف رسالة محادثة", "تم حذف الرسالة $messageId بواسطة الأدمن")
        }
    }

    fun deleteChatRoom(rideId: String) {
        viewModelScope.launch {
            dao.deleteChatMessagesForRide(rideId)
            addAdminActivityLog("تفريغ محادثة رحلة", "تم حذف جميع رسائل المحادثة للرحلة $rideId")
        }
    }

    fun sendAdminChatMessage(
        rideId: String,
        senderId: String,
        senderName: String,
        messageText: String,
        imageUri: String? = null,
        isSystem: Boolean = false
    ) {
        viewModelScope.launch {
            val msg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                rideId = rideId,
                senderId = senderId,
                receiverId = "all",
                messageText = messageText,
                imageUri = imageUri,
                isLocation = false,
                isPaymentReminder = false
            )
            dao.insertChatMessage(msg)
            addAdminActivityLog("إرسال رسالة كأدمن", "إرسال رسالة للرحلة $rideId باسم: $senderName")
        }
    }

    // Comprehensive Broadcast Notification
    fun sendBroadcastNotification(
        title: String,
        message: String,
        targetAudience: String,
        type: String = NotificationType.SYSTEM.name
    ) {
        viewModelScope.launch {
            val users = dao.getAllUsers().first()
            val filteredUsers = when (targetAudience) {
                "DRIVERS" -> users.filter { it.userRole == "DRIVER" || it.rideCount > 0 }
                "PASSENGERS" -> users.filter { it.userRole == "PASSENGER" }
                else -> users
            }

            val notifs = filteredUsers.map { user ->
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    title = title,
                    message = message,
                    type = type
                )
            }
            dao.insertNotifications(notifs)
            addAdminActivityLog("إرسال إشعار جماعي (Broadcast)", "العنوان: $title | الفئة: $targetAudience | العدد: ${filteredUsers.size}")
        }
    }

    // Admin User Profile Management
    fun adminUpdateUserData(userId: String, name: String, phone: String, role: String, points: Int) {
        viewModelScope.launch {
            dao.updateUserData(userId, name, phone, role, points)
            addAdminActivityLog("تعديل بيانات مستخدم", "تم تعديل بيانات المستخدم $name ($userId) - الرصيد: $points نقطة")
        }
    }

    fun adminResetUserPassword(userId: String) {
        viewModelScope.launch {
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "🔒 إعادة تعيين كلمة المرور",
                message = "تمت إعادة تعيين كلمة المرور الخاصة بك إلى كلمة المرور الافتراضية (123456) من قبل مدير النظام.",
                type = NotificationType.SYSTEM.name
            )
            dao.insertNotification(notif)
            addAdminActivityLog("إعادة تعيين كلمة مرور", "تم إعادة تعيين كلمة المرور للمستخدم $userId")
        }
    }

    // Admin Wallet Adjustments
    fun adminAdjustUserWallet(userId: String, pointsDelta: Int, reason: String) {
        viewModelScope.launch {
            if (pointsDelta > 0) {
                dao.addWalletPoints(userId, pointsDelta)
                dao.insertWalletTransaction(
                    WalletTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        type = TransactionType.TOP_UP.name,
                        points = pointsDelta,
                        amountUsd = 0.0,
                        description = "إيداع نقاط من الإدارة: $reason"
                    )
                )
            } else if (pointsDelta < 0) {
                val absPoints = kotlin.math.abs(pointsDelta)
                dao.deductWalletPoints(userId, absPoints)
                dao.insertWalletTransaction(
                    WalletTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        type = TransactionType.DEDUCTION.name,
                        points = absPoints,
                        amountUsd = 0.0,
                        description = "خصم نقاط من الإدارة: $reason"
                    )
                )
            }
            addAdminActivityLog("تعديل رصيد مستخدم", "تعديل رصيد $userId بمقدار $pointsDelta نقطة. السبب: $reason")
        }
    }

    fun adminCancelWalletTransaction(txId: String) {
        viewModelScope.launch {
            dao.deleteWalletTransaction(txId)
            addAdminActivityLog("إلغاء حركة مالية", "تم إلغاء وحذف المعاملة المالية $txId")
        }
    }

    // Admin Ride Editing & Control
    fun adminEditRide(rideId: String, start: String, end: String, date: String, time: String, price: Double, seats: Int) {
        viewModelScope.launch {
            dao.updateRideDetails(rideId, start, end, date, time, price, seats)
            addAdminActivityLog("تعديل تفاصيل رحلة", "تم تعديل الرحلة $rideId ($start ➔ $end)")
        }
    }

    fun adminDeleteRide(rideId: String) {
        viewModelScope.launch {
            dao.deleteRide(rideId)
            addAdminActivityLog("حذف رحلة نهائياً", "تم حذف الرحلة $rideId من قاعدة البيانات")
        }
    }

    // Admin Requested Trips Editing & Control
    fun adminEditRequestedTrip(requestId: String, start: String, end: String, date: String, time: String, men: Int, women: Int, children: Int) {
        viewModelScope.launch {
            dao.updateRequestedTripDetails(requestId, start, end, date, time, men, women, children)
            addAdminActivityLog("تعديل طلب رحلة", "تم تعديل الطلب $requestId ($start ➔ $end)")
        }
    }

    fun adminReopenRequestedTrip(requestId: String) {
        viewModelScope.launch {
            dao.updateRequestedTripStatus(requestId, "OPEN", null, null)
            dao.deleteRide("ride_from_req_$requestId")
            addAdminActivityLog("إعادة فتح طلب رحلة", "تمت إعادة فتح الطلب $requestId ليكون متاحاً لجميع السائقين")
        }
    }

    fun adminAssignRequestedTrip(requestId: String, driverId: String, driverName: String) {
        viewModelScope.launch {
            val req = dao.getAllRequestedTrips().first().find { it.id == requestId }
            if (req != null) {
                dao.updateRequestedTripStatus(requestId, "ACCEPTED", driverId, driverName)
                val newDriverRide = RideEntity(
                    id = "ride_from_req_$requestId",
                    driverId = driverId,
                    driverName = driverName,
                    driverAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                    driverRating = 5.0f,
                    driverTripCount = 20,
                    driverVerified = true,
                    startCity = req.startCity,
                    endCity = req.endCity,
                    departureDate = req.departureDate,
                    departureTime = req.departureTime,
                    duration = "2 ساعة 30 دقيقة",
                    pricePerSeat = 5.0,
                    availableSeats = req.menCount + req.womenCount + req.childrenCount,
                    totalSeats = 4,
                    carModel = "تويوتا كامري",
                    carColor = "فضي",
                    carPlate = "دمشق 883211",
                    status = "UPCOMING"
                )
                dao.insertRide(newDriverRide)
                addAdminActivityLog("تعيين سائق لطلب رحلة", "تم تعيين السائق $driverName للطلب $requestId إدارياً")
            }
        }
    }

    // Telegram Bot Integration Backend Dispatcher
    fun processTelegramCommand(command: String, params: Map<String, String>): String {
        return when (command) {
            "/start" -> "أهلاً بك في بوت وسلني (Wasalni Bot)! يمكنك البحث عن الرحلات ونشر رحلة وإدارة محفظتك عبر Telegram."
            "/rides" -> {
                val rides = allRides.value
                if (rides.isEmpty()) "لا توجد رحلات متاحة حالياً."
                else "الرحلات المتاحة:\n" + rides.joinToString("\n") {
                    "🚘 ${it.startCity} ➔ ${it.endCity} | السائق: ${it.driverName} | السيارة: ${it.carModel} (${it.carColor}) | اللوحة: ${it.carPlate} | السعر: \$${it.pricePerSeat} | المقاعد: ${it.availableSeats}"
                }
            }
            "/points" -> {
                val user = currentUser.value
                "رصيد النقاط الخاص بك: ${user?.walletPoints ?: 0} نقطة."
            }
            else -> "أمر غير معروف. الأوامر المتاحة: /start, /rides, /points"
        }
    }
}
