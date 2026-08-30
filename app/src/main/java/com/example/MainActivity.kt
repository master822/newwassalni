package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.util.AppNotificationManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.model.AppLanguage
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private var appViewModel: AppViewModel? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "Notification permission granted: $isGranted")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val openNotifs = intent.getBooleanExtra(AppNotificationManager.EXTRA_OPEN_NOTIFICATIONS, false)
        val rideId = intent.getStringExtra(AppNotificationManager.EXTRA_RIDE_ID)
        if (!rideId.isNullOrBlank()) {
            appViewModel?.openRideChat(rideId)
        } else if (openNotifs) {
            appViewModel?.toggleNotificationsDialog(true)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppNotificationManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        try {
            com.example.service.WassalniBackgroundSyncReceiver.schedule(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error scheduling background sync", e)
        }

        setContent {
            val viewModel: AppViewModel = viewModel()
            appViewModel = viewModel

            LaunchedEffect(Unit) {
                handleNotificationIntent(intent)
                try {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                if (!token.isNullOrBlank()) {
                                    viewModel.updateFcmToken(token)
                                }
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Firebase Messaging token fetch error: ${e.message}")
                }
            }

            val language by viewModel.appLanguage.collectAsStateWithLifecycle()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val selectedRide by viewModel.selectedRide.collectAsStateWithLifecycle()

            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val userPoints = currentUser?.walletPoints ?: 50

            val allRides by viewModel.allRides.collectAsStateWithLifecycle()
            val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
            val userBookings by viewModel.userBookings.collectAsStateWithLifecycle()
            val walletTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()
            val topUpRequests by viewModel.topUpRequests.collectAsStateWithLifecycle()
            val notifications by viewModel.notifications.collectAsStateWithLifecycle()
            val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
            val unreadMessagesCount by viewModel.unreadMessagesCount.collectAsStateWithLifecycle()
            val chatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle(initialValue = emptyList())
            val requestedTrips by viewModel.requestedTrips.collectAsStateWithLifecycle()
            val allChatMessages by viewModel.allChatMessages.collectAsStateWithLifecycle()
            val deletedChatRideIds by viewModel.deletedChatRideIds.collectAsStateWithLifecycle()

            val isImpersonating by viewModel.isImpersonating.collectAsStateWithLifecycle()
            val impersonatedUser by viewModel.impersonatedUser.collectAsStateWithLifecycle()
            val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()

            val showSettings by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
            val showNotifications by viewModel.showNotificationsDialog.collectAsStateWithLifecycle()
            val showInsufficientAlert by viewModel.showInsufficientBalanceAlert.collectAsStateWithLifecycle()
            val showTopUpModal by viewModel.showTopUpModal.collectAsStateWithLifecycle()
            val shamCashAccount by viewModel.shamCashAccount.collectAsStateWithLifecycle()
            val appDownloadUrl by viewModel.appDownloadUrl.collectAsStateWithLifecycle()
            val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
            var showAuthDialog by remember { mutableStateOf(false) }
            var showSplashScreen by remember { mutableStateOf(true) }

            val searchFrom by viewModel.searchFromCity.collectAsStateWithLifecycle()
            val searchTo by viewModel.searchToCity.collectAsStateWithLifecycle()
            val searchDate by viewModel.searchDate.collectAsStateWithLifecycle()
            val searchPassengers by viewModel.searchPassengers.collectAsStateWithLifecycle()

            val layoutDirection = if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
            val coroutineScope = rememberCoroutineScope()

            var isRefreshing by remember { mutableStateOf(false) }

            val performRefresh: () -> Unit = {
                isRefreshing = true
                viewModel.refreshAllData()
                coroutineScope.launch {
                    kotlinx.coroutines.delay(800L)
                    isRefreshing = false
                }
            }

            var lastBackPressTime by remember { mutableLongStateOf(0L) }

            // Physical Android back button & gesture navigation handler
            BackHandler {
                if (showAuthDialog && isLoggedIn) {
                    showAuthDialog = false
                } else {
                    val handled = viewModel.navigateBack()
                    if (!handled) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000) {
                            finish()
                        } else {
                            lastBackPressTime = currentTime
                            val exitMsg = if (language == AppLanguage.ARABIC) {
                                "اضغط مرة أخرى للرجوع والخروج من التطبيق"
                            } else {
                                "Press back again to exit"
                            }
                            android.widget.Toast.makeText(this@MainActivity, exitMsg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                WassalniTheme(darkTheme = isDarkMode) {
                    if (showSplashScreen) {
                        SplashScreen(
                            appName = "وصلني",
                            appTagline = "نسافر معاً، نوصل بأمان 🚗💨",
                            onSplashFinished = { showSplashScreen = false }
                        )
                    } else if (!isLoggedIn) {
                        AuthScreen(
                            language = language,
                            isDarkMode = isDarkMode,
                            onLoginSuccess = { emailOrPhone, password ->
                                coroutineScope.launch {
                                    val result = viewModel.loginUserAccount(emailOrPhone, password)
                                    android.widget.Toast.makeText(this@MainActivity, result.second, android.widget.Toast.LENGTH_SHORT).show()
                                    if (result.first && viewModel.isAdminLoggedIn.value) {
                                        viewModel.setScreen("admin")
                                    }
                                }
                            },
                            onRegisterSuccess = { name, email, phone, pass, refCode, verifyToken ->
                                coroutineScope.launch {
                                    val result = viewModel.registerUserAccount(
                                        name,
                                        email,
                                        phone,
                                        pass,
                                        refCode,
                                        verifyToken
                                    )
                                    android.widget.Toast.makeText(this@MainActivity, result.second, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onSendPhoneOtp = { phone ->
                                viewModel.sendPhoneOtp(phone)
                            },
                            onVerifyPhoneOtp = { phone, otp ->
                                viewModel.verifyPhoneOtp(phone, otp)
                            },
                            onResetPasswordWithPhone = { phone, otp, pass ->
                                viewModel.resetPasswordWithPhone(phone, otp, pass)
                            },
                            onToggleDarkMode = { viewModel.toggleDarkMode() },
                            onLanguageChange = { viewModel.setLanguage(it) }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                        topBar = {
                            HeaderBar(
                                userPoints = userPoints,
                                unreadNotificationsCount = unreadCount,
                                language = language,
                                currentUserAvatar = currentUser?.avatarUrl,
                                onWalletClick = { viewModel.setScreen("wallet") },
                                onNotificationClick = { viewModel.toggleNotificationsDialog(true) },
                                onSettingsClick = { viewModel.toggleSettingsDialog(true) },
                                onAuthClick = { showAuthDialog = true }
                            )
                        },
                        bottomBar = {
                            if (currentScreen != "messages" || selectedRide == null) {
                                BottomNavBar(
                                    currentRoute = currentScreen,
                                    onTabSelected = { tab -> viewModel.setScreen(tab.route) },
                                    language = language,
                                    isAdmin = isAdminLoggedIn,
                                    unreadMessagesCount = unreadMessagesCount
                                )
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Impersonation Banner for Super Admin
                            if (isImpersonating && impersonatedUser != null) {
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.SwitchAccount,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "أنت تتصفح الآن بحساب: ${impersonatedUser?.name}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.stopImpersonation()
                                                viewModel.setScreen("admin")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("العودة للأدمن", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = performRefresh,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "screen_transition"
                                ) { screen ->
                                    when (screen) {
                                        "search" -> SearchScreen(
                                            fromCity = searchFrom,
                                            toCity = searchTo,
                                            date = searchDate,
                                            passengers = searchPassengers,
                                            suggestedRides = allRides,
                                            language = language,
                                            onFromChange = { viewModel.searchFromCity.value = it },
                                            onToChange = { viewModel.searchToCity.value = it },
                                            onSwap = { viewModel.swapSearchCities() },
                                            onPassengersChange = { viewModel.searchPassengers.value = it },
                                            onSearchClick = { viewModel.setScreen("search_results") },
                                            onRideClick = { ride ->
                                                viewModel.selectRide(ride)
                                                viewModel.setScreen("ride_details")
                                            },
                                            onOpenRequestedTrips = { viewModel.setScreen("requested_trips") }
                                        )

                                        "search_results" -> SearchResultsScreen(
                                            fromCity = searchFrom,
                                            toCity = searchTo,
                                            rides = allRides,
                                            language = language,
                                            onRideClick = { ride ->
                                                viewModel.selectRide(ride)
                                                viewModel.setScreen("ride_details")
                                            },
                                            onBack = { viewModel.setScreen("search") }
                                        )

                                        "ride_details" -> {
                                            selectedRide?.let { ride ->
                                                RideDetailScreen(
                                                    ride = ride,
                                                    language = language,
                                                    allUsers = allUsers,
                                                    onBookRide = { r, seats -> viewModel.bookRide(r, seats) },
                                                    onOpenChat = { r ->
                                                        viewModel.selectRide(r)
                                                        viewModel.setScreen("messages")
                                                    },
                                                    onRateDriver = { driverId, rideId, stars, comment, tags ->
                                                        viewModel.submitDriverRating(driverId, rideId, stars, comment, tags)
                                                    },
                                                    onBack = { viewModel.setScreen("search_results") }
                                                )
                                            }
                                        }

                                        "my_rides" -> MyRidesScreen(
                                            driverRides = allRides.filter { it.driverId == viewModel.currentUserId },
                                            passengerBookings = userBookings,
                                            allRides = allRides,
                                            language = language,
                                            onCancelRide = { rideId -> viewModel.cancelRide(rideId) },
                                            onDeleteRide = { rideId -> viewModel.cancelRide(rideId) },
                                            onDeleteBooking = { bookingId, rideId ->
                                                viewModel.deletePassengerBooking(bookingId, rideId)
                                            },
                                            onOpenChat = { ride ->
                                                viewModel.selectRide(ride)
                                                viewModel.setScreen("messages")
                                            }
                                        )

                                        "publish" -> PublishRideScreen(
                                            userPoints = userPoints,
                                            language = language,
                                            onPublish = { start, end, d, t, seats, price, carModel, carColor, carPlate, women, luggage ->
                                                viewModel.publishRide(start, end, d, t, seats, price, carModel, carColor, carPlate, women, luggage)
                                            },
                                            onOpenTopUpModal = { viewModel.toggleTopUpModal(true) }
                                        )

                                        "messages" -> MessagesScreen(
                                            ride = selectedRide,
                                            messages = chatMessages,
                                            allRides = allRides.filter { it.id !in deletedChatRideIds },
                                            allChatMessages = allChatMessages,
                                            allUsers = allUsers,
                                            deletedChatRideIds = deletedChatRideIds,
                                            language = language,
                                            currentUserId = viewModel.activeUserId.value.ifBlank { viewModel.currentUserId },
                                            onSelectConversation = { r -> 
                                                viewModel.selectRide(r)
                                                viewModel.markRideMessagesAsRead(r.id)
                                            },
                                            onSendMessage = { text, img, audio, audioDuration, isLoc ->
                                                val rideId = selectedRide?.id ?: "ride_1"
                                                val currentUid = viewModel.activeUserId.value.ifBlank { viewModel.currentUserId }
                                                val isDirect = rideId.startsWith("chat_user_")
                                                val targetReceiver = if (isDirect) {
                                                    val targetUid = rideId.removePrefix("chat_user_")
                                                    if (currentUid == targetUid) "admin" else targetUid
                                                } else if (selectedRide?.driverId == currentUid) {
                                                    "passenger_id"
                                                } else {
                                                    selectedRide?.driverId ?: "driver_id"
                                                }
                                                viewModel.sendChatMessage(
                                                    rideId = rideId,
                                                    text = text,
                                                    imageUri = img,
                                                    audioUri = audio,
                                                    audioDuration = audioDuration,
                                                    isLocation = isLoc,
                                                    receiverId = targetReceiver
                                                )
                                            },
                                            onDeleteConversation = { rideId ->
                                                viewModel.deleteChatConversation(rideId)
                                            },
                                            onDeleteMessage = { messageId ->
                                                viewModel.deleteChatMessage(selectedRide?.id ?: "", messageId)
                                            },
                                            onSendPaymentReminder = {
                                                val rideId = selectedRide?.id ?: "ride_1"
                                                viewModel.sendPaymentReminder(rideId)
                                            },
                                            onBackToList = { viewModel.selectRide(null) },
                                            onMarkAllAsRead = { viewModel.markAllMessagesAsRead() },
                                            onMarkMessagesAsRead = { rideId -> viewModel.markRideMessagesAsRead(rideId) }
                                        )

                                        "requested_trips" -> RequestedTripsScreen(
                                            currentUserId = viewModel.currentUserId,
                                            requestedTrips = requestedTrips,
                                            language = language,
                                            onBackClick = { viewModel.setScreen("search") },
                                            onPublishRequest = { start, end, date, time, men, women, children ->
                                                viewModel.publishRequestedTrip(start, end, date, time, men, women, children)
                                            },
                                            onAcceptRequest = { reqId ->
                                                viewModel.acceptRequestedTrip(reqId)
                                            },
                                            onCancelAcceptedRequest = { reqId ->
                                                viewModel.cancelAcceptedRequestedTrip(reqId)
                                            },
                                            onDeleteRequest = { reqId ->
                                                viewModel.deleteRequestedTrip(reqId)
                                            },
                                            onOpenChat = { targetUid, name, avatar ->
                                                viewModel.startDirectChat(targetUid, name, avatar)
                                            }
                                        )

                                        "wallet" -> WalletScreen(
                                            userPoints = userPoints,
                                            transactions = walletTransactions,
                                            language = language,
                                            userReferralCode = currentUser?.referralCode ?: "WASALNI-100",
                                            appDownloadUrl = appDownloadUrl,
                                            showTopUpModal = showTopUpModal,
                                            shamCashCode = shamCashAccount,
                                            onToggleTopUpModal = { viewModel.toggleTopUpModal(it) },
                                            onSubmitTopUpRequest = { pts, usd, path ->
                                                viewModel.submitTopUpRequest(pts, usd, path)
                                            },
                                            onDeleteTransaction = { txId ->
                                                viewModel.deleteWalletTransaction(txId)
                                            },
                                            onClearAllTransactions = {
                                                viewModel.clearAllWalletTransactions()
                                            }
                                        )

                                        "admin" -> AdminDashboardScreen(
                                            viewModel = viewModel,
                                            topUpRequests = topUpRequests,
                                            allUsers = allUsers,
                                            allRides = allRides,
                                            language = language,
                                            onApproveRequest = { id -> viewModel.approveTopUpRequest(id) },
                                            onRejectRequest = { id, reason -> viewModel.rejectTopUpRequest(id, reason) },
                                            onLogoutAdmin = { viewModel.setScreen("search") },
                                            onBack = { viewModel.setScreen("search") }
                                        )

                                        else -> SearchScreen(
                                            fromCity = searchFrom,
                                            toCity = searchTo,
                                            date = searchDate,
                                            passengers = searchPassengers,
                                            suggestedRides = allRides,
                                            language = language,
                                            onFromChange = { viewModel.searchFromCity.value = it },
                                            onToChange = { viewModel.searchToCity.value = it },
                                            onSwap = { viewModel.swapSearchCities() },
                                            onPassengersChange = { viewModel.searchPassengers.value = it },
                                            onSearchClick = { viewModel.setScreen("search_results") },
                                            onRideClick = { ride ->
                                                viewModel.selectRide(ride)
                                                viewModel.setScreen("ride_details")
                                            },
                                            onOpenRequestedTrips = { viewModel.setScreen("requested_trips") }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Insufficient Balance Dialog
                    if (showInsufficientAlert) {
                        AlertDialog(
                            onDismissRequest = { viewModel.toggleInsufficientBalanceAlert(false) },
                            title = { Text(AppStrings.get("insufficient_balance", language), fontWeight = FontWeight.Bold) },
                            text = { Text(AppStrings.get("insufficient_balance_msg", language)) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.toggleInsufficientBalanceAlert(false)
                                        viewModel.setScreen("wallet")
                                        viewModel.toggleTopUpModal(true)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("alert_topup_now_btn")
                                ) {
                                    Text(AppStrings.get("topup_now", language), color = Color.White)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.toggleInsufficientBalanceAlert(false) }) {
                                    Text("إلغاء")
                                }
                            }
                        )
                    }

                    // Settings Dialog
                    if (showSettings) {
                        SettingsDialog(
                            currentUser = currentUser,
                            currentLanguage = language,
                            isDarkMode = isDarkMode,
                            onLanguageChange = { viewModel.setLanguage(it) },
                            onToggleDarkMode = { viewModel.toggleDarkMode() },
                            onUpdateProfile = { name, avatarUrl, phone ->
                                viewModel.updateUserProfile(name, avatarUrl, phone)
                            },
                            onDismiss = { viewModel.toggleSettingsDialog(false) }
                        )
                    }

                    // Notification Center Dialog
                    if (showNotifications) {
                        NotificationCenterDialog(
                            notifications = notifications,
                            language = language,
                            onDismiss = { viewModel.toggleNotificationsDialog(false) },
                            onDeleteNotification = { notificationId ->
                                viewModel.deleteNotification(notificationId)
                            },
                            onDeleteAllNotifications = {
                                viewModel.deleteAllNotifications()
                            },
                            onTestNotification = {
                                viewModel.showTestExternalNotification()
                            }
                        )
                    }

                    // Authentication & Account Dialog
                    if (showAuthDialog) {
                        AuthDialog(
                            language = language,
                            isLoggedIn = isLoggedIn,
                            isMandatory = false,
                            userName = currentUser?.name ?: "أحمد المحمد",
                            userEmail = currentUser?.email ?: "ahmed@wasalni.app",
                            userPhone = currentUser?.phone ?: "+963 988 123 456",
                            userPoints = userPoints,
                            onLoginSuccess = { emailOrPhone, password ->
                                coroutineScope.launch {
                                    val result = viewModel.loginUserAccount(emailOrPhone, password)
                                    android.widget.Toast.makeText(this@MainActivity, result.second, android.widget.Toast.LENGTH_SHORT).show()
                                    if (result.first) {
                                        showAuthDialog = false
                                        if (viewModel.isAdminLoggedIn.value) {
                                            viewModel.setScreen("admin")
                                        }
                                    }
                                }
                            },
                            onRegisterSuccess = { name, email, phone, pass, refCode, verifyToken ->
                                coroutineScope.launch {
                                    val result = viewModel.registerUserAccount(
                                        name,
                                        email,
                                        phone,
                                        pass,
                                        refCode,
                                        verifyToken
                                    )
                                    android.widget.Toast.makeText(this@MainActivity, result.second, android.widget.Toast.LENGTH_LONG).show()
                                    if (result.first) {
                                        showAuthDialog = false
                                    }
                                }
                            },
                            onSendPhoneOtp = { phone ->
                                viewModel.sendPhoneOtp(phone)
                            },
                            onVerifyPhoneOtp = { phone, otp ->
                                viewModel.verifyPhoneOtp(phone, otp)
                            },
                            onResetPasswordPhone = { phone, otp, pass ->
                                viewModel.resetPasswordWithPhone(phone, otp, pass)
                            },
                            onForgotPasswordEmail = { email ->
                                viewModel.sendForgotPasswordEmail(email)
                            },
                            onResetPasswordEmail = { email, otp, pass ->
                                viewModel.resetPasswordWithEmail(email, otp, pass)
                            },
                            onLogout = {
                                viewModel.logoutUser()
                                showAuthDialog = false
                            },
                            onOpenWallet = {
                                showAuthDialog = false
                                viewModel.setScreen("wallet")
                            },
                            onDismiss = { showAuthDialog = false }
                        )
                    }
                }
                }
            }
        }
    }
}
