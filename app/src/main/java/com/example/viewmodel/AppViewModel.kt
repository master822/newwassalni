package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.WassalniRepository
import kotlinx.coroutines.flow.*
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
    private val _isAdminLoggedIn = MutableStateFlow(repository.tokenMgr.getUserRole() == "ADMIN" || repository.tokenMgr.getUserRole() == "SUPER_ADMIN")
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
        viewModelScope.launch {
            try {
                syncInitialData()
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
                )
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
        referralCode: String?
    ): Pair<Boolean, String> {
        val result = repository.register(name, email, phone, pass, referralCode)
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

    // ==========================================
    // User Profile Actions
    // ==========================================

    fun updateUserProfile(name: String, avatarUrl: String, phone: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updatedUser = user.copy(name = name, avatarUrl = avatarUrl, phone = phone)
            dao.insertUser(updatedUser)

            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                title = "تم تحديث بيانات الملف الشخصي",
                message = "تم تحديث البيانات بنجاح.",
                type = NotificationType.SYSTEM.name
            )
            dao.insertNotification(notif)
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
            repository.publishRequestedTrip(
                startCity = startCity,
                endCity = endCity,
                departureDate = date,
                departureTime = time,
                menCount = men,
                womenCount = women,
                childrenCount = children
            )
        }
    }

    fun acceptRequestedTrip(requestId: String) {
        viewModelScope.launch {
            repository.acceptRequestedTrip(requestId)
        }
    }

    fun cancelAcceptedRequestedTrip(requestId: String) {
        viewModelScope.launch {
            repository.cancelAcceptedRequestedTrip(requestId)
        }
    }

    fun deleteRequestedTrip(requestId: String) {
        viewModelScope.launch {
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

    // ==========================================
    // Chat Operations
    // ==========================================

    fun sendChatMessage(
        rideId: String,
        text: String,
        imageUri: String? = null,
        isLocation: Boolean = false,
        senderId: String = currentUserId,
        receiverId: String = "driver_id"
    ) {
        viewModelScope.launch {
            repository.sendChatMessage(
                rideId = rideId,
                text = text,
                imageUri = imageUri,
                isLocation = isLocation,
                receiverId = receiverId
            )
        }
    }

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

    // ==========================================
    // Admin Management Operations
    // ==========================================

    fun approveTopUpRequest(requestId: String) {
        viewModelScope.launch {
            repository.adminApproveTopUp(requestId)
            addAdminActivityLog("موافقة شحن نقاط", "الموافقة على طلب الشحن $requestId")
        }
    }

    fun rejectTopUpRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            repository.adminRejectTopUp(requestId, reason)
            addAdminActivityLog("رفض شحن نقاط", "رفض طلب $requestId بسبب: $reason")
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

    fun adminAdjustUserWallet(userId: String, pointsDelta: Int, reason: String) {
        viewModelScope.launch {
            repository.adminAdjustWallet(userId, pointsDelta, reason)
            addAdminActivityLog("تعديل رصيد مستخدم", "تعديل رصيد $userId بمقدار $pointsDelta نقطة. السبب: $reason")
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
            dao.insertNotification(notif)
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
            dao.deleteRide(rideId)
            repository.adminDeleteRide(rideId, "حذف إداري نهائي")
            addAdminActivityLog("حذف رحلة نهائياً", "تم حذف الرحلة $rideId من قاعدة البيانات")
        }
    }

    fun cancelRideByAdmin(rideId: String, reason: String) {
        viewModelScope.launch {
            dao.updateRideStatus(rideId, RideStatus.CANCELLED.name)
            repository.adminDeleteRide(rideId, reason)
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
        viewModelScope.launch {
            dao.deleteChatMessagesForRide(rideId)
            repository.adminClearChatRoom(rideId)
            addAdminActivityLog("تفريغ محادثة رحلة", "تم حذف جميع رسائل الرحلة $rideId")
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
            repository.adminSendChatMessage(rideId, messageText)
            addAdminActivityLog("إرسال رسالة كأدمن", "إرسال رسالة للرحلة $rideId")
        }
    }

    fun sendBroadcastNotification(
        title: String,
        message: String,
        targetAudience: String,
        type: String = NotificationType.SYSTEM.name
    ) {
        viewModelScope.launch {
            repository.adminBroadcast(title, message, targetAudience)
            // Also insert local notification for current user so it's instantly visible
            val notif = NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                title = title,
                message = message,
                type = NotificationType.SYSTEM.name
            )
            dao.insertNotification(notif)
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
