package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.WassalniRepository
import com.example.util.AppNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()
    private val repository = WassalniRepository(application, dao)

    val activeUserId = MutableStateFlow(repository.tokenMgr.getUserId() ?: "user_default")
    val currentUserId: String get() = activeUserId.value

    val isImpersonating = MutableStateFlow(false)
    val impersonatedUser = MutableStateFlow<UserEntity?>(null)

    // Remote Dynamic App Configuration
    val appName = MutableStateFlow("وصلني")
    val appTagline = MutableStateFlow("نسافر معاً، نوصل بأمان")
    val appLogoUrl = MutableStateFlow("https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=300")
    val dynamicIconVariant = MutableStateFlow("Emerald Green (افتراضي)")
    val isMaintenanceMode = MutableStateFlow(false)

    // Settings State
    private val _appLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Navigation & Screen State
    private val _currentScreen = MutableStateFlow("search")
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

    // Admin Authentication & Session State (Dynamic, no hardcoded password)
    private val _isAdminLoggedIn = MutableStateFlow(repository.tokenMgr.isAdmin())
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _lastAdminActivityTime = MutableStateFlow(System.currentTimeMillis())
    val lastAdminActivityTime: StateFlow<Long> = _lastAdminActivityTime.asStateFlow()

    // User Session & Mandatory Auth State
    val isLoggedIn = MutableStateFlow(repository.tokenMgr.isLoggedIn())

    // Admin Settings State
    val ridePublishCost = MutableStateFlow(50)
    val appCommissionPercent = MutableStateFlow(5.0)
    val featureChatEnabled = MutableStateFlow(true)
    val featureRatingsEnabled = MutableStateFlow(true)
    val featureWomenOnlyEnabled = MutableStateFlow(true)
    val cancellationRefundPercent = MutableStateFlow(100)
    val shamCashAccount = MutableStateFlow("ba64858e96d4ad9c6096948bc2dbc970")
    val appDownloadUrl = MutableStateFlow("https://wasalni.app/download")

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

    private val _deletedRideIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedRideIds: StateFlow<Set<String>> = _deletedRideIds.asStateFlow()

    private val _deletedRequestedTripIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedRequestedTripIds: StateFlow<Set<String>> = _deletedRequestedTripIds.asStateFlow()

    val allRides: StateFlow<List<RideEntity>> = combine(dao.getAllRides(), _deletedRideIds) { rides, deleted ->
        rides.filter { it.id !in deleted }
    }.catch { emit(emptyList()) }
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

    val requestedTrips: StateFlow<List<RequestedTripEntity>> = combine(dao.getAllRequestedTrips(), _deletedRequestedTripIds) { trips, deleted ->
        trips.filter { it.id !in deleted }
    }.catch { emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeChatMessages: Flow<List<ChatMessageEntity>> = _selectedRide.flatMapLatest { ride ->
        if (ride != null) dao.getChatMessages(ride.id) else flowOf(emptyList())
    }

    private val _deletedChatRideIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedChatRideIds: StateFlow<Set<String>> = _deletedChatRideIds.asStateFlow()

    val unreadMessagesCount: StateFlow<Int> = combine(allChatMessages, activeUserId, _deletedChatRideIds, _selectedRide) { messages, uid, deletedRides, selRide ->
        val currentUid = uid.ifBlank { currentUserId }
        val activeRideId = selRide?.id
        messages.count { msg ->
            msg.rideId !in deletedRides &&
            msg.rideId != activeRideId &&
            !msg.isRead &&
            msg.senderId != currentUid &&
            (msg.receiverId == currentUid || msg.receiverId.isBlank() || msg.receiverId == "passenger_id" || msg.receiverId == "driver_id" || (_isAdminLoggedIn.value && msg.receiverId == "admin"))
        }
    }.catch { emit(0) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            try {
                syncInitialData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Realtime Background Synchronization for Chat Messages & Notifications
        viewModelScope.launch {
            var syncTick = 0
            while (isActive) {
                try {
                    val activeRide = _selectedRide.value
                    if (activeRide != null) {
                        repository.syncChatMessages(activeRide.id)
                        dao.markAllRideChatMessagesAsRead(activeRide.id)
                    }
                    repository.syncAllChatMessages()
                    if (repository.tokenMgr.isLoggedIn()) {
                        repository.fetchNotifications()
                    }
                    syncTick++
                    if (syncTick % 10 == 0) {
                        repository.syncPublicUsers()
                    }
                } catch (e: Exception) {
                    // Gracefully log without terminating loop
                    android.util.Log.w("WassalniChat", "Chat sync loop: ${e.message}")
                }
                delay(1500)
            }
        }
    }


    fun refreshAllData() {
        viewModelScope.launch {
            try {
                syncInitialData()

                if (repository.tokenMgr.isLoggedIn()) {
                    repository.getProfile()
                    repository.fetchWalletTransactions()
                    repository.fetchNotifications()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun syncInitialData() {
        // Sync remote settings
        repository.fetchRemoteSettings().onSuccess { settings ->
            appName.value = settings.appName ?: "وصلني"
            appTagline.value = settings.appTagline ?: "نسافر معاً، نوصل بأمان"
            shamCashAccount.value = settings.shamCashAccount ?: "ba64858e96d4ad9c6096948bc2dbc970"
            appDownloadUrl.value = settings.appDownloadUrl ?: "https://wasalni.app/download"
            ridePublishCost.value = settings.ridePublishCost ?: 50
            appCommissionPercent.value = settings.appCommissionPercent ?: 5.0
            isMaintenanceMode.value = settings.isMaintenanceMode ?: false
        }

        // Sync rides and requested trips
        repository.fetchRides()
        repository.fetchRequestedTrips()
        repository.fetchAdminUsers()
        repository.syncPublicUsers()
        seedSampleUsersIfEmpty()

        if (repository.tokenMgr.isLoggedIn()) {
            val uid = repository.tokenMgr.getUserId() ?: currentUserId
            activeUserId.value = uid
            isLoggedIn.value = true
            val role = repository.tokenMgr.getUserRole()
            _isAdminLoggedIn.value = (role == "ADMIN" || role == "SUPER_ADMIN")

            repository.getProfile()
            repository.fetchWalletTransactions()
            repository.fetchNotifications()
        } else {
            // Seed local default if completely empty
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
        }
        deduplicateMessagesIfAny()
    }

    private suspend fun deduplicateMessagesIfAny() {
        try {
            val all = dao.getAllChatMessages().first()
            val seen = mutableSetOf<String>()
            for (msg in all) {
                val timeBucket = msg.timestamp / 4000L
                val key = "${msg.rideId}_${msg.senderId}_${msg.messageText}_${msg.audioDurationSeconds}_$timeBucket"
                if (!seen.add(key)) {
                    dao.deleteChatMessage(msg.id)
                }
            }
        } catch (ignored: Exception) {}
    }

    private suspend fun seedSampleUsersIfEmpty() {
        try {
            val users = dao.getAllUsers().first()
            if (users.size <= 1) {
                val samples = listOf(
                    UserEntity(
                        id = currentUserId,
                        name = "أحمد المحمد",
                        email = "ahmad@wasalni.app",
                        phone = "+963 988 123 456",
                        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                        rating = 4.9f,
                        rideCount = 38,
                        isVerified = true,
                        walletPoints = 150,
                        userRole = "DRIVER"
                    ),
                    UserEntity(
                        id = "user_driver_101",
                        name = "سامر الحمصي",
                        email = "samer@wasalni.app",
                        phone = "+963 944 555 111",
                        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=300",
                        rating = 4.8f,
                        rideCount = 64,
                        isVerified = true,
                        walletPoints = 280,
                        userRole = "DRIVER"
                    ),
                    UserEntity(
                        id = "user_driver_102",
                        name = "مريم الحلبي",
                        email = "maryam@wasalni.app",
                        phone = "+963 933 777 222",
                        avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=300",
                        rating = 5.0f,
                        rideCount = 42,
                        isVerified = true,
                        walletPoints = 320,
                        userRole = "DRIVER"
                    ),
                    UserEntity(
                        id = "user_pass_201",
                        name = "خالد العلي",
                        email = "khaled@wasalni.app",
                        phone = "+963 955 888 333",
                        avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=300",
                        rating = 4.7f,
                        rideCount = 19,
                        isVerified = true,
                        walletPoints = 85,
                        userRole = "PASSENGER"
                    ),
                    UserEntity(
                        id = "user_pass_202",
                        name = "سارة الشامي",
                        email = "sara@wasalni.app",
                        phone = "+963 966 999 444",
                        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=300",
                        rating = 4.9f,
                        rideCount = 27,
                        isVerified = true,
                        walletPoints = 110,
                        userRole = "PASSENGER"
                    ),
                    UserEntity(
                        id = "user_pass_203",
                        name = "عمر اليوسف",
                        email = "omar@wasalni.app",
                        phone = "+963 977 111 555",
                        avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=300",
                        rating = 4.6f,
                        rideCount = 12,
                        isVerified = false,
                        walletPoints = 40,
                        userRole = "PASSENGER"
                    )
                )
                dao.insertUsers(samples)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                        isWomenOnly = true
                    )
                ).filter { it.id !in _deletedRideIds.value }
                sampleRides.forEach { dao.insertRide(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                    )
                )
                sampleRequests.forEach { dao.insertRequestedTrip(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // Real Authentication & Session Actions
    // ==========================================

    suspend fun loginUserAccount(emailOrPhone: String, pass: String): Pair<Boolean, String> {
        val result = repository.login(emailOrPhone, pass)
        return if (result.isSuccess) {
            val user = result.getOrNull()
            activeUserId.value = user?.id ?: "user_default"
            isLoggedIn.value = true
            val role = user?.role ?: repository.tokenMgr.getUserRole()
            _isAdminLoggedIn.value = (role == "ADMIN" || role == "SUPER_ADMIN")
            _lastAdminActivityTime.value = System.currentTimeMillis()
            syncInitialData()
            Pair(true, "تم تسجيل الدخول بنجاح")
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "فشل تسجيل الدخول")
        }
    }

    suspend fun registerUserAccount(
        name: String,
        email: String,
        phone: String,
        pass: String,
        referralCode: String?,
        verifyToken: String? = null
    ): Pair<Boolean, String> {
        val result = repository.register(
            name,
            email,
            phone,
            pass,
            referralCode,
            verifyToken
        )
        return if (result.isSuccess) {
            val user = result.getOrNull()
            activeUserId.value = user?.id ?: "user_default"
            isLoggedIn.value = true
            val role = user?.role ?: repository.tokenMgr.getUserRole()
            _isAdminLoggedIn.value = (role == "ADMIN" || role == "SUPER_ADMIN")
            syncInitialData()
            Pair(true, "تم إنشاء الحساب بنجاح وتم منحك 50 نقطة ترحيبية في محفظتك!")
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "فشل إنشاء الحساب")
        }
    }

    fun logoutUser() {
        repository.logout()
        isLoggedIn.value = false
        _isAdminLoggedIn.value = false
        activeUserId.value = "user_default"
        _currentScreen.value = "search"
    }

    suspend fun sendPhoneOtp(phone: String): Pair<Boolean, String> {
        val result = repository.sendOtp(phone)
        return if (result.isSuccess) {
            Pair(true, result.getOrNull()?.message ?: "تم إرسال رمز التحقق المؤلف من 6 أرقام عبر SMS بنجاح")
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "فشل في إرسال رمز التحقق عبر SMS")
        }
    }

    suspend fun verifyPhoneOtp(phone: String, otp: String): Pair<Boolean, String> {
        val result = repository.verifyOtp(phone, otp)
        return if (result.isSuccess) {
            val verifyToken = result.getOrNull()?.verifyToken
            if (!verifyToken.isNullOrBlank()) {
                Pair(true, verifyToken)
            } else {
                Pair(false, "تم التحقق من الرمز لكن لم يتم استلام رمز التحقق من الخادم")
            }
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "رمز التحقق غير صحيح أو منتهي الصلاحية")
        }
    }

    suspend fun resetPasswordWithPhone(phone: String, otp: String, newPass: String): Pair<Boolean, String> {
        val result = repository.resetPassword(phone, otp, newPass)
        return if (result.isSuccess) {
            Pair(true, "تمت إعادة تعيين كلمة المرور بنجاح! يمكنك الآن تسجيل الدخول.")
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "فشل في إعادة تعيين كلمة المرور")
        }
    }

    suspend fun sendForgotPasswordEmail(email: String): Pair<Boolean, String> {
        val result = repository.sendForgotPasswordEmail(email)
        return if (result.isSuccess) {
            Pair(true, result.getOrNull() ?: "تم إرسال رمز التحقق بنجاح")
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "فشل في إرسال رمز التحقق")
        }
    }

    suspend fun resetPasswordWithEmail(email: String, otp: String, newPass: String): Pair<Boolean, String> {
        val result = repository.resetPasswordWithEmail(email, otp, newPass)
        return if (result.isSuccess) {
            Pair(true, result.getOrNull() ?: "تمت إعادة تعيين كلمة المرور بنجاح")
        } else {
            Pair(false, result.exceptionOrNull()?.message ?: "فشل في إعادة تعيين كلمة المرور")
        }
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
    }

    fun checkAdminSessionTimeout(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (_isAdminLoggedIn.value && (currentTime - _lastAdminActivityTime.value) > 3600000) {
            logoutAdmin()
            return true
        }
        if (_isAdminLoggedIn.value) {
            _lastAdminActivityTime.value = currentTime
        }
        return false
    }

    // ==========================================
    // UI Navigation & State
    // ==========================================

    private val screenBackStack = mutableListOf<String>()

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setScreen(screen: String, addToBackStack: Boolean = true) {
        if (_currentScreen.value != screen) {
            if (addToBackStack) {
                screenBackStack.add(_currentScreen.value)
            }
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        // 1. Close open dialogs first
        if (_showNotificationsDialog.value) {
            _showNotificationsDialog.value = false
            return true
        }
        if (_showSettingsDialog.value) {
            _showSettingsDialog.value = false
            return true
        }
        if (_showTopUpModal.value) {
            _showTopUpModal.value = false
            return true
        }
        if (_showInsufficientBalanceAlert.value) {
            _showInsufficientBalanceAlert.value = false
            return true
        }

        // 2. If in chat messages and a specific conversation is open, go back to conversations list
        if (_currentScreen.value == "messages" && _selectedRide.value != null) {
            _selectedRide.value = null
            return true
        }

        // 3. Pop previous screen from backstack
        while (screenBackStack.isNotEmpty()) {
            val previousScreen = screenBackStack.removeAt(screenBackStack.size - 1)
            if (previousScreen != _currentScreen.value) {
                _currentScreen.value = previousScreen
                if (previousScreen != "messages" && previousScreen != "ride_details") {
                    _selectedRide.value = null
                }
                return true
            }
        }

        // 4. Default fallback transitions if backstack was empty:
        return when (_currentScreen.value) {
            "ride_details" -> {
                _selectedRide.value = null
                _currentScreen.value = "search_results"
                true
            }
            "search_results",
            "messages",
            "requested_trips",
            "publish",
            "bookings",
            "my_trips",
            "wallet",
            "profile",
            "admin" -> {
                _selectedRide.value = null
                _currentScreen.value = "search"
                true
            }
            else -> false // At root search screen
        }
    }

    fun selectRide(ride: RideEntity?) {
        _selectedRide.value = ride
        if (ride != null) {
            markRideMessagesAsRead(ride.id)
            viewModelScope.launch {
                repository.syncChatMessages(ride.id)
            }
        }
    }

    fun markChatMessagesAsRead(rideId: String) {
        markRideMessagesAsRead(rideId)
    }

    fun openRideChat(rideId: String) {
        viewModelScope.launch {
            val ride = dao.getRideById(rideId) ?: allRides.value.find { it.id == rideId }
            if (ride != null) {
                selectRide(ride)
                setScreen("messages")
            } else {
                setScreen("messages")
            }
        }
    }

    fun showTestExternalNotification() {
        AppNotificationManager.showTestNotification(getApplication())
    }

    private suspend fun insertAndNotify(notif: NotificationEntity) {
        dao.insertNotification(notif)
        if (notif.userId == currentUserId) {
            AppNotificationManager.showSystemNotification(
                context = getApplication(),
                id = notif.id,
                title = notif.title,
                message = notif.message,
                type = notif.type
            )
        }
    }

    fun swapSearchCities() {
        val temp = searchFromCity.value
        searchFromCity.value = searchToCity.value
        searchToCity.value = temp
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

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val result = repository.deleteNotification(notificationId)
            if (result.isSuccess) {
                dao.deleteNotification(notificationId)
            } else {
                println("ERROR: Failed to delete notification: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            val result = repository.deleteAllNotifications(currentUserId)
            if (result.isSuccess) {
                dao.clearUserNotifications(currentUserId)
            } else {
                println("ERROR: Failed to delete all notifications: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleInsufficientBalanceAlert(show: Boolean) {
        _showInsufficientBalanceAlert.value = show
    }

    fun toggleTopUpModal(show: Boolean) {
        _showTopUpModal.value = show
    }

    // ==========================================
    // User Profile Actions
    // ==========================================

    fun updateUserProfile(name: String, avatarUrl: String, phone: String) {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            val existing = currentUser.value ?: dao.getUser(currentUid)
            val updatedUser = existing?.copy(name = name, avatarUrl = avatarUrl, phone = phone) ?: UserEntity(
                id = currentUid,
                name = name,
                email = "user@example.com",
                phone = phone,
                avatarUrl = avatarUrl,
                rating = 5.0f,
                rideCount = 1,
                isVerified = true,
                walletPoints = 50,
                isSuspended = false,
                suspendReason = null,
                registrationDate = "2026-01-15",
                userRole = "راكب وسائق",
                referralCode = "WASALNI-100"
            )
            dao.insertUser(updatedUser)
            dao.updateDriverProfileInRides(currentUid, name, avatarUrl)
            dao.updateUserProfileInRequestedTrips(currentUid, name, avatarUrl)

            try {
                repository.updateProfile(name, avatarUrl, phone)
                repository.syncPublicUsers()
                repository.fetchRides()
                repository.fetchRequestedTrips()
            } catch (e: Exception) {
                // Ignore network errors, local state already updated
            }

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUid,
                title = "تم تحديث بيانات الملف الشخصي",
                message = "تم تحديث صورتك الشخصية وبيانات حسابك بنجاح وستظهر لجميع المستخدمين.",
                type = NotificationType.SYSTEM.name
            )
            insertAndNotify(notif)
        }
    }

    fun updateFcmToken(token: String) {
        viewModelScope.launch {
            try {
                repository.updateFcmToken(token)
            } catch (e: Exception) {
                android.util.Log.w("WassalniFCM", "Update FCM token failed: ${e.message}")
            }
        }
    }

    // ==========================================
    // Ride Operations
    // ==========================================

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
            repository.publishRide(
                startCity = startCity,
                endCity = endCity,
                date = date,
                time = time,
                pricePerSeat = pricePerSeat,
                availableSeats = availableSeats,
                carModel = carModel,
                carColor = carColor,
                carPlate = carPlate,
                isWomenOnly = isWomenOnly,
                allowsLuggage = allowsLuggage
            )
            _currentScreen.value = "my_rides"
        }
        return true
    }

    fun bookRide(ride: RideEntity, seats: Int = 1) {
        viewModelScope.launch {
            repository.bookRide(ride.id, seats)
        }
    }

    fun cancelRide(rideId: String) {
        viewModelScope.launch {
            repository.cancelRide(rideId)
        }
    }

    fun deletePassengerBooking(bookingId: String, rideId: String) {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            dao.deleteBooking(bookingId)
            if (currentUid.isNotBlank()) {
                dao.deleteBookingByRideId(rideId, currentUid)
            }
            repository.deletePassengerBooking(bookingId, rideId)
            repository.syncUserBookings()
        }
    }

    // ==========================================
    // Requested Trips Operations
    // ==========================================

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
            val res = repository.publishRequestedTrip(
                startCity = startCity,
                endCity = endCity,
                departureDate = date,
                departureTime = time,
                menCount = men,
                womenCount = women,
                childrenCount = children
            )
            repository.syncRequestedTrips()
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUid,
                title = "📌 تم تثبيت طلب رحلتك بنجاح",
                message = "تم نشر وتثبيت طلب رحلتك من $startCity إلى $endCity بنجاح، وسيتم إشعارك فور قبول أي كابتن للطلب.",
                type = NotificationType.SYSTEM.name
            )
            insertAndNotify(notif)
        }
    }

    fun acceptRequestedTrip(requestId: String) {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            val user = currentUser.value ?: dao.getUser(currentUid)
            val driverName = user?.name ?: "كابتن وسلني"
            val driverAvatar = user?.avatarUrl ?: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300"
            repository.acceptRequestedTrip(requestId, currentUid, driverName, driverAvatar)
        }
    }

    fun cancelAcceptedRequestedTrip(requestId: String) {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            val user = currentUser.value ?: dao.getUser(currentUid)
            val driverName = user?.name ?: "كابتن وسلني"
            repository.cancelAcceptedRequestedTrip(requestId, currentUid, driverName)
        }
    }

    fun deleteRequestedTrip(requestId: String) {
        viewModelScope.launch {
            _deletedRequestedTripIds.value = _deletedRequestedTripIds.value + requestId
            dao.deleteRequestedTrip(requestId)
            dao.deleteRide("ride_from_req_$requestId")
            repository.deleteRequestedTrip(requestId)
            addAdminActivityLog("حذف طلب رحلة", "تم حذف الطلب $requestId")
        }
    }

    // ==========================================
    // Wallet & Top-Up Operations
    // ==========================================

    fun submitTopUpRequest(packagePoints: Int, packagePriceUsd: Double, receiptImagePath: String) {
        viewModelScope.launch {
            repository.submitTopUpRequest(packagePoints, packagePriceUsd, receiptImagePath)
        }
    }

    fun deleteWalletTransaction(txId: String) {
        viewModelScope.launch {
            dao.deleteWalletTransaction(txId)
            repository.deleteWalletTransaction(txId)
        }
    }

    fun clearAllWalletTransactions() {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            if (currentUid.isNotBlank()) {
                dao.clearUserWalletTransactions(currentUid)
            } else {
                dao.clearAllWalletTransactions()
            }
            repository.clearAllWalletTransactions()
        }
    }

    // ==========================================
    // Chat Operations
    // ==========================================

    fun sendChatMessage(
        rideId: String,
        text: String,
        imageUri: String? = null,
        audioUri: String? = null,
        audioDuration: Int = 0,
        isLocation: Boolean = false,
        receiverId: String = "driver_id"
    ) {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            val formattedText = text.ifBlank {
                if (audioUri != null) "تسجيل صوتي ($audioDuration ثانية)"
                else if (imageUri != null) "صورة مرفقة"
                else "رسالة"
            }

            // Determine actual target receiver
            val targetReceiver = when {
                receiverId.isNotBlank() && receiverId != "driver_id" && receiverId != "passenger_id" -> receiverId
                rideId.startsWith("chat_user_") -> {
                    val targetUserId = rideId.removePrefix("chat_user_")
                    if (currentUid == targetUserId) "admin" else targetUserId
                }
                else -> {
                    val ride = allRides.value.find { it.id == rideId }
                    if (ride != null && currentUid == ride.driverId) "passenger_id" else (ride?.driverId ?: receiverId)
                }
            }

            val localMsg = ChatMessageEntity(
                id = "msg_${UUID.randomUUID().toString().substring(0, 8)}",
                rideId = rideId,
                senderId = currentUid,
                receiverId = targetReceiver,
                messageText = formattedText,
                imageUri = imageUri,
                audioUri = audioUri,
                audioDurationSeconds = audioDuration,
                isLocation = isLocation,
                timestamp = System.currentTimeMillis()
            )
            // Immediately insert locally for instantaneous UI response
            dao.insertChatMessage(localMsg)

            // If message is directed to another user or admin, send an instant notification
            if (targetReceiver.isNotBlank() && targetReceiver != currentUid && targetReceiver != "all") {
                val senderUser = dao.getUser(currentUid)
                val senderName = if (isAdminLoggedIn.value || currentUid.contains("admin", ignoreCase = true)) {
                    "إدارة التطبيق 🛡️"
                } else {
                    senderUser?.name ?: "مستخدم"
                }
                val notifTitle = "رسالة جديدة من $senderName 💬"
                val notifBody = if (audioUri != null) "أرسل لك تسجيلاً صوتياً 🎙️ ($audioDuration ث)" else if (imageUri != null) "أرسل لك صورة مرفقة 📷" else formattedText
                val notif = NotificationEntity(
                    id = "notif_msg_${UUID.randomUUID().toString().substring(0, 8)}",
                    userId = targetReceiver,
                    title = notifTitle,
                    message = notifBody,
                    type = "CHAT",
                    timestamp = System.currentTimeMillis()
                )
                dao.insertNotification(notif)
            }

            // Sync with backend without duplicate insertion
            val sendResult = repository.sendChatMessage(
                rideId = rideId,
                text = formattedText,
                imageUri = imageUri,
                audioUri = audioUri,
                audioDuration = audioDuration,
                isLocation = isLocation,
                receiverId = targetReceiver,
                existingMessage = localMsg
            )
            // Immediately sync to refresh conversations and messages
            repository.syncChatMessages(rideId)
            repository.syncAllChatMessages()
        }
    }

    fun markRideMessagesAsRead(rideId: String) {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            dao.markAllRideChatMessagesAsRead(rideId)
            if (currentUid.isNotBlank()) {
                dao.markChatMessagesAsRead(rideId, currentUid)
                dao.markChatNotificationsAsRead(currentUid)
            }
            try {
                repository.markChatMessagesAsRead(rideId)
            } catch (_: Exception) {}
        }
    }

    fun markAllMessagesAsRead() {
        viewModelScope.launch {
            val currentUid = activeUserId.value.ifBlank { currentUserId }
            dao.markAllChatMessagesAsRead()
            if (currentUid.isNotBlank()) {
                dao.markChatNotificationsAsRead(currentUid)
            }
            dao.markAllChatNotificationsAsRead()
            try {
                repository.markAllChatMessagesAsRead()
            } catch (_: Exception) {}
        }
    }

    fun startDirectChat(targetUserId: String, targetUserName: String, targetUserAvatar: String = "") {
        val targetUser = UserEntity(
            id = targetUserId,
            name = targetUserName,
            email = "",
            phone = "",
            avatarUrl = targetUserAvatar,
            rating = 5.0f,
            rideCount = 10,
            walletPoints = 0,
            referralCode = ""
        )
        startDirectChatWithUser(targetUser)
    }

    fun startDirectChatWithUser(user: UserEntity) {
        viewModelScope.launch {
            val directChatId = "chat_user_${user.id}"
            // Un-delete if previously deleted
            _deletedChatRideIds.value = _deletedChatRideIds.value - directChatId
            
            val directRide = RideEntity(
                id = directChatId,
                driverId = user.id,
                driverName = user.name,
                driverAvatar = user.avatarUrl,
                startCity = "محادثة مباشرة",
                endCity = user.name,
                departureDate = "دعم وتواصل",
                departureTime = "",
                duration = "فوري",
                pricePerSeat = 0.0,
                availableSeats = 1,
                totalSeats = 1,
                carModel = "دعم وإشراف الإدارة",
                carColor = "أزرق",
                carPlate = "ADMIN",
                driverRating = user.rating,
                driverTripCount = user.rideCount,
                driverVerified = user.isVerified
            )
            dao.insertRide(directRide)
            _selectedRide.value = directRide
            _currentScreen.value = "messages"
            repository.syncChatMessages(directChatId)
            addAdminActivityLog("محادثة مع مستخدم", "بدء محادثة مباشرة مع: ${user.name} (${user.id})")
        }
    }

    fun submitDriverRating(
        driverId: String,
        rideId: String,
        ratingScore: Float,
        reviewComment: String = "",
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val existingRide = dao.getRideById(rideId)
            val currentRating = existingRide?.driverRating ?: 4.9f
            val currentTrips = existingRide?.driverTripCount ?: 12
            val newTrips = currentTrips + 1
            // Compute weighted new rating
            val computedRating = ((currentRating * currentTrips) + ratingScore) / newTrips
            val roundedRating = (Math.round(computedRating * 10.0) / 10.0).toFloat().coerceIn(1.0f, 5.0f)

            dao.updateDriverRatingInRides(driverId, roundedRating, newTrips)
            dao.updateUserRatingAndRides(driverId, roundedRating, newTrips)

            // Update in-memory selected ride
            _selectedRide.value?.let { sel ->
                if (sel.id == rideId || sel.driverId == driverId) {
                    _selectedRide.value = sel.copy(
                        driverRating = roundedRating,
                        driverTripCount = newTrips
                    )
                }
            }

            // Create notification for driver
            val tagText = if (tags.isNotEmpty()) " (${tags.joinToString(", ")})" else ""
            val commentText = if (reviewComment.isNotBlank()) " - \"$reviewComment\"" else ""
            val notification = NotificationEntity(
                id = "notif_rate_${UUID.randomUUID().toString().substring(0, 8)}",
                userId = driverId,
                title = "تقييم جديد ⭐ ($ratingScore/5)",
                message = "تلقيت تقييماً جديداً لرحلتك! $tagText $commentText",
                type = "RATING",
                timestamp = System.currentTimeMillis()
            )
            insertAndNotify(notification)
            addAdminActivityLog("تقييم سائق", "تم تقييم السائق $driverId بدرجة $ratingScore نجوم للرحلة $rideId")
        }
    }

    fun deleteChatConversation(rideId: String) {
        viewModelScope.launch {
            _deletedChatRideIds.value = _deletedChatRideIds.value + rideId
            dao.deleteChatMessagesForRide(rideId)
            if (rideId.startsWith("chat_")) {
                dao.deleteRide(rideId)
            }
            if (_selectedRide.value?.id == rideId) {
                _selectedRide.value = null
            }
            repository.deleteChatConversation(rideId)
            addAdminActivityLog("حذف محادثة", "تم حذف وحجب المحادثة $rideId نهائياً")
        }
    }

    fun deleteChatMessage(messageId: String) {
        viewModelScope.launch {
            dao.deleteChatMessage(messageId)
            repository.deleteChatMessage(messageId)
        }
    }

    fun sendAdminChatMessage(
        rideId: String,
        senderId: String,
        senderName: String,
        text: String,
        imageUri: String? = null,
        isSystem: Boolean = false
    ) {
        viewModelScope.launch {
            val formattedText = text.ifBlank {
                if (imageUri != null) "صورة مرفقة" else "رسالة إدارية"
            }
            val localMsg = ChatMessageEntity(
                id = "admin_msg_${UUID.randomUUID().toString().substring(0, 8)}",
                rideId = rideId,
                senderId = senderId,
                receiverId = "all",
                messageText = formattedText,
                imageUri = imageUri,
                audioUri = null,
                audioDurationSeconds = 0,
                isLocation = false,
                timestamp = System.currentTimeMillis()
            )
            dao.insertChatMessage(localMsg)
            repository.sendChatMessage(
                rideId = rideId,
                text = formattedText,
                imageUri = imageUri,
                audioUri = null,
                audioDuration = 0,
                isLocation = false,
                receiverId = "all"
            )
            addAdminActivityLog("إرسال رسالة في محادثة", "تم إرسال رسالة في غرفة $rideId باسم $senderName")
        }
    }

    fun sendPaymentReminder(rideId: String) {
        viewModelScope.launch {
            val msg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                rideId = rideId,
                senderId = "system",
                receiverId = activeUserId.value,
                messageText = "تذكير: بعد انتهاء الرحلة يرجى تحويل/دفع المبلغ للسائق.",
                isPaymentReminder = true
            )
            dao.insertChatMessage(msg)
        }
    }

    // ==========================================
    // Admin Management Operations
    // ==========================================

    fun approveTopUpRequest(requestId: String) {
        viewModelScope.launch {
            val result = repository.adminApproveTopUp(requestId)

            if (result.isSuccess) {
                // Remove the processed request from local Room immediately.
                dao.deleteTopUpRequest(requestId)

                // Refresh wallet/profile data after successful approval.
                repository.fetchWalletTransactions()
                repository.getProfile()

                addAdminActivityLog(
                    "موافقة شحن نقاط",
                    "تمت الموافقة على طلب الشحن $requestId وإضافة النقاط"
                )
            } else {
                println(
                    "ERROR: Failed to approve top-up $requestId: " +
                        result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun rejectTopUpRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            val result = repository.adminRejectTopUp(requestId, reason)

            if (result.isSuccess) {
                // Remove the processed request from local Room.
                dao.deleteTopUpRequest(requestId)

                addAdminActivityLog(
                    "رفض شحن نقاط",
                    "رفض طلب $requestId بسبب: $reason"
                )
            } else {
                println(
                    "ERROR: Failed to reject top-up $requestId: " +
                        result.exceptionOrNull()?.message
                )
            }
        }
    }

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
        activeUserId.value = repository.tokenMgr.getUserId() ?: "user_default"
        isImpersonating.value = false
        _currentScreen.value = "admin"
        addAdminActivityLog("إنهاء تقمص الهوية", "تم الخروج من حساب $lastImpersonated والعودة للوحة الأدمن")
    }

    fun suspendUser(userId: String, reason: String) {
        viewModelScope.launch {
            repository.adminToggleSuspend(userId, true, reason)
            addAdminActivityLog("تعليق حساب مستخدم", "تم تعليق المستخدم $userId بسبب: $reason")
        }
    }

    fun reactivateUser(userId: String) {
        viewModelScope.launch {
            repository.adminToggleSuspend(userId, false, null)
            addAdminActivityLog("إعادة تفعيل حساب", "تم فك التجميد وتفعيل حساب المستخدم $userId")
        }
    }

    fun adminCreateUser(
        name: String,
        email: String,
        phone: String,
        role: String,
        initialPoints: Int = 50,
        isVerified: Boolean = true,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val res = repository.adminCreateUser(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    role = role,
                    initialPoints = initialPoints,
                    isVerified = isVerified
                )
                if (res.isSuccess) {
                    val created = res.getOrNull()
                    addAdminActivityLog(
                        "إنشاء مستخدم جديد",
                        "تم إنشاء حساب للمستخدم ${created?.name ?: name} برقم ${created?.phone ?: phone} ورصيد $initialPoints نقطة"
                    )
                    onComplete(true, "تم إنشاء المستخدم بنجاح")
                } else {
                    onComplete(false, res.exceptionOrNull()?.message ?: "فشل إنشاء المستخدم")
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "حدث خطأ غير متوقع")
            }
        }
    }

    fun adminDeleteUser(userId: String) {
        viewModelScope.launch {
            dao.deleteUser(userId)
            repository.adminDeleteUser(userId)
            addAdminActivityLog("حذف مستخدم نهائياً", "تم حذف المستخدم $userId نهائياً من قاعدة البيانات")
        }
    }

    fun adminAdjustUserWallet(
        userId: String,
        pointsDelta: Int,
        reason: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = repository.adminAdjustWallet(
                userId,
                pointsDelta,
                reason
            )

            if (result.isSuccess) {
                // The wallet points are updated in Room directly from the server response in repository.
                // We do not add pointsDelta a second time here to prevent double-crediting.
                addAdminActivityLog(
                    "تعديل رصيد مستخدم",
                    "تم تعديل رصيد $userId بمقدار $pointsDelta نقطة. السبب: $reason"
                )

                onResult(true, null)

            } else {

                val error = result.exceptionOrNull()?.message
                    ?: "فشل تعديل الرصيد"

                println(
                    "ERROR: Failed to adjust wallet $userId: $error"
                )

                onResult(false, error)
            }
        }
    }

    fun adminUpdateUserData(userId: String, name: String, phone: String, role: String, points: Int) {
        viewModelScope.launch {
            dao.updateUserData(userId, name, phone, role, points)
            repository.adminUpdateUser(userId, name, "", phone, role, points)
            addAdminActivityLog("تعديل بيانات مستخدم", "تم تعديل بيانات المستخدم $name ($userId)")
        }
    }

    fun adminResetUserPassword(userId: String) {
        viewModelScope.launch {
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "🔒 إعادة تعيين كلمة المرور",
                message = "تمت إعادة تعيين كلمة المرور الخاصة بك إلى (123456) من قبل مدير النظام.",
                type = NotificationType.SYSTEM.name
            )
            insertAndNotify(notif)
            addAdminActivityLog("إعادة تعيين كلمة مرور", "تم إعادة تعيين كلمة المرور للمستخدم $userId")
        }
    }

    fun adminCancelWalletTransaction(txId: String) {
        viewModelScope.launch {
            dao.deleteWalletTransaction(txId)
            addAdminActivityLog("إلغاء حركة مالية", "تم إلغاء وحذف المعاملة المالية $txId")
        }
    }

    fun adminEditRide(rideId: String, start: String, end: String, date: String, time: String, price: Double, seats: Int) {
        viewModelScope.launch {
            dao.updateRideDetails(rideId, start, end, date, time, price, seats)
            repository.adminUpdateRide(
                rideId = rideId,
                startCity = start,
                endCity = end,
                departureDate = date,
                departureTime = time,
                pricePerSeat = price,
                availableSeats = seats,
                status = "UPCOMING"
            )
            addAdminActivityLog("تعديل تفاصيل رحلة", "تم تعديل الرحلة $rideId ($start ➔ $end)")
        }
    }

    fun adminDeleteRide(rideId: String) {
        viewModelScope.launch {
            _deletedRideIds.value = _deletedRideIds.value + rideId
            val ride = dao.getRideById(rideId)
            dao.deleteRide(rideId)
            dao.deleteChatMessagesForRide(rideId)
            _deletedChatRideIds.value = _deletedChatRideIds.value + rideId
            if (_selectedRide.value?.id == rideId) {
                _selectedRide.value = null
            }
            repository.adminDeleteRide(rideId, "حذف إداري نهائي")
            if (ride != null) {
                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = ride.driverId,
                    title = "حذف رحلة من قبل الإدارة",
                    message = "تم حذف رحلتك (${ride.startCity} إلى ${ride.endCity}) من قبل إدارة التطبيق.",
                    type = NotificationType.SYSTEM.name,
                    timestamp = System.currentTimeMillis()
                )
                insertAndNotify(notif)
            }
            addAdminActivityLog("حذف رحلة نهائياً", "تم حذف الرحلة $rideId من قاعدة البيانات نهائياً")
        }
    }

    fun cancelRideByAdmin(rideId: String, reason: String) {
        viewModelScope.launch {
            _deletedRideIds.value = _deletedRideIds.value + rideId
            val ride = dao.getRideById(rideId)
            dao.deleteRide(rideId)
            dao.deleteChatMessagesForRide(rideId)
            _deletedChatRideIds.value = _deletedChatRideIds.value + rideId
            if (_selectedRide.value?.id == rideId) {
                _selectedRide.value = null
            }
            repository.adminDeleteRide(rideId, reason)
            if (ride != null) {
                val notif = NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = ride.driverId,
                    title = "إلغاء وحذف رحلة من قبل الإدارة",
                    message = "تم إلغاء وحذف رحلتك (${ride.startCity} إلى ${ride.endCity}) من قبل إدارة التطبيق. السبب: ${reason.ifBlank { "إلغاء إداري" }}",
                    type = NotificationType.SYSTEM.name,
                    timestamp = System.currentTimeMillis()
                )
                insertAndNotify(notif)
            }
            addAdminActivityLog("إلغاء وحذف رحلة", "تم إلغاء وحذف الرحلة $rideId بواسطة الأدمن بسبب: $reason")
        }
    }

    fun adminEditRequestedTrip(requestId: String, start: String, end: String, date: String, time: String, men: Int, women: Int, children: Int) {
        viewModelScope.launch {
            dao.updateRequestedTripDetails(requestId, start, end, date, time, men, women, children)
            repository.adminUpdateRequestedTrip(
                tripId = requestId,
                startCity = start,
                endCity = end,
                departureDate = date,
                departureTime = time,
                menCount = men,
                womenCount = women,
                childrenCount = children,
                status = "OPEN"
            )
            addAdminActivityLog("تعديل طلب رحلة", "تم تعديل الطلب $requestId ($start ➔ $end)")
        }
    }

    fun adminReopenRequestedTrip(requestId: String) {
        viewModelScope.launch {
            dao.updateRequestedTripStatus(requestId, "OPEN", null, null)
            dao.deleteRide("ride_from_req_$requestId")
            repository.adminReopenRequestedTrip(requestId)
            addAdminActivityLog("إعادة فتح طلب رحلة", "تمت إعادة فتح الطلب $requestId")
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

    fun editChatMessage(rideId: String, messageId: String, newText: String) {
        viewModelScope.launch {
            dao.updateChatMessageText(messageId, newText)
            repository.adminEditChatMessage(rideId, messageId, newText)
            addAdminActivityLog("تعديل رسالة محادثة", "تم تعديل نص الرسالة $messageId")
        }
    }

    fun deleteChatMessage(rideId: String, messageId: String) {
        viewModelScope.launch {
            dao.deleteChatMessage(messageId)
            repository.adminDeleteChatMessage(rideId, messageId)
            addAdminActivityLog("حذف رسالة محادثة", "تم حذف الرسالة $messageId")
        }
    }

    fun deleteChatRoom(rideId: String) {
        deleteChatConversation(rideId)
    }

    fun sendBroadcastNotification(
        title: String,
        message: String,
        targetAudience: String,
        type: String = NotificationType.SYSTEM.name
    ) {
        viewModelScope.launch {
            repository.adminBroadcast(title, message, targetAudience)
            val allUsers = dao.getAllUsers().first()
            val targetUsers = when (targetAudience) {
                "DRIVERS" -> allUsers.filter { it.userRole.contains("DRIVER", ignoreCase = true) }
                "PASSENGERS" -> allUsers.filter { it.userRole.contains("PASSENGER", ignoreCase = true) }
                else -> allUsers
            }
            targetUsers.forEach { u ->
                val notif = NotificationEntity(
                    id = "notif_bc_${UUID.randomUUID().toString().substring(0, 8)}",
                    userId = u.id,
                    title = title,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
                dao.insertNotification(notif)
            }
            if (targetUsers.none { it.id == currentUserId }) {
                val notif = NotificationEntity(
                    id = "notif_bc_${UUID.randomUUID().toString().substring(0, 8)}",
                    userId = currentUserId,
                    title = title,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
                insertAndNotify(notif)
            }
            addAdminActivityLog("إرسال إشعار جماعي", "العنوان: $title | الفئة: $targetAudience")
        }
    }

    fun updateShamCashAccount(acc: String) {
        if (acc.isNotBlank()) {
            shamCashAccount.value = acc
            viewModelScope.launch {
                repository.updateRemoteSettings(shamCashAccount = acc)
            }
            addAdminActivityLog("تعديل حساب شام كاش الموحد", "تم تحديث رقم حساب شام كاش الموحد إلى: $acc")
        }
    }

    fun updateAppDownloadUrl(url: String) {
        if (url.isNotBlank()) {
            appDownloadUrl.value = url
            viewModelScope.launch {
                repository.updateRemoteSettings(appDownloadUrl = url)
            }
            addAdminActivityLog("تعديل رابط تنزيل التطبيق", "تم تحديث رابط التنزيل الموحد إلى: $url")
        }
    }

    fun updateRemoteAppConfig(name: String, tagline: String, logoUrl: String, iconVariant: String, maintenance: Boolean) {
        appName.value = name
        appTagline.value = tagline
        appLogoUrl.value = logoUrl
        dynamicIconVariant.value = iconVariant
        isMaintenanceMode.value = maintenance
        viewModelScope.launch {
            repository.updateRemoteSettings(
                appName = name,
                appTagline = tagline,
                appLogoUrl = logoUrl,
                dynamicIconVariant = iconVariant,
                isMaintenanceMode = maintenance
            )
        }
        addAdminActivityLog("تحديث الإعدادات العامة", "اسم التطبيق: $name | الصيانة: $maintenance")
    }

    fun addAdminActivityLog(action: String, details: String) {
        val newLog = AdminActivityLog(
            id = UUID.randomUUID().toString(),
            actionName = action,
            details = details,
            timestamp = "2026-08-07 " + java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        )
        _adminActivityLogs.update { listOf(newLog) + it }
    }

    fun replySupportTicket(ticketId: String, replyText: String) {
        _supportTickets.value = _supportTickets.value.map {
            if (it.id == ticketId) it.copy(status = "RESOLVED", adminReply = replyText) else it
        }
        addAdminActivityLog("الرد على تذكرة دعم", "التذكرة $ticketId - تمت المعالجة والإغلاق")
    }

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

    fun processTelegramCommand(command: String, params: Map<String, String>): String {
        return when (command) {
            "/start" -> "أهلاً بك في بوت وسلني (Wasalni Bot)! يمكنك البحث عن الرحلات ونشر رحلة وإدارة محفظتك عبر Telegram."
            "/rides" -> {
                val rides = allRides.value
                if (rides.isEmpty()) "لا توجد رحلات متاحة حالياً."
                else "الرحلات المتاحة:\n" + rides.joinToString("\n") {
                    "🚘 ${it.startCity} ➔ ${it.endCity} | السائق: ${it.driverName} | السعر: \$${it.pricePerSeat} | المقاعد: ${it.availableSeats}"
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
