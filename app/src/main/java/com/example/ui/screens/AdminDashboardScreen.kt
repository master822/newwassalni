package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.normalizeSyrianPhoneNumber
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

enum class AdminSection(val titleAr: String, val subtitleAr: String, val icon: ImageVector, val accentColor: Color) {
    USERS("إدارة المستخدمين والحسابات", "تعديل البيانات، الأرصدة، والحظر", Icons.Filled.People, Color(0xFF2563EB)),
    CHAT_CONTROL("الرقابة والتحكم بالمحادثات", "مراقبة رسائل الرحلات والتحكم بها", Icons.Filled.Forum, Color(0xFF7C3AED)),
    TOPUP_REQUESTS("طلبات الشحن وشام كاش", "مراجعة إشعارات الدفع والاعتماد", Icons.Filled.Payments, Color(0xFF059669)),
    RIDES_MANAGEMENT("إدارة الرحلات المنشورة", "تعديل، إلغاء، وإدارة مقاعد الرحلات", Icons.Filled.DirectionsCar, Color(0xFFD97706)),
    REQUESTED_TRIPS("طلبات الركاب المعلقة", "استعراض وتعديل طلبات الرحلات", Icons.Filled.PinDrop, Color(0xFF0284C7)),
    BROADCAST("الإشعارات والبث الجماعي", "إرسال تنبيهات لجميع المستخدمين", Icons.Filled.Campaign, Color(0xFFEA580C)),
    BRANDING_SETTINGS("الهوية السحابية والإعدادات", "تغيير اسم التطبيق والشعار ونظام العمل", Icons.Filled.SettingsSuggest, Color(0xFF4F46E5)),
    FINANCIAL_LEDGER("السجل والتدقيق المالي", "تتبع كشف حساب الحركات والعمولات", Icons.Filled.AccountBalance, Color(0xFF0D9488)),
    SUPPORT_TICKETS("مركز الدعم الفني", "معالجة شكاوى واستفسارات المستخدمين", Icons.Filled.SupportAgent, Color(0xFFDC2626)),
    SECURITY_LOGS("سجلات الأمان والنشاطات", "مراقبة عمليات النظام وتسجيلات الدخول", Icons.Filled.Security, Color(0xFF475569))
}

@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    topUpRequests: List<TopUpRequestEntity>,
    allUsers: List<UserEntity>,
    allRides: List<RideEntity>,
    language: AppLanguage,
    onApproveRequest: (requestId: String) -> Unit,
    onRejectRequest: (requestId: String, reason: String) -> Unit,
    onLogoutAdmin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSection by remember { mutableStateOf<AdminSection?>(null) }

    // Dialog states
    var requestToReject by remember { mutableStateOf<TopUpRequestEntity?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }

    var userToSuspend by remember { mutableStateOf<UserEntity?>(null) }
    var suspendReasonText by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

    // Create User state
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserPhone by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf("راكب وسائق") }
    var newUserPoints by remember { mutableStateOf("50") }
    var newUserIsVerified by remember { mutableStateOf(true) }

    var userToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var editUserName by remember { mutableStateOf("") }
    var editUserPhone by remember { mutableStateOf("") }
    var editUserRole by remember { mutableStateOf("PASSENGER") }
    var editUserPoints by remember { mutableStateOf("50") }

    var userToAdjustWallet by remember { mutableStateOf<UserEntity?>(null) }
    var adjustPointsDelta by remember { mutableStateOf("") }
    var adjustPointsReason by remember { mutableStateOf("") }
    var isPointsAddition by remember { mutableStateOf(true) }

    var userToViewDetails by remember { mutableStateOf<UserEntity?>(null) }

    var messageToEdit by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var editMessageNewText by remember { mutableStateOf("") }

    var rideToCancel by remember { mutableStateOf<RideEntity?>(null) }
    var cancelRideReasonText by remember { mutableStateOf("") }
    var rideToDelete by remember { mutableStateOf<RideEntity?>(null) }

    var rideToEdit by remember { mutableStateOf<RideEntity?>(null) }
    var editRideStartCity by remember { mutableStateOf("") }
    var editRideEndCity by remember { mutableStateOf("") }
    var editRideDate by remember { mutableStateOf("") }
    var editRideTime by remember { mutableStateOf("") }
    var editRidePrice by remember { mutableStateOf("") }
    var editRideSeats by remember { mutableStateOf("") }

    var requestedTripToEdit by remember { mutableStateOf<RequestedTripEntity?>(null) }
    var editReqStartCity by remember { mutableStateOf("") }
    var editReqEndCity by remember { mutableStateOf("") }
    var editReqDate by remember { mutableStateOf("") }
    var editReqTime by remember { mutableStateOf("") }
    var editReqMen by remember { mutableStateOf("1") }
    var editReqWomen by remember { mutableStateOf("0") }
    var editReqChildren by remember { mutableStateOf("0") }

    var selectedTicketForReply by remember { mutableStateOf<SupportTicket?>(null) }
    var ticketReplyText by remember { mutableStateOf("") }

    // Remote dynamic configuration
    val currentAppName by viewModel.appName.collectAsState()
    val currentAppTagline by viewModel.appTagline.collectAsState()
    val currentAppLogoUrl by viewModel.appLogoUrl.collectAsState()
    val currentDynamicIconVariant by viewModel.dynamicIconVariant.collectAsState()
    val currentIsMaintenance by viewModel.isMaintenanceMode.collectAsState()

    var configAppName by remember(currentAppName) { mutableStateOf(currentAppName) }
    var configAppTagline by remember(currentAppTagline) { mutableStateOf(currentAppTagline) }
    var configAppLogoUrl by remember(currentAppLogoUrl) { mutableStateOf(currentAppLogoUrl) }
    var configIconVariant by remember(currentDynamicIconVariant) { mutableStateOf(currentDynamicIconVariant) }
    var configIsMaintenance by remember(currentIsMaintenance) { mutableStateOf(currentIsMaintenance) }

    // Push & Broadcast Notif states
    var broadcastTitleText by remember { mutableStateOf("") }
    var broadcastBodyText by remember { mutableStateOf("") }
    var broadcastAudience by remember { mutableStateOf("ALL") }
    var broadcastType by remember { mutableStateOf("SYSTEM") }

    // Settings states from VM
    val publishCost by viewModel.ridePublishCost.collectAsState()
    val commissionPercent by viewModel.appCommissionPercent.collectAsState()
    val chatEnabled by viewModel.featureChatEnabled.collectAsState()
    val ratingsEnabled by viewModel.featureRatingsEnabled.collectAsState()
    val womenOnlyEnabled by viewModel.featureWomenOnlyEnabled.collectAsState()
    val currentShamCashAccount by viewModel.shamCashAccount.collectAsState()
    val currentAppDownloadUrl by viewModel.appDownloadUrl.collectAsState()

    var shamCashInput by remember(currentShamCashAccount) { mutableStateOf(currentShamCashAccount) }
    var appDownloadUrlInput by remember(currentAppDownloadUrl) { mutableStateOf(currentAppDownloadUrl) }

    val activityLogs by viewModel.adminActivityLogs.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val requestedTrips by viewModel.requestedTrips.collectAsState()
    val allWalletTransactions by viewModel.allWalletTransactions.collectAsState()
    val allChatMessages by viewModel.allChatMessages.collectAsState()

    var previewReceiptUri by remember { mutableStateOf<String?>(null) }
    var selectedRideChatRoom by remember { mutableStateOf<String?>(null) }

    val totalRevenue = topUpRequests.filter { it.status == RequestStatus.APPROVED.name }.sumOf { it.packagePriceUsd }
    val totalPointsSold = topUpRequests.filter { it.status == RequestStatus.APPROVED.name }.sumOf { it.packagePoints }
    val pendingTopupsCount = topUpRequests.count { it.status == RequestStatus.PENDING.name }
    val openTicketsCount = supportTickets.count { it.status == "OPEN" }

    // Check inactivity timeout
    LaunchedEffect(Unit) {
        if (viewModel.checkAdminSessionTimeout()) {
            Toast.makeText(context, "انتهت جلسة الأدمن بعد ساعة من عدم النشاط", Toast.LENGTH_LONG).show()
            onLogoutAdmin()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentSection,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "admin_navigation"
        ) { section ->
            if (section == null) {
                // MAIN ADMIN HUB SCREEN
                AdminMainHub(
                    allUsersCount = allUsers.size,
                    allRidesCount = allRides.size,
                    chatMessagesCount = allChatMessages.size,
                    topUpRequestsCount = topUpRequests.size,
                    pendingTopupsCount = pendingTopupsCount,
                    requestedTripsCount = requestedTrips.size,
                    openTicketsCount = openTicketsCount,
                    totalRevenue = totalRevenue,
                    totalPointsSold = totalPointsSold,
                    onSelectSection = { currentSection = it },
                    onLogoutAdmin = {
                        viewModel.logoutAdmin()
                        onLogoutAdmin()
                        Toast.makeText(context, "تم تسجيل خروج المشرف", Toast.LENGTH_SHORT).show()
                    },
                    onBack = onBack
                )
            } else {
                // DEDICATED FULL SUB-PAGE FOR THE SELECTED SECTION
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 14.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sub-Page Header with Back Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    if (selectedRideChatRoom != null) {
                                        selectedRideChatRoom = null
                                    } else {
                                        currentSection = null
                                    }
                                },
                                shape = CircleShape
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة للوحة الأدمن")
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(section.icon, contentDescription = null, tint = section.accentColor, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = section.titleAr,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = section.subtitleAr,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = section.accentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Super Admin",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = section.accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated Sub-Page Content
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (section) {
                            AdminSection.USERS -> {
                                AdminUsersSubPage(
                                    allUsers = allUsers,
                                    onCreateUser = {
                                        newUserName = ""
                                        newUserPhone = ""
                                        newUserEmail = ""
                                        newUserRole = "راكب وسائق"
                                        newUserPoints = "50"
                                        newUserIsVerified = true
                                        showCreateUserDialog = true
                                    },
                                    onChatWithUser = { u ->
                                        viewModel.startDirectChatWithUser(u)
                                        Toast.makeText(context, "بدء محادثة مع: ${u.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    onAdjustWallet = { u ->
                                        userToAdjustWallet = u
                                        adjustPointsDelta = ""
                                        adjustPointsReason = ""
                                        isPointsAddition = true
                                    },
                                    onEditUser = { u ->
                                        userToEdit = u
                                        editUserName = u.name
                                        editUserPhone = u.phone
                                        editUserRole = u.userRole
                                        editUserPoints = u.walletPoints.toString()
                                    },
                                    onViewDetails = { u -> userToViewDetails = u },
                                    onSuspend = { u -> userToSuspend = u },
                                    onReactivate = { u ->
                                        viewModel.reactivateUser(u.id)
                                        Toast.makeText(context, "تمت إعادة تفعيل حساب ${u.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteUser = { u -> userToDelete = u }
                                )
                            }

                            AdminSection.CHAT_CONTROL -> {
                                AdminChatControlSubPage(
                                    allRides = allRides,
                                    allChatMessages = allChatMessages,
                                    selectedRideChatRoom = selectedRideChatRoom,
                                    onSelectRideRoom = { selectedRideChatRoom = it },
                                    onDeleteRoom = { rideId ->
                                        viewModel.deleteChatConversation(rideId)
                                        Toast.makeText(context, "تم حذف وتفريغ المحادثة بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            AdminSection.TOPUP_REQUESTS -> {
                                AdminTopUpSubPage(
                                    topUpRequests = topUpRequests,
                                    onPreviewReceipt = { previewReceiptUri = it },
                                    onApprove = { onApproveRequest(it) },
                                    onReject = { requestToReject = it }
                                )
                            }

                            AdminSection.RIDES_MANAGEMENT -> {
                                AdminRidesSubPage(
                                    allRides = allRides,
                                    onEditRide = { r ->
                                        rideToEdit = r
                                        editRideStartCity = r.startCity
                                        editRideEndCity = r.endCity
                                        editRideDate = r.departureDate
                                        editRideTime = r.departureTime
                                        editRidePrice = r.pricePerSeat.toString()
                                        editRideSeats = r.availableSeats.toString()
                                    },
                                    onDeleteRide = { r -> rideToDelete = r },
                                    onCancelRide = { r -> rideToCancel = r }
                                )
                            }

                            AdminSection.REQUESTED_TRIPS -> {
                                AdminRequestedTripsSubPage(
                                    requestedTrips = requestedTrips,
                                    onEditTrip = { req ->
                                        requestedTripToEdit = req
                                        editReqStartCity = req.startCity
                                        editReqEndCity = req.endCity
                                        editReqDate = req.departureDate
                                        editReqTime = req.departureTime
                                        editReqMen = req.menCount.toString()
                                        editReqWomen = req.womenCount.toString()
                                        editReqChildren = req.childrenCount.toString()
                                    },
                                    onReopenTrip = { reqId ->
                                        viewModel.adminReopenRequestedTrip(reqId)
                                        Toast.makeText(context, "تمت إعادة فتح طلب الرحلة للسائقين", Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteTrip = { reqId ->
                                        viewModel.deleteRequestedTrip(reqId)
                                        Toast.makeText(context, "تم حذف طلب الرحلة", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            AdminSection.BROADCAST -> {
                                AdminBroadcastSubPage(
                                    title = broadcastTitleText,
                                    onTitleChange = { broadcastTitleText = it },
                                    body = broadcastBodyText,
                                    onBodyChange = { broadcastBodyText = it },
                                    audience = broadcastAudience,
                                    onAudienceChange = { broadcastAudience = it },
                                    type = broadcastType,
                                    onTypeChange = { broadcastType = it },
                                    onSend = {
                                        if (broadcastTitleText.isNotBlank() && broadcastBodyText.isNotBlank()) {
                                            viewModel.sendBroadcastNotification(broadcastTitleText, broadcastBodyText, broadcastAudience, broadcastType)
                                            broadcastTitleText = ""
                                            broadcastBodyText = ""
                                            Toast.makeText(context, "تم إرسال الإشعار الجماعي بنجاح", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "يرجى تعبئة عنوان ونص الإشعار", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            AdminSection.BRANDING_SETTINGS -> {
                                AdminBrandingSubPage(
                                    appName = configAppName,
                                    onAppNameChange = { configAppName = it },
                                    appTagline = configAppTagline,
                                    onAppTaglineChange = { configAppTagline = it },
                                    appLogoUrl = configAppLogoUrl,
                                    onAppLogoUrlChange = { configAppLogoUrl = it },
                                    iconVariant = configIconVariant,
                                    onIconVariantChange = { configIconVariant = it },
                                    isMaintenance = configIsMaintenance,
                                    onToggleMaintenance = { configIsMaintenance = it },
                                    shamCashAccount = shamCashInput,
                                    onShamCashChange = { shamCashInput = it },
                                    appDownloadUrl = appDownloadUrlInput,
                                    onDownloadUrlChange = { appDownloadUrlInput = it },
                                    onSave = {
                                        viewModel.updateRemoteAppConfig(configAppName, configAppTagline, configAppLogoUrl, configIconVariant, configIsMaintenance)
                                        viewModel.updateShamCashAccount(shamCashInput)
                                        viewModel.updateAppDownloadUrl(appDownloadUrlInput)
                                        Toast.makeText(context, "تم حفظ وتطبيق التعديلات السحابية بنجاح", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            AdminSection.FINANCIAL_LEDGER -> {
                                AdminFinancialLedgerSubPage(
                                    transactions = allWalletTransactions,
                                    totalRevenue = totalRevenue,
                                    totalPointsSold = totalPointsSold
                                )
                            }

                            AdminSection.SUPPORT_TICKETS -> {
                                AdminSupportTicketsSubPage(
                                    tickets = supportTickets,
                                    onReplyClick = { t ->
                                        selectedTicketForReply = t
                                        ticketReplyText = t.adminReply ?: ""
                                    }
                                )
                            }

                            AdminSection.SECURITY_LOGS -> {
                                AdminSecurityLogsSubPage(activityLogs = activityLogs)
                            }
                        }
                    }
                }
            }
        }

        // ================= DIALOGS =================
        // Create New User Dialog (Admin)
        if (showCreateUserDialog) {
            AlertDialog(
                onDismissRequest = { showCreateUserDialog = false },
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = PrimaryGreen) },
                title = { Text("إنشاء مستخدم جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "أدخل بيانات الحساب الجديد ليتم تسجيله في النظام وتفعيل محفظته مباشرة:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = newUserName,
                            onValueChange = { newUserName = it },
                            label = { Text("اسم المستخدم الكامل *") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("new_user_name_input")
                        )

                        OutlinedTextField(
                            value = newUserPhone,
                            onValueChange = { newUserPhone = it },
                            label = { Text("رقم الهاتف (مثال: 0988123456) *") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("new_user_phone_input")
                        )

                        OutlinedTextField(
                            value = newUserEmail,
                            onValueChange = { newUserEmail = it },
                            label = { Text("البريد الإلكتروني (اختياري)") },
                            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("new_user_email_input")
                        )

                        Text("نوع الحساب / الصلاحية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("راكب وسائق", "سائق", "راكب").forEach { role ->
                                FilterChip(
                                    selected = newUserRole == role,
                                    onClick = { newUserRole = role },
                                    label = { Text(role, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newUserPoints,
                            onValueChange = { newUserPoints = it },
                            label = { Text("الرصيد الأولي للنقاط في المحفظة") },
                            leadingIcon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = PrimaryGreen) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("new_user_points_input")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newUserIsVerified = !newUserIsVerified }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                Text("توثيق الحساب فورياً (شارة موثق)", fontSize = 13.sp)
                            }
                            Switch(
                                checked = newUserIsVerified,
                                onCheckedChange = { newUserIsVerified = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newUserName.isBlank()) {
                                Toast.makeText(context, "يرجى إدخال اسم المستخدم", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (newUserPhone.isBlank()) {
                                Toast.makeText(context, "يرجى إدخال رقم الهاتف", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val normPhone = normalizeSyrianPhoneNumber(newUserPhone)
                            val points = newUserPoints.toIntOrNull() ?: 50
                            viewModel.adminCreateUser(
                                name = newUserName.trim(),
                                email = newUserEmail.trim(),
                                phone = normPhone,
                                role = newUserRole,
                                initialPoints = points,
                                isVerified = newUserIsVerified
                            ) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showCreateUserDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_create_user_btn")
                    ) {
                        Text("إنشاء الحساب", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateUserDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // User Delete Confirmation Dialog
        userToDelete?.let { user ->
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("حذف المستخدم نهائياً", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("هل أنت متأكد من رغبتك في حذف حساب (${user.name}) نهائياً من النظام؟")
                        Text("الهاتف: ${user.phone} • الرصيد: ${user.walletPoints} نقطة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("تحذير: لا يمكن التراجع عن هذا الإجراء وسيتم مسح كافة سجلات الحساب.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.adminDeleteUser(user.id)
                            userToDelete = null
                            Toast.makeText(context, "تم حذف المستخدم نهائياً بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد الحذف النهائي", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) { Text("إلغاء") }
                }
            )
        }

        // User Suspend Dialog
        userToSuspend?.let { user ->
            AlertDialog(
                onDismissRequest = { userToSuspend = null },
                title = { Text("تعليق حساب المستخدم: ${user.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("يرجى كتابة سبب تعليق الحساب لتوضيحه للمستخدم والتوثيق:")
                        OutlinedTextField(
                            value = suspendReasonText,
                            onValueChange = { suspendReasonText = it },
                            placeholder = { Text("مثال: مخالفة شروط الاستخدام أو الإلغاء المتكرر") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.suspendUser(user.id, suspendReasonText)
                            userToSuspend = null
                            suspendReasonText = ""
                            Toast.makeText(context, "تم تعليق الحساب وإرسال الإشعار", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد التعليق", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToSuspend = null }) { Text("إلغاء") }
                }
            )
        }

        // Adjust Wallet Dialog
        userToAdjustWallet?.let { user ->
            AlertDialog(
                onDismissRequest = { userToAdjustWallet = null },
                title = { Text("تعديل رصيد محفظة: ${user.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("الرصيد الحالي: ${user.walletPoints} نقطة", fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isPointsAddition,
                                onClick = { isPointsAddition = true },
                                label = { Text("+ إضافة نقاط (إيداع)") }
                            )
                            FilterChip(
                                selected = !isPointsAddition,
                                onClick = { isPointsAddition = false },
                                label = { Text("- خصم نقاط") }
                            )
                        }
                        OutlinedTextField(
                            value = adjustPointsDelta,
                            onValueChange = { adjustPointsDelta = it },
                            label = { Text("عدد النقاط") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = adjustPointsReason,
                            onValueChange = { adjustPointsReason = it },
                            label = { Text("سبب العملية (يظهر في سجل المحفظة)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pts = adjustPointsDelta.toIntOrNull() ?: 0
                            val reason = adjustPointsReason.trim()

                            if (pts <= 0) {
                                Toast.makeText(
                                    context,
                                    "أدخل عدد نقاط صحيح أكبر من صفر",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (reason.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "اكتب سبب العملية",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val delta =
                                    if (isPointsAddition) pts else -pts

                                viewModel.adminAdjustUserWallet(
                                    user.id,
                                    delta,
                                    reason
                                ) { success, error ->

                                    if (success) {
                                        userToAdjustWallet = null
                                        adjustPointsDelta = ""
                                        adjustPointsReason = ""

                                        Toast.makeText(
                                            context,
                                            "تم تعديل الرصيد بنجاح",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    } else {
                                        Toast.makeText(
                                            context,
                                            error ?: "فشل تعديل الرصيد",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("تطبيق التعديل", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToAdjustWallet = null }) { Text("إلغاء") }
                }
            )
        }

        // Edit User Dialog
        userToEdit?.let { user ->
            AlertDialog(
                onDismissRequest = { userToEdit = null },
                title = { Text("تعديل بيانات المستخدم", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editUserName, onValueChange = { editUserName = it }, label = { Text("الاسم الكامل") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editUserPhone, onValueChange = { editUserPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = editUserPoints, onValueChange = { editUserPoints = it }, label = { Text("نقاط المحفظة") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = editUserRole == "PASSENGER", onClick = { editUserRole = "PASSENGER" }, label = { Text("راكب") })
                            FilterChip(selected = editUserRole == "DRIVER", onClick = { editUserRole = "DRIVER" }, label = { Text("سائق") })
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.adminUpdateUserData(user.id, editUserName, editUserPhone, editUserRole, editUserPoints.toIntOrNull() ?: user.walletPoints)
                            userToEdit = null
                            Toast.makeText(context, "تم تحديث بيانات المستخدم", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("حفظ التعديلات", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToEdit = null }) { Text("إلغاء") }
                }
            )
        }

        // User Details Dialog
        userToViewDetails?.let { user ->
            AlertDialog(
                onDismissRequest = { userToViewDetails = null },
                title = { Text("تفاصيل المستخدم: ${user.name}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("المعرف: ${user.id}", fontSize = 11.sp, color = Color.Gray)
                        Text("البريد: ${user.email}", fontSize = 12.sp)
                        Text("الهاتف: ${user.phone}", fontSize = 12.sp)
                        Text("الدور: ${user.userRole}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("التقييم: ${user.rating} ⭐", fontSize = 12.sp)
                        Text("عدد الرحلات: ${user.rideCount}", fontSize = 12.sp)
                        Text("نقاط المحفظة: ${user.walletPoints} نقطة", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                        Text("كود الإحالة: ${user.referralCode}", fontSize = 12.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Text("حالة الحساب: ${if (user.isSuspended) "معلق (${user.suspendReason ?: ""})" else "نشط"}", fontSize = 12.sp, color = if (user.isSuspended) ErrorRed else PrimaryGreen)
                    }
                },
                confirmButton = {
                    Button(onClick = { userToViewDetails = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                        Text("إغلاق", color = Color.White)
                    }
                }
            )
        }

        // Reject TopUp Dialog
        requestToReject?.let { req ->
            AlertDialog(
                onDismissRequest = { requestToReject = null },
                title = { Text("رفض طلب الشحن", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("طلب شحن باقة ${req.packagePoints} نقطة للمستخدم ${req.userName}")
                        OutlinedTextField(
                            value = rejectionReasonText,
                            onValueChange = { rejectionReasonText = it },
                            placeholder = { Text("سبب الرفض (مثال: إشعار غير واضح أو رقم الحوالة غير صحيح)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onRejectRequest(req.id, rejectionReasonText)
                            requestToReject = null
                            rejectionReasonText = ""
                            Toast.makeText(context, "تم رفض الطلب وإشعار المستخدم", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد الرفض", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { requestToReject = null }) { Text("إلغاء") }
                }
            )
        }

        // Preview Receipt Image Modal
        previewReceiptUri?.let { uri ->
            AlertDialog(
                onDismissRequest = { previewReceiptUri = null },
                title = { Text("معاينة إشعار الدفع", fontWeight = FontWeight.Bold) },
                text = {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Receipt Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                },
                confirmButton = {
                    Button(onClick = { previewReceiptUri = null }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                        Text("إغلاق", color = Color.White)
                    }
                }
            )
        }

        // Edit Message Dialog
        messageToEdit?.let { msg ->
            AlertDialog(
                onDismissRequest = { messageToEdit = null },
                title = { Text("تعديل نص الرسالة كأدمن", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editMessageNewText,
                        onValueChange = { editMessageNewText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.editChatMessage(msg.rideId, msg.id, editMessageNewText)
                            messageToEdit = null
                            Toast.makeText(context, "تم تعديل الرسالة بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("تحديث الرسالة", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToEdit = null }) { Text("إلغاء") }
                }
            )
        }

        // Edit Ride Dialog
        rideToEdit?.let { ride ->
            AlertDialog(
                onDismissRequest = { rideToEdit = null },
                title = { Text("تعديل بيانات الرحلة", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = editRideStartCity, onValueChange = { editRideStartCity = it }, label = { Text("من") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editRideEndCity, onValueChange = { editRideEndCity = it }, label = { Text("إلى") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = editRideDate, onValueChange = { editRideDate = it }, label = { Text("التاريخ") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editRideTime, onValueChange = { editRideTime = it }, label = { Text("الوقت") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = editRidePrice, onValueChange = { editRidePrice = it }, label = { Text("السعر ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = editRideSeats, onValueChange = { editRideSeats = it }, label = { Text("المقاعد") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.adminEditRide(
                                ride.id,
                                editRideStartCity,
                                editRideEndCity,
                                editRideDate,
                                editRideTime,
                                editRidePrice.toDoubleOrNull() ?: ride.pricePerSeat,
                                editRideSeats.toIntOrNull() ?: ride.availableSeats
                            )
                            rideToEdit = null
                            Toast.makeText(context, "تم حفظ تعديلات الرحلة", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("حفظ التعديل", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rideToEdit = null }) { Text("إلغاء") }
                }
            )
        }

        // Cancel Ride Dialog
        rideToCancel?.let { ride ->
            AlertDialog(
                onDismissRequest = { rideToCancel = null },
                title = { Text("إلغاء الرحلة كمدير نظام", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("رحلة: ${ride.startCity} ➔ ${ride.endCity} (السائق: ${ride.driverName})")
                        OutlinedTextField(
                            value = cancelRideReasonText,
                            onValueChange = { cancelRideReasonText = it },
                            placeholder = { Text("سبب الإلغاء لتضمينه في الإشعار...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.cancelRideByAdmin(ride.id, cancelRideReasonText)
                            rideToCancel = null
                            cancelRideReasonText = ""
                            Toast.makeText(context, "تم إلغاء وحذف الرحلة وإشعار السائق", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد الإلغاء", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rideToCancel = null }) { Text("تراجع") }
                }
            )
        }

        // Delete Ride Permanently Dialog
        rideToDelete?.let { ride ->
            AlertDialog(
                onDismissRequest = { rideToDelete = null },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("حذف الرحلة نهائياً", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        "هل أنت متأكد من حذف رحلة (${ride.startCity} ➔ ${ride.endCity}) للسائق ${ride.driverName} نهائياً؟ سيتم مسحها بالكامل من التطبيق وقاعدة البيانات.",
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.adminDeleteRide(ride.id)
                            rideToDelete = null
                            Toast.makeText(context, "تم حذف الرحلة نهائياً من النظام", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("تأكيد الحذف", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rideToDelete = null }) { Text("إلغاء") }
                }
            )
        }

        // Support Ticket Reply Dialog
        selectedTicketForReply?.let { ticket ->
            AlertDialog(
                onDismissRequest = { selectedTicketForReply = null },
                title = { Text("الرد على تذكرة الدعم: ${ticket.subject}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("المستخدم: ${ticket.userName} (${ticket.userEmail})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("نص المشكلة: ${ticket.messageText}", fontSize = 13.sp)
                        OutlinedTextField(
                            value = ticketReplyText,
                            onValueChange = { ticketReplyText = it },
                            label = { Text("نص رد المشرف وحل المشكلة") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ticketReplyText.isNotBlank()) {
                                viewModel.replySupportTicket(ticket.id, ticketReplyText)
                                selectedTicketForReply = null
                                Toast.makeText(context, "تم الرد وإغلاق التذكرة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("إرسال الرد والإغلاق", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedTicketForReply = null }) { Text("إلغاء") }
                }
            )
        }
    }
}

// ================= MAIN HUB COMPOSABLE =================
@Composable
private fun AdminMainHub(
    allUsersCount: Int,
    allRidesCount: Int,
    chatMessagesCount: Int,
    topUpRequestsCount: Int,
    pendingTopupsCount: Int,
    requestedTripsCount: Int,
    openTicketsCount: Int,
    totalRevenue: Double,
    totalPointsSold: Int,
    onSelectSection: (AdminSection) -> Unit,
    onLogoutAdmin: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة")
                    }
                    Column {
                        Text("مركز تحكم المشغل الرئيسي", fontSize = 17.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text("Super Admin Dashboard", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(13.dp))
                            Text("أعلى صلاحية", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                        }
                    }

                    IconButton(
                        onClick = onLogoutAdmin,
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = "خروج المشرف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Key Metrics Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                            Text("الإيرادات المعتمدة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$${String.format(java.util.Locale.US, "%.2f", totalRevenue)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = PrimaryGreen)
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Stars, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                            Text("النقاط المباعة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$totalPointsSold pts", fontSize = 15.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.PendingActions, contentDescription = null, tint = if (pendingTopupsCount > 0) Color(0xFFD97706) else PrimaryGreen, modifier = Modifier.size(14.dp))
                            Text("شحن معلق", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$pendingTopupsCount طلب", fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (pendingTopupsCount > 0) Color(0xFFD97706) else PrimaryGreen)
                    }
                }
            }
        }

        item {
            Text(
                text = "أقسام التحكم الشامل (اختر قسماً لفتحه في صفحة مستقلة):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Functional Section Tiles (Clean, regular structured cards)
        items(AdminSection.values()) { section ->
            val countBadge = when (section) {
                AdminSection.USERS -> "$allUsersCount مستخدم"
                AdminSection.CHAT_CONTROL -> "$chatMessagesCount رسالة"
                AdminSection.TOPUP_REQUESTS -> if (pendingTopupsCount > 0) "$pendingTopupsCount معلق!" else "$topUpRequestsCount طلب"
                AdminSection.RIDES_MANAGEMENT -> "$allRidesCount رحلة"
                AdminSection.REQUESTED_TRIPS -> "$requestedTripsCount طلب"
                AdminSection.SUPPORT_TICKETS -> if (openTicketsCount > 0) "$openTicketsCount مفتوحة" else "مكتمل"
                else -> null
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSection(section) }
                    .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = section.accentColor.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(section.accentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(section.icon, contentDescription = null, tint = section.accentColor, modifier = Modifier.size(24.dp))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = section.titleAr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = section.subtitleAr,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (countBadge != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = section.accentColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = countBadge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = section.accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ================= DEDICATED SUB-PAGE COMPOSABLES =================

// 1. Users Sub-Page
@Composable
private fun AdminUsersSubPage(
    allUsers: List<UserEntity>,
    onCreateUser: () -> Unit,
    onChatWithUser: (UserEntity) -> Unit,
    onAdjustWallet: (UserEntity) -> Unit,
    onEditUser: (UserEntity) -> Unit,
    onViewDetails: (UserEntity) -> Unit,
    onSuspend: (UserEntity) -> Unit,
    onReactivate: (UserEntity) -> Unit,
    onDeleteUser: (UserEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = allUsers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true) ||
        it.phone.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن مستخدم...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("admin_user_search")
            )

            Button(
                onClick = onCreateUser,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.testTag("admin_add_user_btn")
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مستخدم جديد", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(filteredUsers) { u ->
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!u.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = u.avatarUrl,
                                        contentDescription = u.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryGreen)
                                    }
                                }
                                Column {
                                    Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${u.phone} • ${u.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(color = GoldAccent.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                            Text("محفظة: ${u.walletPoints} نقطة", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                        Text("${u.userRole} • ${u.rating}⭐", fontSize = 10.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (u.isSuspended) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = if (u.isSuspended) "معلق" else "نشط",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (u.isSuspended) Color(0xFF991B1B) else Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Action Buttons Row (Clear & spacious)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onChatWithUser(u) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.2f).testTag("chat_user_btn_${u.id}")
                            ) {
                                Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("محادثة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onAdjustWallet(u) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("الرصيد", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { onEditUser(u) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("تعديل", fontSize = 11.sp)
                            }

                            IconButton(onClick = { onViewDetails(u) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Info, contentDescription = "Details", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            }

                            if (u.isSuspended) {
                                IconButton(onClick = { onReactivate(u) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Activate", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                }
                            } else {
                                IconButton(onClick = { onSuspend(u) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Filled.Block, contentDescription = "Suspend", tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                }
                            }

                            IconButton(
                                onClick = { onDeleteUser(u) },
                                modifier = Modifier.size(32.dp).testTag("delete_user_btn_${u.id}")
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = "حذف المستخدم", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 2. Chat Control & Monitoring Sub-Page (Read-Only monitoring as requested)
@Composable
private fun AdminChatControlSubPage(
    allRides: List<RideEntity>,
    allChatMessages: List<ChatMessageEntity>,
    selectedRideChatRoom: String?,
    onSelectRideRoom: (String?) -> Unit,
    onDeleteRoom: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var roomToDelete by remember { mutableStateOf<RideEntity?>(null) }
    var playingAudioId by remember { mutableStateOf<String?>(null) }

    val activeRide = allRides.find { it.id == selectedRideChatRoom }

    // Dialog for confirming conversation deletion in Admin panel
    if (roomToDelete != null) {
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("حذف وتفريغ المحادثة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "هل أنت متأكد من مسح جميع رسائل محادثة (${roomToDelete?.startCity} ➔ ${roomToDelete?.endCity}) نهائياً من النظام وقائمة المحادثات؟",
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        roomToDelete?.let { r ->
                            onDeleteRoom(r.id)
                            if (selectedRideChatRoom == r.id) {
                                onSelectRideRoom(null)
                            }
                        }
                        roomToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) {
                    Text("إلغاء", fontSize = 12.sp)
                }
            }
        )
    }

    if (selectedRideChatRoom == null) {
        // List of all conversations with Search
        val filteredRides = allRides.filter { ride ->
            ride.startCity.contains(searchQuery, ignoreCase = true) ||
            ride.endCity.contains(searchQuery, ignoreCase = true) ||
            ride.driverName.contains(searchQuery, ignoreCase = true)
        }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث في المحادثات بالمدينة أو اسم المستخدم...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryGreen) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Info Card about Read-Only Monitoring
            Surface(
                color = PrimaryGreen.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = DarkGreen, modifier = Modifier.size(18.dp))
                    Text(
                        text = "اضغط على أي محادثة لمراقبتها وقراءة محتواها وسجل رسائلها بالكامل.",
                        fontSize = 11.5.sp,
                        color = DarkGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (filteredRides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد محادثات مطابقة للبحث", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredRides, key = { it.id }) { ride ->
                        val rideMsgs = allChatMessages.filter { it.rideId == ride.id }.sortedBy { it.timestamp }
                        val lastMsg = rideMsgs.lastOrNull()
                        val lastMsgText = when {
                            lastMsg == null -> "محادثة جاهزة للتواصل"
                            lastMsg.audioUri != null -> "🎙️ تسجيل صوتي (${lastMsg.audioDurationSeconds} ث)"
                            lastMsg.imageUri != null -> "📷 صورة مرفقة"
                            else -> lastMsg.messageText
                        }

                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val lastTimeStr = if (lastMsg != null) timeFormat.format(Date(lastMsg.timestamp)) else ride.departureDate

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectRideRoom(ride.id) },
                            cornerRadius = 14.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (!ride.driverAvatar.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ride.driverAvatar,
                                                contentDescription = ride.driverName,
                                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = if (ride.id.startsWith("chat_user_")) "محادثة مباشرة: ${ride.driverName}" else "${ride.startCity} ➔ ${ride.endCity}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "السائق / الطرف: ${ride.driverName} • $lastTimeStr",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (rideMsgs.isNotEmpty()) PrimaryGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${rideMsgs.size} رسائل",
                                            fontSize = 10.sp,
                                            color = if (rideMsgs.isNotEmpty()) PrimaryGreen else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lastMsgText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { onSelectRideRoom(ride.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("قراءة المحتوى", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = { roomToDelete = ride },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "حذف المحادثة", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // =========================================================================
        // READ-ONLY CHAT VIEWER (Only reading conversation content as requested)
        // =========================================================================
        val currentRoomMsgs = allChatMessages.filter { it.rideId == selectedRideChatRoom }.sortedBy { it.timestamp }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Room Top Header with Read-Only Badge
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onSelectRideRoom(null) },
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = if (activeRide?.id?.startsWith("chat_user_") == true) "محادثة: ${activeRide.driverName}" else "${activeRide?.startCity} ➔ ${activeRide?.endCity}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "السائق: ${activeRide?.driverName ?: "مستخدم"} • تاريخ الرحلة: ${activeRide?.departureDate ?: ""}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = WarningAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(13.dp))
                            Text("وضع القراءة والرقابة فقط", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                        }
                    }
                }
            }

            // Message List Box
            if (currentRoomMsgs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Text("لا توجد رسائل مسجلة بعد في هذه المحادثة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentRoomMsgs, key = { it.id }) { msg ->
                        val isSystem = msg.senderId == "system"
                        val isAdmin = msg.senderId == "admin" || msg.senderId == "super_admin"
                        val isDriver = msg.senderId == activeRide?.driverId
                        val isAudio = !msg.audioUri.isNullOrBlank() || msg.audioDurationSeconds > 0

                        val timeFormat = SimpleDateFormat("h:mm a • dd MMM", Locale.getDefault())
                        val timeStr = timeFormat.format(Date(msg.timestamp))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 10.dp
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            when {
                                                isAdmin -> Icons.Filled.AdminPanelSettings
                                                isSystem -> Icons.Filled.Info
                                                isDriver -> Icons.Filled.DirectionsCar
                                                else -> Icons.Filled.Person
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                isAdmin -> GoldAccent
                                                isSystem -> Color.Blue
                                                isDriver -> PrimaryGreen
                                                else -> TrueBlue
                                            },
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = when {
                                                isAdmin -> "مدير النظام (Admin)"
                                                isSystem -> "تنبيه تلقائي من النظام"
                                                isDriver -> "السائق: ${activeRide?.driverName}"
                                                else -> "الطرف الآخر / الراكب (ID: ${msg.senderId})"
                                            },
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isAdmin -> PrimaryGreen
                                                isDriver -> DarkGreen
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }

                                    Text(
                                        text = timeStr,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isAudio) {
                                    val isPlaying = playingAudioId == msg.id
                                    val duration = msg.audioDurationSeconds.takeIf { it > 0 } ?: 8

                                    Surface(
                                        color = PrimaryGreen.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    playingAudioId = if (isPlaying) null else msg.id
                                                    if (!isPlaying) {
                                                        Toast.makeText(context, "تشغيل التسجيل الصوتي ($duration ثانية)", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp).background(PrimaryGreen, CircleShape)
                                            ) {
                                                Icon(
                                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "تشغيل التسجيل",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("تسجيل صوتي 🎙️ ($duration ثانية)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                                                Text(if (isPlaying) "جاري الاستماع..." else "انقر للاستماع إلى التسجيل الصوتي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                if (!msg.imageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = msg.imageUri,
                                        contentDescription = "Message Image",
                                        modifier = Modifier.size(160.dp, 100.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                if (msg.messageText.isNotBlank() && (!isAudio || msg.messageText != "تسجيل صوتي (${msg.audioDurationSeconds} ثانية)")) {
                                    Text(
                                        text = msg.messageText,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Informational Read-Only Footer Banner (Explicitly confirming no other actions can be done here)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "هذا القسم مخصص للرقابة وقراءة محتوى المحادثات فقط دون تعديل أو إرسال.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// 3. TopUp Sub-Page
@Composable
private fun AdminTopUpSubPage(
    topUpRequests: List<TopUpRequestEntity>,
    onPreviewReceipt: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (TopUpRequestEntity) -> Unit
) {
    if (topUpRequests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد طلبات شحن حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(topUpRequests) { req ->
                val isPending = req.status == RequestStatus.PENDING.name
                val isApproved = req.status == RequestStatus.APPROVED.name

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(req.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("باقة ${req.packagePoints} نقطة • $${req.packagePriceUsd}", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPending) Color(0xFFFEF3C7) else if (isApproved) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (isPending) "قيد المراجعة" else if (isApproved) "مكتمل" else "مرفوض",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isPending) Color(0xFFD97706) else if (isApproved) Color(0xFF065F46) else Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onPreviewReceipt(req.receiptImagePath) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Image, contentDescription = null, tint = PrimaryGreen)
                                    Text("معاينة صورة إشعار شام كاش المرفق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = PrimaryGreen)
                            }
                        }

                        if (isPending) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApprove(req.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("موافقة وإضافة النقاط", fontSize = 12.sp, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = { onReject(req) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("رفض مع السبب", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. Rides Sub-Page
@Composable
private fun AdminRidesSubPage(
    allRides: List<RideEntity>,
    onEditRide: (RideEntity) -> Unit,
    onDeleteRide: (RideEntity) -> Unit,
    onCancelRide: (RideEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredRides = allRides.filter { ride ->
        ride.startCity.contains(searchQuery, ignoreCase = true) ||
        ride.endCity.contains(searchQuery, ignoreCase = true) ||
        ride.driverName.contains(searchQuery, ignoreCase = true) ||
        ride.carModel.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في الرحلات بالمدينة أو السائق أو نوع السيارة...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryGreen) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        if (filteredRides.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (allRides.isEmpty()) "لا توجد رحلات منشورة في النظام حالياً" else "لا توجد رحلات مطابقة للبحث",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredRides, key = { it.id }) { ride ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${ride.startCity} ➔ ${ride.endCity}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$${ride.pricePerSeat} / مقعد", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 13.sp)
                            }
                            Text("السائق: ${ride.driverName} • ${ride.departureDate} ${ride.departureTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("المقاعد المتبقية: ${ride.availableSeats} • السيارة: ${ride.carModel} (${ride.carPlate})", fontSize = 11.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onEditRide(ride) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("تعديل", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { onCancelRide(ride) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("إلغاء", fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { onDeleteRide(ride) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("حذف نهائي", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Requested Trips Sub-Page
@Composable
private fun AdminRequestedTripsSubPage(
    requestedTrips: List<RequestedTripEntity>,
    onEditTrip: (RequestedTripEntity) -> Unit,
    onReopenTrip: (String) -> Unit,
    onDeleteTrip: (String) -> Unit
) {
    if (requestedTrips.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد طلبات رحلات حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(requestedTrips) { req ->
                val isAccepted = req.status == "ACCEPTED"
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${req.startCity} ➔ ${req.endCity}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAccepted) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (isAccepted) "مقبول من سائق" else "مفتوح للسائقين",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAccepted) Color(0xFF065F46) else Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("الراكب: ${req.userName} (${req.userPhone}) • ${req.departureDate} ${req.departureTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("الركاب: ${req.menCount} رجال • ${req.womenCount} نساء • ${req.childrenCount} أطفال", fontSize = 11.sp)

                        if (isAccepted && req.acceptedByDriverName != null) {
                            Text("السائق المقبول: ${req.acceptedByDriverName}", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isAccepted) {
                                OutlinedButton(
                                    onClick = { onReopenTrip(req.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إعادة فتح للسائقين", fontSize = 10.sp)
                                }
                            }
                            Button(
                                onClick = { onDeleteTrip(req.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حذف الطلب", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. Broadcast Sub-Page
@Composable
private fun AdminBroadcastSubPage(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    audience: String,
    onAudienceChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val templates = listOf(
        Triple(
            "تحديث التطبيق 🚀",
            "تحديث جديد لتطبيق وسلني متوفر الآن!",
            "تم تحسين سرعة الحجز وإضافة ميزات جديدة لتسهيل رحلاتكم اليومية. نتمنى لكم تجربة مميزة!"
        ),
        Triple(
            "مكافأة شحن 🎁",
            "عرض خاص: نقاط إضافية على كل عملية شحن!",
            "اشحن محفظتك الآن عبر شام كاش واحصل على 20% نقاط إضافية مجانية لفترة محدودة."
        ),
        Triple(
            "تنبيه السائقين ⏱️",
            "تنبيه هام بخصوص مواعيد الانطلاق ونقاط التجمع",
            "نرجو من جميع الكباتن الالتزام بنقاط الانطلاق والمواعيد المحددة لضمان أعلى تقييم ورضا الركاب."
        ),
        Triple(
            "صيانة مجدولة 🛠️",
            "صيانة دورية لتحسين كفاءة النظام",
            "سيتم إجراء صيانة سريعة لتحسين أداء الخوادم لمدة 15 دقيقة الليلة. شكراً لتعاونكم معنا."
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Quick Templates Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        Text("قوالب إشعارات سريعة جاهزة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        templates.take(2).forEach { (lbl, t, b) ->
                            OutlinedButton(
                                onClick = {
                                    onTitleChange(t)
                                    onBodyChange(b)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        templates.drop(2).forEach { (lbl, t, b) ->
                            OutlinedButton(
                                onClick = {
                                    onTitleChange(t)
                                    onBodyChange(b)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Broadcast Form Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Campaign, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                            Text("إرسال بث جماعي فوري", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Surface(color = PrimaryGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text("إشعار Push", fontSize = 10.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // Target Audience
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("الجمهور المستهدف للبث:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val audiences = listOf(
                                Triple("ALL", "الجميع", Icons.Filled.Groups),
                                Triple("DRIVERS", "السائقين", Icons.Filled.DirectionsCar),
                                Triple("PASSENGERS", "الركاب", Icons.Filled.Person)
                            )
                            audiences.forEach { (key, label, icon) ->
                                val isSelected = audience == key
                                Surface(
                                    onClick = { onAudienceChange(key) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notification Type
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("نوع الإشعار والتصنيف:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val types = listOf(
                                "GENERAL" to "عام",
                                "ALERT" to "تنبيه هام",
                                "PROMO" to "عرض ترويجي",
                                "SYSTEM" to "نظام"
                            )
                            types.forEach { (key, label) ->
                                FilterChip(
                                    selected = type == key,
                                    onClick = { onTypeChange(key) },
                                    label = { Text(label, fontSize = 10.5.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("عنوان الإشعار الجماعي") },
                        placeholder = { Text("مثال: تحديث أمني أو مكافأة شحن جديدة...") },
                        leadingIcon = { Icon(Icons.Filled.Title, contentDescription = null, tint = PrimaryGreen) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Body
                    OutlinedTextField(
                        value = body,
                        onValueChange = onBodyChange,
                        label = { Text("محتوى ونص الإشعار") },
                        placeholder = { Text("اكتب تفاصيل الرسالة التي ستصل لجميع المستخدمين في مركز الإشعارات وهواتفهم...") },
                        leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null, tint = PrimaryGreen) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5
                    )

                    // Info banner
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            Text(
                                "سيتم تسليم هذا الإشعار فوراً في هواتف المستخدمين وحفظه في مركز الإشعارات الخاص بهم مع إمكانية قراءته بالكامل.",
                                fontSize = 11.sp,
                                color = DarkGreen,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Send Button
                    Button(
                        onClick = onSend,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "إرسال البث الجماعي الفوري الآن",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// 7. Branding & Settings Sub-Page
@Composable
private fun AdminBrandingSubPage(
    appName: String,
    onAppNameChange: (String) -> Unit,
    appTagline: String,
    onAppTaglineChange: (String) -> Unit,
    appLogoUrl: String,
    onAppLogoUrlChange: (String) -> Unit,
    iconVariant: String,
    onIconVariantChange: (String) -> Unit,
    isMaintenance: Boolean,
    onToggleMaintenance: (Boolean) -> Unit,
    shamCashAccount: String,
    onShamCashChange: (String) -> Unit,
    appDownloadUrl: String,
    onDownloadUrlChange: (String) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("الهوية السحابية للتطبيق (Remote Branding)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = appName,
                        onValueChange = onAppNameChange,
                        label = { Text("اسم التطبيق المعتمد") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = appTagline,
                        onValueChange = onAppTaglineChange,
                        label = { Text("الشعار الترويجي (Tagline)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = appLogoUrl,
                        onValueChange = onAppLogoUrlChange,
                        label = { Text("رابط صورة اللوغو (Cloud Logo URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shamCashAccount,
                        onValueChange = onShamCashChange,
                        label = { Text("رقم حساب شام كاش الموحد للاستقبال") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = appDownloadUrl,
                        onValueChange = onDownloadUrlChange,
                        label = { Text("رابط تحميل التطبيق المباشر (APK / Store)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("وضع الصيانة المؤقت", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("إيقاف الحجوزات ونشر الرحلات مؤقتاً للصيانة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isMaintenance, onCheckedChange = onToggleMaintenance)
                    }

                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وتطبيق التغييرات سحابياً", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 8. Financial Ledger Sub-Page
@Composable
private fun AdminFinancialLedgerSubPage(
    transactions: List<WalletTransactionEntity>,
    totalRevenue: Double,
    totalPointsSold: Int
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 12.dp) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("إجمالي الإيراد المحقق", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format(java.util.Locale.US, "%.2f", totalRevenue)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PrimaryGreen)
                }
            }
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 12.dp) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("إجمالي النقاط المودعة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalPointsSold pts", fontSize = 14.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                }
            }
        }

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد حركات مالية مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(transactions) { tx ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.description, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("المستخدم: ${tx.userId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = if (tx.type == "DEDUCTION") "-${tx.points} pts" else "+${tx.points} pts",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (tx.type == "DEDUCTION") MaterialTheme.colorScheme.error else PrimaryGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

// 9. Support Tickets Sub-Page
@Composable
private fun AdminSupportTicketsSubPage(
    tickets: List<SupportTicket>,
    onReplyClick: (SupportTicket) -> Unit
) {
    if (tickets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد تذاكر دعم فني مفتوحة حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(tickets) { ticket ->
                val isResolved = ticket.status == "RESOLVED"
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ticket.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isResolved) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (isResolved) "تم الحل" else "مفتوحة",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isResolved) Color(0xFF065F46) else Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("من: ${ticket.userName} (${ticket.userEmail}) • التاريخ: ${ticket.dateText}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ticket.messageText, fontSize = 12.sp)

                        if (ticket.adminReply != null) {
                            Surface(color = PrimaryGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                Text("رد المشرف: ${ticket.adminReply}", fontSize = 11.sp, color = PrimaryGreen, modifier = Modifier.padding(8.dp))
                            }
                        }

                        if (!isResolved) {
                            Button(
                                onClick = { onReplyClick(ticket) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("معالجة والرد على التذكرة", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 10. Security Logs Sub-Page
@Composable
private fun AdminSecurityLogsSubPage(
    activityLogs: List<AdminActivityLog>
) {
    if (activityLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد سجلات نشاط مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(activityLogs) { log ->
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.actionName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("التاريخ: ${log.timestamp}", fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
