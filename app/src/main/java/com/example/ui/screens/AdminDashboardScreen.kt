package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.AppViewModel

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
    var selectedTab by remember { mutableStateOf(0) }

    // Dialog states
    var requestToReject by remember { mutableStateOf<TopUpRequestEntity?>(null) }
    var rejectionReasonText by remember { mutableStateOf("") }

    var userToSuspend by remember { mutableStateOf<UserEntity?>(null) }
    var suspendReasonText by remember { mutableStateOf("") }

    var userToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var editUserName by remember { mutableStateOf("") }
    var editUserPhone by remember { mutableStateOf("") }
    var editUserRole by remember { mutableStateOf("PASSENGER") }
    var editUserPoints by remember { mutableStateOf("50") }

    var userToAdjustWallet by remember { mutableStateOf<UserEntity?>(null) }
    var adjustPointsDelta by remember { mutableStateOf("") }
    var adjustPointsReason by remember { mutableStateOf("") }
    var isPointsAddition by remember { mutableStateOf(true) }

    var userToChatWith by remember { mutableStateOf<UserEntity?>(null) }
    var userToViewDetails by remember { mutableStateOf<UserEntity?>(null) }
    var adminChatMessageText by remember { mutableStateOf("") }
    var adminAttachedImageUri by remember { mutableStateOf<String?>(null) }

    var messageToEdit by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var editMessageNewText by remember { mutableStateOf("") }

    var rideToCancel by remember { mutableStateOf<RideEntity?>(null) }
    var cancelRideReasonText by remember { mutableStateOf("") }

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
    var broadcastAudience by remember { mutableStateOf("ALL") } // ALL, DRIVERS, PASSENGERS
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
    val loginLogs by viewModel.adminLoginLogs.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val homeBanners by viewModel.homeBanners.collectAsState()
    val requestedTrips by viewModel.requestedTrips.collectAsState()

    val allWalletTransactions by viewModel.allWalletTransactions.collectAsState()
    val allChatMessages by viewModel.allChatMessages.collectAsState()

    var previewReceiptUri by remember { mutableStateOf<String?>(null) }
    var selectedRideChatRoom by remember { mutableStateOf<String?>(null) }

    val totalRevenue = topUpRequests.filter { it.status == RequestStatus.APPROVED.name }.sumOf { it.packagePriceUsd }
    val totalPointsSold = topUpRequests.filter { it.status == RequestStatus.APPROVED.name }.sumOf { it.packagePoints }

    // Check inactivity timeout
    LaunchedEffect(Unit) {
        if (viewModel.checkAdminSessionTimeout()) {
            Toast.makeText(context, "انتهت الجلسة بعد ساعة من عدم النشاط، يرجى إعادة تسجيل الدخول", Toast.LENGTH_LONG).show()
            onLogoutAdmin()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "لوحة تحكم المشغل الرئيسية (Super Admin)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "التحكم الشامل بجميع المستخدمين، الرحلات، المحادثات والأرصدة",
                        fontSize = 10.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = GoldAccent.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                        Text("Super Admin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.logoutAdmin()
                        onLogoutAdmin()
                        Toast.makeText(context, "تم تسجيل خروج الأدمن بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("admin_logout_btn")
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Metrics Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("الإيرادات المعتمدة", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format(java.util.Locale.US, "%.2f", totalRevenue)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PrimaryGreen)
                }
            }
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("النقاط المباعة", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalPointsSold pts", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PrimaryGreen)
                }
            }
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("الرحلات المعروضة", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${allRides.size}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = PrimaryGreen)
                }
            }
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("طلبات الرحلات", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${requestedTrips.size}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = GoldAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Admin Tab Chips
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryGreen,
            edgePadding = 0.dp,
            divider = {}
        ) {
            val tabs = listOf(
                "المستخدمين (${allUsers.size})",
                "التحكم بالمحادثات (${allChatMessages.size})",
                "طلبات الشحن (${topUpRequests.size})",
                "الرحلات المنشورة (${allRides.size})",
                "طلبات الرحلات (${requestedTrips.size})",
                "الإشعارات الجماعية",
                "الإعدادات والهوية",
                "سجل المحفظة (${allWalletTransactions.size})",
                "الدعم الفني (${supportTickets.count { it.status == "OPEN" }})",
                "سجلات الأمان (${activityLogs.size})"
            )
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> {
                    // Users Management with Impersonation, Edit, Wallet Adjust
                    var userSearchQuery by remember { mutableStateOf("") }
                    val filteredUsers = allUsers.filter {
                        it.name.contains(userSearchQuery, ignoreCase = true) ||
                        it.email.contains(userSearchQuery, ignoreCase = true) ||
                        it.phone.contains(userSearchQuery, ignoreCase = true)
                    }

                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            placeholder = { Text("بحث عن مستخدم بالاسم، الإيميل، أو الهاتف...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_user_search")
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(filteredUsers) { u ->
                                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryGreen)
                                                }
                                                Column {
                                                    Text(u.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("${u.phone} • ${u.email}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                                        // Action Buttons Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // IMPERSONATION BUTTON (Critical)
                                            Button(
                                                onClick = {
                                                    viewModel.startImpersonation(u)
                                                    Toast.makeText(context, "تم الدخول بحساب المستخدم: ${u.name}", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1.2f)
                                            ) {
                                                Icon(Icons.Filled.SwitchAccount, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("دخول بحسابه", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // ADJUST WALLET
                                            OutlinedButton(
                                                onClick = {
                                                    userToAdjustWallet = u
                                                    adjustPointsDelta = ""
                                                    adjustPointsReason = ""
                                                    isPointsAddition = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("الرصيد", fontSize = 10.sp)
                                            }

                                            // EDIT USER
                                            OutlinedButton(
                                                onClick = {
                                                    userToEdit = u
                                                    editUserName = u.name
                                                    editUserPhone = u.phone
                                                    editUserRole = u.userRole
                                                    editUserPoints = u.walletPoints.toString()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("تعديل", fontSize = 10.sp)
                                            }

                                            // DETAILS
                                            IconButton(
                                                onClick = { userToViewDetails = u },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Filled.Info, contentDescription = "Details", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                            }

                                            // SUSPEND / REACTIVATE
                                            if (u.isSuspended) {
                                                IconButton(
                                                    onClick = { viewModel.reactivateUser(u.id); Toast.makeText(context, "تمت إعادة تفعيل حساب ${u.name}", Toast.LENGTH_SHORT).show() },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Activate", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { userToSuspend = u },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Filled.Block, contentDescription = "Suspend", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Chat Control Center (Super Admin Full Control)
                    val ridesWithMessages = allRides.filter { r -> allChatMessages.any { it.rideId == r.id } }
                    val activeRideChat = allRides.find { it.id == selectedRideChatRoom }

                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("التحكم الكامل والرقابة على محادثات الرحلات", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            if (selectedRideChatRoom != null) {
                                TextButton(onClick = { selectedRideChatRoom = null }) {
                                    Text("عرض كل الرحلات", fontSize = 11.sp, color = PrimaryGreen)
                                }
                            }
                        }

                        if (selectedRideChatRoom == null) {
                            // List of all conversations
                            if (allChatMessages.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد رسائل محادثة في النظام بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp)
                                ) {
                                    items(allRides) { ride ->
                                        val rideMsgs = allChatMessages.filter { it.rideId == ride.id }
                                        if (rideMsgs.isNotEmpty()) {
                                            val lastMsg = rideMsgs.last()
                                            GlassCard(
                                                modifier = Modifier.fillMaxWidth().clickable { selectedRideChatRoom = ride.id },
                                                cornerRadius = 14.dp
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${ride.startCity} ➔ ${ride.endCity}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Surface(color = PrimaryGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                                            Text("${rideMsgs.size} رسائل", fontSize = 10.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                    Text("السائق: ${ride.driverName} • التاريخ: ${ride.departureDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("آخر رسالة: ${lastMsg.messageText}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Viewing a specific ride's chat room with Super Admin powers
                            val currentRoomMsgs = allChatMessages.filter { it.rideId == selectedRideChatRoom }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("محادثة رحلة: ${activeRideChat?.startCity} ➔ ${activeRideChat?.endCity}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryGreen)
                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteChatRoom(selectedRideChatRoom!!)
                                        Toast.makeText(context, "تم تفريغ وحذف جميع رسائل المحادثة", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("تفريغ المحادثة", fontSize = 10.sp)
                                }
                            }

                            // Chat Messages List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentRoomMsgs) { msg ->
                                    val isSystem = msg.senderId == "system"
                                    val isAdmin = msg.senderId == "admin" || msg.senderId == "super_admin"

                                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 10.dp) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(
                                                        if (isAdmin) Icons.Filled.AdminPanelSettings else if (isSystem) Icons.Filled.Info else Icons.Filled.Person,
                                                        contentDescription = null,
                                                        tint = if (isAdmin) GoldAccent else if (isSystem) Color.Blue else PrimaryGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = if (isAdmin) "مدير النظام (Admin)" else if (isSystem) "تنبيه النظام" else "مرسل: ${msg.senderId}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isAdmin) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                // Admin actions on message
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = {
                                                            messageToEdit = msg
                                                            editMessageNewText = msg.messageText
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteChatMessage(msg.id)
                                                            Toast.makeText(context, "تم حذف الرسالة", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }

                                            if (msg.imageUri != null) {
                                                AsyncImage(
                                                    model = msg.imageUri,
                                                    contentDescription = "Message Image",
                                                    modifier = Modifier.size(120.dp, 80.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }

                                            Text(msg.messageText, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // Send Message as Super Admin bar
                            var adminSenderRole by remember { mutableStateOf("ADMIN") } // ADMIN, SYSTEM, DRIVER, PASSENGER
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إرسال كـ:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                val roles = listOf("ADMIN" to "أدمن", "SYSTEM" to "نظام", "DRIVER" to "سائق", "PASSENGER" to "راكب")
                                roles.forEach { (key, label) ->
                                    FilterChip(
                                        selected = adminSenderRole == key,
                                        onClick = { adminSenderRole = key },
                                        label = { Text(label, fontSize = 9.sp) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val sampleImages = listOf(
                                            "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957",
                                            "https://images.unsplash.com/photo-1508921912186-1d1a45ebb3c1"
                                        )
                                        adminAttachedImageUri = sampleImages.random()
                                        Toast.makeText(context, "تم إرفاق صورة", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = "Attach", tint = PrimaryGreen)
                                }

                                OutlinedTextField(
                                    value = adminChatMessageText,
                                    onValueChange = { adminChatMessageText = it },
                                    placeholder = { Text("اكتب رسالة كمدير النظام...", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(20.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (adminChatMessageText.isNotBlank() || adminAttachedImageUri != null) {
                                            val senderId = when (adminSenderRole) {
                                                "SYSTEM" -> "system"
                                                "DRIVER" -> activeRideChat?.driverId ?: "driver_id"
                                                "PASSENGER" -> "passenger_id"
                                                else -> "super_admin"
                                            }
                                            val senderName = when (adminSenderRole) {
                                                "SYSTEM" -> "تنبيه النظام"
                                                "DRIVER" -> activeRideChat?.driverName ?: "السائق"
                                                "PASSENGER" -> "الراكب"
                                                else -> "مدير النظام"
                                            }

                                            viewModel.sendAdminChatMessage(
                                                rideId = selectedRideChatRoom!!,
                                                senderId = senderId,
                                                senderName = senderName,
                                                messageText = adminChatMessageText,
                                                imageUri = adminAttachedImageUri,
                                                isSystem = adminSenderRole == "SYSTEM"
                                            )
                                            adminChatMessageText = ""
                                            adminAttachedImageUri = null
                                            Toast.makeText(context, "تم إرسال الرسالة", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = PrimaryGreen)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TopUp Requests
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("طلبات الشحن عبر شام كاش", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        if (topUpRequests.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد طلبات شحن حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(topUpRequests) { req ->
                                    val isPending = req.status == RequestStatus.PENDING.name
                                    val isApproved = req.status == RequestStatus.APPROVED.name

                                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                modifier = Modifier.fillMaxWidth().clickable { previewReceiptUri = req.receiptImagePath }
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
                                                        onClick = { onApproveRequest(req.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("موافقة وإضافة النقاط", fontSize = 12.sp, color = Color.White)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { requestToReject = req },
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
                }

                3 -> {
                    // Rides Management
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إدارة وتعديل الرحلات المنشورة", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                            items(allRides) { ride ->
                                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("${ride.startCity} ➔ ${ride.endCity}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("السائق: ${ride.driverName} • ${ride.departureDate} (${ride.departureTime})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("السعر: $${ride.pricePerSeat} • مقاعد متبقية: ${ride.availableSeats}/${ride.totalSeats}", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (ride.status == "CANCELLED") Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                                            ) {
                                                Text(
                                                    text = if (ride.status == "CANCELLED") "ملغاة" else "متاحة",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = if (ride.status == "CANCELLED") Color(0xFF991B1B) else Color(0xFF065F46),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                                            OutlinedButton(
                                                onClick = {
                                                    rideToEdit = ride
                                                    editRideStartCity = ride.startCity
                                                    editRideEndCity = ride.endCity
                                                    editRideDate = ride.departureDate
                                                    editRideTime = ride.departureTime
                                                    editRidePrice = ride.pricePerSeat.toString()
                                                    editRideSeats = ride.availableSeats.toString()
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("تعديل الرحلة", fontSize = 11.sp)
                                            }

                                            if (ride.status != "CANCELLED") {
                                                OutlinedButton(
                                                    onClick = { rideToCancel = ride },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("إلغاء كأدمن", fontSize = 11.sp)
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.adminDeleteRide(ride.id)
                                                    Toast.makeText(context, "تم حذف الرحلة نهائياً", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // Requested Trips Management
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إدارة طلبات الرحلات (المفتوحة والمقبولة)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        if (requestedTrips.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد طلبات رحلات حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(requestedTrips) { req ->
                                    val isOpen = req.status == "OPEN"
                                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column {
                                                    Text("${req.startCity} ➔ ${req.endCity}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("صاحب الطلب: ${req.userName} • ${req.userPhone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("الموعد: ${req.departureDate} (${req.departureTime}) • الركاب: ${req.menCount} رجال، ${req.womenCount} نساء، ${req.childrenCount} أطفال", fontSize = 11.sp, color = PrimaryGreen)
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isOpen) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        text = if (isOpen) "مفتوح للجميع" else "مقبول (${req.acceptedByDriverName ?: "سائق"})",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        color = if (isOpen) Color(0xFF065F46) else Color(0xFFD97706),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
                                                if (!isOpen) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.adminReopenRequestedTrip(req.id)
                                                            Toast.makeText(context, "تمت إعادة فتح الطلب للسائقين", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("إعادة فتحه (Reopen)", fontSize = 10.sp)
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        requestedTripToEdit = req
                                                        editReqStartCity = req.startCity
                                                        editReqEndCity = req.endCity
                                                        editReqDate = req.departureDate
                                                        editReqTime = req.departureTime
                                                        editReqMen = req.menCount.toString()
                                                        editReqWomen = req.womenCount.toString()
                                                        editReqChildren = req.childrenCount.toString()
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("تعديل الطلب", fontSize = 10.sp)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteRequestedTrip(req.id)
                                                        Toast.makeText(context, "تم حذف الطلب", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // Broadcast Notifications
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                        item {
                            GlassCard(cornerRadius = 16.dp) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("إرسال إشعار جماعي لجميع المستخدمين (Broadcast)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryGreen)

                                    Text("الفئة المستهدفة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val audiences = listOf("ALL" to "جميع المستخدمين", "DRIVERS" to "السائقون فقط", "PASSENGERS" to "الركاب فقط")
                                        audiences.forEach { (key, label) ->
                                            FilterChip(
                                                selected = broadcastAudience == key,
                                                onClick = { broadcastAudience = key },
                                                label = { Text(label, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = broadcastTitleText,
                                        onValueChange = { broadcastTitleText = it },
                                        label = { Text("عنوان الإشعار") },
                                        placeholder = { Text("مثال: تحديث هام بشأن الرحلات") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = broadcastBodyText,
                                        onValueChange = { broadcastBodyText = it },
                                        label = { Text("نص الإشعار") },
                                        placeholder = { Text("اكتب نص الرسالة التي ستصل لجميع المستخدمين...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3
                                    )

                                    Button(
                                        onClick = {
                                            if (broadcastTitleText.isNotBlank() && broadcastBodyText.isNotBlank()) {
                                                viewModel.sendBroadcastNotification(
                                                    title = broadcastTitleText,
                                                    message = broadcastBodyText,
                                                    targetAudience = broadcastAudience,
                                                    type = broadcastType
                                                )
                                                Toast.makeText(context, "تم إرسال الإشعار الجماعي بنجاح لجميع المستهدفين", Toast.LENGTH_SHORT).show()
                                                broadcastTitleText = ""
                                                broadcastBodyText = ""
                                            } else {
                                                Toast.makeText(context, "يرجى كتابة العنوان ونص الإشعار", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Campaign, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إرسال الإشعار الجماعي الآن", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // App Customization & Remote Configuration
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                        item {
                            GlassCard(cornerRadius = 16.dp) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("تخصيص الهوية والشعار والأيقونة السحابية", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryGreen)

                                    OutlinedTextField(
                                        value = configAppName,
                                        onValueChange = { configAppName = it },
                                        label = { Text("اسم التطبيق المعتمد") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = configAppTagline,
                                        onValueChange = { configAppTagline = it },
                                        label = { Text("الشعار / Slogan") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = configAppLogoUrl,
                                        onValueChange = { configAppLogoUrl = it },
                                        label = { Text("رابط صورة الشعار (Logo URL)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Text("نمط ولون أيقونة التطبيق الحركية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    val iconVariants = listOf("Emerald Green (افتراضي)", "Gold Royale", "Midnight Dark")
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        iconVariants.forEach { variant ->
                                            FilterChip(
                                                selected = configIconVariant == variant,
                                                onClick = { configIconVariant = variant },
                                                label = { Text(variant, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("تفعيل وضع الصيانة (Maintenance Mode)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Switch(
                                            checked = configIsMaintenance,
                                            onCheckedChange = { configIsMaintenance = it }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text("بيانات الدفع والتحميل:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)

                                    OutlinedTextField(
                                        value = shamCashInput,
                                        onValueChange = { shamCashInput = it },
                                        label = { Text("رقم حساب شام كاش الموحد") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = appDownloadUrlInput,
                                        onValueChange = { appDownloadUrlInput = it },
                                        label = { Text("رابط تنزيل التطبيق الموحد") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.updateRemoteAppConfig(configAppName, configAppTagline, configAppLogoUrl, configIconVariant, configIsMaintenance)
                                            viewModel.updateShamCashAccount(shamCashInput)
                                            viewModel.updateAppDownloadUrl(appDownloadUrlInput)
                                            Toast.makeText(context, "تم حفظ وتحديث الإعدادات العامة بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                7 -> {
                    // Wallet Audit
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تدقيق محفظة النقاط والعمليات المالية لجميع المستخدمين", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        if (allWalletTransactions.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
                                Text("لا توجد عمليات محفظة مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                                items(allWalletTransactions) { tx ->
                                    val dateStr = remember(tx.createdAt) {
                                        try {
                                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(tx.createdAt))
                                        } catch (e: Exception) { "" }
                                    }

                                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(tx.description, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("المستخدم: ${tx.userId} • التاريخ: $dateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (tx.points > 0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                                ) {
                                                    Text(
                                                        text = if (tx.points > 0) "+${tx.points} نقطة" else "${tx.points} نقطة",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = if (tx.points > 0) Color(0xFF065F46) else Color(0xFF991B1B),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.adminCancelWalletTransaction(tx.id)
                                                        Toast.makeText(context, "تم إلغاء المعاملة المالية", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                8 -> {
                    // Support Tickets
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                        items(supportTickets) { ticket ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(ticket.subject, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("من: ${ticket.userName} (${ticket.userEmail}) • ${ticket.dateText}", fontSize = 10.sp, color = Color.Gray)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (ticket.status == "OPEN") Color(0xFFFEF3C7) else Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = if (ticket.status == "OPEN") "مفتوحة" else "معالجة",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (ticket.status == "OPEN") Color(0xFFD97706) else Color(0xFF065F46),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(ticket.messageText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                                    if (ticket.adminReply != null) {
                                        Surface(color = PrimaryGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Text("رد الأدمن: ${ticket.adminReply}", fontSize = 11.sp, color = PrimaryGreen, modifier = Modifier.padding(8.dp))
                                        }
                                    }

                                    if (ticket.status == "OPEN") {
                                        Button(
                                            onClick = { selectedTicketForReply = ticket },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("الرد وحل التذكرة", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                9 -> {
                    // Security & Logs
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                        item {
                            GlassCard(cornerRadius = 16.dp) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("سجل عمليات ونشاطات الأدمن (Activity Log)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryGreen)

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        activityLogs.forEach { log ->
                                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(log.actionName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryGreen)
                                                        Text(log.timestamp, fontSize = 9.sp, color = Color.Gray)
                                                    }
                                                    Text(log.details, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    // Edit User Dialog
    if (userToEdit != null) {
        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = { Text("تعديل بيانات المستخدم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editUserName,
                        onValueChange = { editUserName = it },
                        label = { Text("اسم المستخدم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUserPhone,
                        onValueChange = { editUserPhone = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUserRole,
                        onValueChange = { editUserRole = it },
                        label = { Text("الدور (PASSENGER أو DRIVER)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUserPoints,
                        onValueChange = { editUserPoints = it },
                        label = { Text("رصيد النقاط") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToEdit?.let {
                            val pts = editUserPoints.toIntOrNull() ?: it.walletPoints
                            viewModel.adminUpdateUserData(it.id, editUserName, editUserPhone, editUserRole, pts)
                            Toast.makeText(context, "تم حفظ التعديلات بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        userToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Adjust Wallet Dialog
    if (userToAdjustWallet != null) {
        val u = userToAdjustWallet!!
        AlertDialog(
            onDismissRequest = { userToAdjustWallet = null },
            title = { Text("تعديل رصيد محفظة: ${u.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرصيد الحالي: ${u.walletPoints} نقطة", fontWeight = FontWeight.Bold, color = PrimaryGreen)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isPointsAddition,
                            onClick = { isPointsAddition = true },
                            label = { Text("إيداع نقاط (+)") }
                        )
                        FilterChip(
                            selected = !isPointsAddition,
                            onClick = { isPointsAddition = false },
                            label = { Text("خصم نقاط (-)") }
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
                        label = { Text("السبب أو الملاحظة") },
                        placeholder = { Text("مثال: مكافأة نشاط أو تسوية حساب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = adjustPointsDelta.toIntOrNull() ?: 0
                        if (amount > 0) {
                            val delta = if (isPointsAddition) amount else -amount
                            viewModel.adminAdjustUserWallet(u.id, delta, adjustPointsReason.ifBlank { "تعديل إداري" })
                            Toast.makeText(context, "تم تعديل الرصيد بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        userToAdjustWallet = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("تأكيد التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToAdjustWallet = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Chat Message Dialog
    if (messageToEdit != null) {
        AlertDialog(
            onDismissRequest = { messageToEdit = null },
            title = { Text("تعديل نص الرسالة كأدمن") },
            text = {
                OutlinedTextField(
                    value = editMessageNewText,
                    onValueChange = { editMessageNewText = it },
                    label = { Text("نص الرسالة المعدل") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        messageToEdit?.let {
                            viewModel.editChatMessage(it.id, editMessageNewText)
                            Toast.makeText(context, "تم تعديل نص الرسالة", Toast.LENGTH_SHORT).show()
                        }
                        messageToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Ride Dialog
    if (rideToEdit != null) {
        AlertDialog(
            onDismissRequest = { rideToEdit = null },
            title = { Text("تعديل بيانات الرحلة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editRideStartCity,
                            onValueChange = { editRideStartCity = it },
                            label = { Text("مدينة الانطلاق") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editRideEndCity,
                            onValueChange = { editRideEndCity = it },
                            label = { Text("مدينة الوصول") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editRideDate,
                            onValueChange = { editRideDate = it },
                            label = { Text("التاريخ") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editRideTime,
                            onValueChange = { editRideTime = it },
                            label = { Text("الوقت") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editRidePrice,
                            onValueChange = { editRidePrice = it },
                            label = { Text("السعر ($)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editRideSeats,
                            onValueChange = { editRideSeats = it },
                            label = { Text("المقاعد") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        rideToEdit?.let {
                            val price = editRidePrice.toDoubleOrNull() ?: it.pricePerSeat
                            val seats = editRideSeats.toIntOrNull() ?: it.availableSeats
                            viewModel.adminEditRide(it.id, editRideStartCity, editRideEndCity, editRideDate, editRideTime, price, seats)
                            Toast.makeText(context, "تم تعديل الرحلة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        rideToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { rideToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Requested Trip Dialog
    if (requestedTripToEdit != null) {
        AlertDialog(
            onDismissRequest = { requestedTripToEdit = null },
            title = { Text("تعديل طلب الرحلة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editReqStartCity,
                            onValueChange = { editReqStartCity = it },
                            label = { Text("الانطلاق") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editReqEndCity,
                            onValueChange = { editReqEndCity = it },
                            label = { Text("الوصول") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editReqDate,
                            onValueChange = { editReqDate = it },
                            label = { Text("التاريخ") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editReqTime,
                            onValueChange = { editReqTime = it },
                            label = { Text("الوقت") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = editReqMen,
                            onValueChange = { editReqMen = it },
                            label = { Text("رجال") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editReqWomen,
                            onValueChange = { editReqWomen = it },
                            label = { Text("نساء") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editReqChildren,
                            onValueChange = { editReqChildren = it },
                            label = { Text("أطفال") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        requestedTripToEdit?.let {
                            val men = editReqMen.toIntOrNull() ?: it.menCount
                            val women = editReqWomen.toIntOrNull() ?: it.womenCount
                            val ch = editReqChildren.toIntOrNull() ?: it.childrenCount
                            viewModel.adminEditRequestedTrip(it.id, editReqStartCity, editReqEndCity, editReqDate, editReqTime, men, women, ch)
                            Toast.makeText(context, "تم تعديل طلب الرحلة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                        requestedTripToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { requestedTripToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Suspend User Dialog
    if (userToSuspend != null) {
        AlertDialog(
            onDismissRequest = { userToSuspend = null },
            title = { Text("تعليق حساب مستخدم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل سبب تعليق حساب ${userToSuspend?.name}:")
                    OutlinedTextField(
                        value = suspendReasonText,
                        onValueChange = { suspendReasonText = it },
                        placeholder = { Text("مثال: مخالفة شروط الخدمة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToSuspend?.let { viewModel.suspendUser(it.id, suspendReasonText) }
                        Toast.makeText(context, "تم تعليق الحساب بنجاح", Toast.LENGTH_SHORT).show()
                        userToSuspend = null
                        suspendReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد التعليق", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToSuspend = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Cancel Ride Dialog
    if (rideToCancel != null) {
        AlertDialog(
            onDismissRequest = { rideToCancel = null },
            title = { Text("إلغاء رحلة من قبل الأدمن") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل سبب إجبار إلغاء الرحلة وسيصل إشعار للسائق والركاب:")
                    OutlinedTextField(
                        value = cancelRideReasonText,
                        onValueChange = { cancelRideReasonText = it },
                        placeholder = { Text("مثال: بلاغ صيانة أو عدم التزام بالتعليمات") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        rideToCancel?.let { viewModel.cancelRideByAdmin(it.id, cancelRideReasonText) }
                        Toast.makeText(context, "تم إلغاء الرحلة وإشعار الأطراف", Toast.LENGTH_SHORT).show()
                        rideToCancel = null
                        cancelRideReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الإلغاء", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rideToCancel = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Support Reply Dialog
    if (selectedTicketForReply != null) {
        AlertDialog(
            onDismissRequest = { selectedTicketForReply = null },
            title = { Text("الرد على تذكرة الدعم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("موضوع: ${selectedTicketForReply?.subject}")
                    Text("الرسالة: ${selectedTicketForReply?.messageText}", fontSize = 12.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = ticketReplyText,
                        onValueChange = { ticketReplyText = it },
                        placeholder = { Text("اكتب الرد الرسمي هنا...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTicketForReply?.let { viewModel.replySupportTicket(it.id, ticketReplyText) }
                        Toast.makeText(context, "تم إرسال الرد وإغلاق التذكرة", Toast.LENGTH_SHORT).show()
                        selectedTicketForReply = null
                        ticketReplyText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("إرسال الرد", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicketForReply = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Receipt Preview Modal
    if (previewReceiptUri != null) {
        AlertDialog(
            onDismissRequest = { previewReceiptUri = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = PrimaryGreen)
                    Text("إشعار دفع شام كاش المرفق", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    ) {
                        AsyncImage(
                            model = previewReceiptUri,
                            contentDescription = "Receipt Full View",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text("تم إرفاق صورة هذا الإشعار يدوياً بواسطة المستخدم من هاتفه المحمول.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { previewReceiptUri = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("إغلاق المعاينة", color = Color.White)
                }
            }
        )
    }

    // User Details Modal
    if (userToViewDetails != null) {
        val u = userToViewDetails!!
        val userTopups = topUpRequests.filter { it.userId == u.id }

        AlertDialog(
            onDismissRequest = { userToViewDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = PrimaryGreen)
                    Text("تفاصيل ونشاط المستخدم: ${u.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AsyncImage(
                                model = u.avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(50.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(u.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("الهاتف: ${u.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("البريد: ${u.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("رصيد المحفظة: ${u.walletPoints} نقطة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                    Text("التقييم: ${u.rating} ⭐", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text("نشاطات المستخدم في النظام:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Column(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("• عدد الرحلات المكتملة: ${u.rideCount} رحلة", fontSize = 12.sp)
                        Text("• حالة الحساب: ${if (u.isSuspended) "معلق (${u.suspendReason ?: "بدون سبب"})" else "نشط ومفعل"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (u.isSuspended) Color.Red else Color(0xFF10B981))
                        Text("• عدد طلبات شحن شام كاش: ${userTopups.size} طلبات", fontSize = 12.sp)
                        Text("• كود الإحالة الخاص به: ${u.referralCode ?: "WASALNI-50"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { userToViewDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        )
    }
}
