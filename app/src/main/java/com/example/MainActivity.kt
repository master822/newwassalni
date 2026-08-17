package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.*
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

        setContent {
            val viewModel: AppViewModel = viewModel()

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
            val chatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle(initialValue = emptyList())
            val requestedTrips by viewModel.requestedTrips.collectAsStateWithLifecycle()

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

            val searchFrom by viewModel.searchFromCity.collectAsStateWithLifecycle()
            val searchTo by viewModel.searchToCity.collectAsStateWithLifecycle()
            val searchDate by viewModel.searchDate.collectAsStateWithLifecycle()
            val searchPassengers by viewModel.searchPassengers.collectAsStateWithLifecycle()

            val layoutDirection = if (language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr
            val coroutineScope = rememberCoroutineScope()

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                WassalniTheme(darkTheme = isDarkMode) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            HeaderBar(
                                userPoints = userPoints,
                                unreadNotificationsCount = unreadCount,
                                language = language,
                                onWalletClick = { viewModel.setScreen("wallet") },
                                onNotificationClick = { viewModel.toggleNotificationsDialog(true) },
                                onSettingsClick = { viewModel.toggleSettingsDialog(true) },
                                onAuthClick = { showAuthDialog = true }
                            )
                        },
                        bottomBar = {
                            BottomNavBar(
                                currentRoute = currentScreen,
                                onTabSelected = { tab -> viewModel.setScreen(tab.route) },
                                language = language,
                                isAdmin = isAdminLoggedIn
                            )
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

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                                    onBookRide = { r, seats -> viewModel.bookRide(r, seats) },
                                                    onOpenChat = { r ->
                                                        viewModel.selectRide(r)
                                                        viewModel.setScreen("messages")
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
                                            allRides = allRides,
                                            language = language,
                                            currentUserId = viewModel.currentUserId,
                                            onSelectConversation = { r -> viewModel.selectRide(r) },
                                            onSendMessage = { text, img, isLoc ->
                                                val rideId = selectedRide?.id ?: "ride_1"
                                                viewModel.sendChatMessage(rideId, text, img, isLoc)
                                            },
                                            onSendPaymentReminder = {
                                                val rideId = selectedRide?.id ?: "ride_1"
                                                viewModel.sendPaymentReminder(rideId)
                                            },
                                            onBackToList = { viewModel.selectRide(null) }
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
                            onDismiss = { viewModel.toggleNotificationsDialog(false) }
                        )
                    }

                    // Authentication & Account Dialog
                    if (showAuthDialog || !isLoggedIn) {
                        AuthDialog(
                            language = language,
                            isLoggedIn = isLoggedIn,
                            isMandatory = !isLoggedIn,
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
                            onLogout = {
                                viewModel.logoutUser()
                                showAuthDialog = false
                            },
                            onDismiss = { showAuthDialog = false }
                        )
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelId = "wassalni_notifications"
            val channelName = getString(R.string.notification_channel_name)
            val channelDesc = getString(R.string.notification_channel_desc)
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                description = channelDesc
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#1E7A5F")
                enableVibration(true)
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
