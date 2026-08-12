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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue
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

    var userToChatWith by remember { mutableStateOf<UserEntity?>(null) }
    var userToViewDetails by remember { mutableStateOf<UserEntity?>(null) }
    var adminChatMessageText by remember { mutableStateOf("") }
    var adminAttachedImageUri by remember { mutableStateOf<String?>(null) }

    var rideToCancel by remember { mutableStateOf<RideEntity?>(null) }
    var cancelRideReasonText by remember { mutableStateOf("") }

    var selectedTicketForReply by remember { mutableStateOf<SupportTicket?>(null) }
    var ticketReplyText by remember { mutableStateOf("") }

    // Password change states
    var oldAdminPass by remember { mutableStateOf("") }
    var newAdminPass by remember { mutableStateOf("") }
    var confirmAdminPass by remember { mutableStateOf("") }

    // Push Notif states
    var notifTitleText by remember { mutableStateOf("") }
    var notifBodyText by remember { mutableStateOf("") }

    // Settings states from VM
    val publishCost by viewModel.ridePublishCost.collectAsState()
    val commissionPercent by viewModel.appCommissionPercent.collectAsState()
    val chatEnabled by viewModel.featureChatEnabled.collectAsState()
    val ratingsEnabled by viewModel.featureRatingsEnabled.collectAsState()
    val womenOnlyEnabled by viewModel.featureWomenOnlyEnabled.collectAsState()
    val currentShamCashAccount by viewModel.shamCashAccount.collectAsState()
    val currentAppDownloadUrl by viewModel.appDownloadUrl.collectAsState()

    val activityLogs by viewModel.adminActivityLogs.collectAsState()
    val loginLogs by viewModel.adminLoginLogs.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val homeBanners by viewModel.homeBanners.collectAsState()

    val allWalletTransactions by viewModel.allWalletTransactions.collectAsState()
    val allChatMessages by viewModel.allChatMessages.collectAsState()

    var previewReceiptUri by remember { mutableStateOf<String?>(null) }

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
                        text = "لوحة تحكم المشغل الرئيسية",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "المشغل الرئيسي (Super Admin)",
                        fontSize = 11.sp,
                        color = TrueBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = TrueBlue.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = TrueBlue, modifier = Modifier.size(14.dp))
                        Text("Super Admin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TrueBlue)
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
                    Text("$${String.format(java.util.Locale.US, "%.2f", totalRevenue)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = TrueBlue)
                }
            }
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("النقاط المباعة", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalPointsSold pts", fontSize = 15.sp, fontWeight = FontWeight.Black, color = TrueBlue)
                }
            }
            GlassCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("إجمالي الرحلات", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${allRides.size}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = TrueBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Admin Tab Chips
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = TrueBlue,
            edgePadding = 0.dp,
            divider = {}
        ) {
            val tabs = listOf(
                "طلبات الشحن (${topUpRequests.size})",
                "المستخدمين (${allUsers.size})",
                "الرحلات (${allRides.size})",
                "الإعدادات العامة",
                "الإشعارات",
                "التقارير والإحصائيات",
                "الدعم الفني (${supportTickets.count { it.status == "OPEN" }})",
                "الأمان والسجلات",
                "سجل المحفظة والعمليات (${allWalletTransactions.size})",
                "مراقبة المحادثات (${allChatMessages.size})"
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
                    // TopUp Requests
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("طلبات الشحن عبر شام كاش", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Button(
                            onClick = { Toast.makeText(context, "تم تصدير تقرير شحن النقاط (Excel/PDF)", Toast.LENGTH_SHORT).show() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrueBlue)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير تقرير", fontSize = 11.sp)
                        }
                    }

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
                                                Text("باقة ${req.packagePoints} نقطة • $${req.packagePriceUsd}", fontSize = 12.sp, color = TrueBlue, fontWeight = FontWeight.Bold)
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
                                            color = TrueBlue.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().clickable { previewReceiptUri = req.receiptImagePath }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = TrueBlue, modifier = Modifier.size(18.dp))
                                                    Text("إشعار دفع شام كاش مرفق يدوياً من الهاتف", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TrueBlue)
                                                }
                                                Button(
                                                    onClick = { previewReceiptUri = req.receiptImagePath },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue)
                                                ) {
                                                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("معاينة الإشعار", fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        if (isPending) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { onApproveRequest(req.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("موافقة وتلقائي الشحن", color = Color.White, fontSize = 12.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = { requestToReject = req },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("رفض مع بيان السبب", fontSize = 12.sp)
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

            1 -> {
                // User Management
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

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
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
                                                modifier = Modifier.size(36.dp).clip(CircleShape).background(TrueBlue.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Person, contentDescription = null, tint = TrueBlue)
                                            }
                                            Column {
                                                Text(u.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${u.phone} • ${u.email}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${u.userRole} • ${u.rating}⭐ • محفظة: ${u.walletPoints} نقطة", fontSize = 10.sp, color = TrueBlue, fontWeight = FontWeight.Bold)
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { userToChatWith = u },
                                            colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("محادثة", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { userToViewDetails = u },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("التفاصيل والنشاط", fontSize = 11.sp)
                                        }

                                        if (u.isSuspended) {
                                            TextButton(onClick = { viewModel.reactivateUser(u.id); Toast.makeText(context, "تمت إعادة تفعيل حساب ${u.name}", Toast.LENGTH_SHORT).show() }) {
                                                Text("تفعيل", fontSize = 11.sp, color = Color(0xFF10B981))
                                            }
                                        } else {
                                            TextButton(onClick = { userToSuspend = u }) {
                                                Text("تعليق", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                        }

                                        TextButton(onClick = { viewModel.deleteUserByAdmin(u.id); Toast.makeText(context, "تم حذف الحساب نهائياً", Toast.LENGTH_SHORT).show() }) {
                                            Text("حذف", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Rides Management
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إدارة الرحلات والمشاوير المنشورة", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                        items(allRides) { ride ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("${ride.startCity} ➔ ${ride.endCity}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("السائق: ${ride.driverName} • ${ride.departureDate} (${ride.departureTime})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("السعر: $${ride.pricePerSeat} • مقاعد متبقية: ${ride.availableSeats}/${ride.totalSeats}", fontSize = 11.sp, color = TrueBlue, fontWeight = FontWeight.Bold)
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

                                    if (ride.status != "CANCELLED") {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            OutlinedButton(
                                                onClick = { rideToCancel = ride },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("إلغاء الرحلة كأدمن", fontSize = 11.sp)
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
                // General Settings
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Info, contentDescription = null, tint = TrueBlue)
                                    Text("سياسة تسعير الرحلات والخصومات", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TrueBlue)
                                }
                                Text(
                                    "تحديد أسعار المقاعد والرحلات يتم بحرية تامة وبشكل كلي من قِبل السائق أثناء نشر رحلته. يمنع النظام الأدمن من فرض سعر إجباري للرحلة أو تطبيق خصم عليها لضمان الشفافية والعدالة.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("إعدادات حساب شام كاش الموحد للمشغل", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)
                                Text("رقم الحساب الموحد المعلن لكافة المستخدمين لشحن المحفظة (الافتراضي: ba64858e96d4ad9c6096948bc2dbc970):", fontSize = 11.sp, color = Color.Gray)

                                var editedAcc by remember(currentShamCashAccount) { mutableStateOf(currentShamCashAccount) }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = editedAcc,
                                        onValueChange = { editedAcc = it },
                                        label = { Text("رقم حساب شام كاش الموحد") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.updateShamCashAccount(editedAcc)
                                            Toast.makeText(context, "تم تحديث رقم حساب شام كاش بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("حفظ الحساب")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("إعدادات رابط تنزيل التطبيق (دعوة الأصدقاء)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)
                                Text("الرابط المرفق تلقائياً في رسائل دعوات الواتساب للمستخدمين:", fontSize = 11.sp, color = Color.Gray)

                                var editedUrl by remember(currentAppDownloadUrl) { mutableStateOf(currentAppDownloadUrl) }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = editedUrl,
                                        onValueChange = { editedUrl = it },
                                        label = { Text("رابط تنزيل التطبيق") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.updateAppDownloadUrl(editedUrl)
                                            Toast.makeText(context, "تم تحديث رابط تنزيل التطبيق بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("حفظ الرابط")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("إعدادات كلفة النشر", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("كلفة نشر الرحلة (نقاط)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("النقاط التي تُخصم تلقائياً من السائق عند النشر", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    OutlinedTextField(
                                        value = publishCost.toString(),
                                        onValueChange = { viewModel.ridePublishCost.value = it.toIntOrNull() ?: 50 },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(90.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("ميزات المنصة والتطبيقات", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("تفعيل الدردشة والمحادثات المباشرة", fontSize = 12.sp)
                                    Switch(checked = chatEnabled, onCheckedChange = { viewModel.featureChatEnabled.value = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("تفعيل نظام التقييم والمراجعات", fontSize = 12.sp)
                                    Switch(checked = ratingsEnabled, onCheckedChange = { viewModel.featureRatingsEnabled.value = it })
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("تفعيل تصفية الرحلات النسائية فقط", fontSize = 12.sp)
                                    Switch(checked = womenOnlyEnabled, onCheckedChange = { viewModel.featureWomenOnlyEnabled.value = it })
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("تغيير كلمة مرور المشغل (Super Admin)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)

                                OutlinedTextField(
                                    value = oldAdminPass,
                                    onValueChange = { oldAdminPass = it },
                                    label = { Text("كلمة المرور الحالية") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newAdminPass,
                                    onValueChange = { newAdminPass = it },
                                    label = { Text("كلمة المرور الجديدة") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = confirmAdminPass,
                                    onValueChange = { confirmAdminPass = it },
                                    label = { Text("تأكيد كلمة المرور الجديدة") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (newAdminPass != confirmAdminPass) {
                                            Toast.makeText(context, "كلمتا المرور الجديدتان غير متطابقتين", Toast.LENGTH_SHORT).show()
                                        } else {
                                            if (viewModel.changeAdminPassword(oldAdminPass, newAdminPass)) {
                                                Toast.makeText(context, "تم تغيير كلمة مرور الأدمن بنجاح!", Toast.LENGTH_SHORT).show()
                                                oldAdminPass = ""
                                                newAdminPass = ""
                                                confirmAdminPass = ""
                                            } else {
                                                Toast.makeText(context, "كلمة المرور الحالية غير صحيحة", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("حفظ كلمة المرور الجديدة", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // Notifications & Banners
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("إرسال إشعار عام لكافة المستخدمين", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)

                                OutlinedTextField(
                                    value = notifTitleText,
                                    onValueChange = { notifTitleText = it },
                                    label = { Text("عنوان الإشعار") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = notifBodyText,
                                    onValueChange = { notifBodyText = it },
                                    label = { Text("نص الإشعار") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        if (notifTitleText.isNotBlank() && notifBodyText.isNotBlank()) {
                                            viewModel.sendGlobalNotification(notifTitleText, notifBodyText, "الكل")
                                            Toast.makeText(context, "تم بث الإشعار بنجاح لكافة مستخدمي التطبيق", Toast.LENGTH_SHORT).show()
                                            notifTitleText = ""
                                            notifBodyText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بث الإشعار الآن", color = Color.White)
                                }
                            }
                        }
                    }

                    item {
                        Text("إعلانات البنرات على الصفحة الرئيسية", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    items(homeBanners) { b ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(b.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("توجيه: ${b.targetRoute}", fontSize = 10.sp, color = Color.Gray)
                                }

                                Switch(checked = b.isActive, onCheckedChange = { viewModel.toggleBannerStatus(b.id) })
                            }
                        }
                    }
                }
            }

            5 -> {
                // Reports & Analytics
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("إحصائيات المبيعات والنشاط اليومي", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)

                                Text("توزيع مستخدمي المنصة (سائقين vs ركاب)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(
                                    progress = { 0.35f },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color = TrueBlue,
                                    trackColor = Color.LightGray
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("السائقين (35%)", fontSize = 10.sp, color = TrueBlue)
                                    Text("الركاب (65%)", fontSize = 10.sp, color = Color.Gray)
                                }

                                HorizontalDivider()

                                Text("أعلى الخطوط طلباً ونشاطاً", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("1. دمشق ➔ حلب (42% من الرحلات)", fontSize = 11.sp)
                                    Text("2. دمشق ➔ حمص (28% من الرحلات)", fontSize = 11.sp)
                                    Text("3. دمشق ➔ اللاذقية (18% من الرحلات)", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            6 -> {
                // Support Tickets
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
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
                                    Surface(color = TrueBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text("رد الأدمن: ${ticket.adminReply}", fontSize = 11.sp, color = TrueBlue, modifier = Modifier.padding(8.dp))
                                    }
                                }

                                if (ticket.status == "OPEN") {
                                    Button(
                                        onClick = { selectedTicketForReply = ticket },
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
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

            7 -> {
                // Security & Logs
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("سجل عمليات ونشاطات الأدمن (Activity Log)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    activityLogs.forEach { log ->
                                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(log.actionName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TrueBlue)
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

                    item {
                        GlassCard(cornerRadius = 16.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("سجل تسجيلات الدخول للأدمن (Login History)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TrueBlue)

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    loginLogs.forEach { l ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${l.timestamp} • ${l.ipAddress}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(l.deviceBrowser, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            8 -> {
                // Wallet Transactions Audit
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
                                        Column {
                                            Text(tx.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("التاريخ: $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (tx.points > 0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                        ) {
                                            Text(
                                                text = if (tx.points > 0) "+${tx.points} نقطة" else "${tx.points} نقطة",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (tx.points > 0) Color(0xFF065F46) else Color(0xFF991B1B),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            9 -> {
                // Chat Messages Audit
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مراقبة وتفتيش كافة محادثات ورسائل الدردشة بين المستخدمين", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    if (allChatMessages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد رسائل دردشة في النظام بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                            items(allChatMessages) { msg ->
                                val senderName = remember(msg.senderId, allUsers) {
                                    allUsers.find { it.id == msg.senderId }?.name ?: "مستخدم (${msg.senderId})"
                                }
                                val msgDateStr = remember(msg.timestamp) {
                                    try {
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                                    } catch (e: Exception) { "" }
                                }

                                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = TrueBlue)
                                                Text(senderName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Text(msgDateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        if (msg.imageUri != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(140.dp, 90.dp).clickable { previewReceiptUri = msg.imageUri }
                                            ) {
                                                AsyncImage(
                                                    model = msg.imageUri,
                                                    contentDescription = "Chat Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        if (msg.isLocation) {
                                            Text("📍 مشاركة موقع جغرافي مباشر", fontSize = 12.sp, color = TrueBlue, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(msg.messageText, fontSize = 13.sp)
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

    // Reject Dialog
    if (requestToReject != null) {
        AlertDialog(
            onDismissRequest = { requestToReject = null },
            title = { Text("رفض طلب الشحن") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل سبب رفض الطلب وسيصل إشعار للمستخدم:")
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = { rejectionReasonText = it },
                        placeholder = { Text("مثال: عدم وضوح صورة الإشعار") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        requestToReject?.let { onRejectRequest(it.id, rejectionReasonText) }
                        requestToReject = null
                        rejectionReasonText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { requestToReject = null }) {
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
                        placeholder = { Text("مثال: مخالطة سياسات الاستخدام") },
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

    // Ticket Reply Dialog
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
                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue)
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
                    Icon(Icons.Filled.Receipt, contentDescription = null, tint = TrueBlue)
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
                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue)
                ) {
                    Text("إغلاق المعاينة", color = Color.White)
                }
            }
        )
    }

    // Admin Chat With User Dialog
    if (userToChatWith != null) {
        val u = userToChatWith!!
        val chatRoomId = "admin_chat_${u.id}"
        val userMessages = allChatMessages.filter { it.rideId == chatRoomId || (it.senderId == u.id || it.receiverId == u.id) }

        AlertDialog(
            onDismissRequest = {
                userToChatWith = null
                adminChatMessageText = ""
                adminAttachedImageUri = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Chat, contentDescription = null, tint = TrueBlue)
                    Text("محادثة مع المستخدم: ${u.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Message History
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (userMessages.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text("لا توجد رسائل سابقة مع هذا المستخدم.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(userMessages) { msg ->
                                val isAdminMsg = msg.senderId == "admin" || msg.senderId == "system"
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isAdminMsg) Alignment.End else Alignment.Start
                                ) {
                                    Surface(
                                        color = if (isAdminMsg) TrueBlue else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = if (isAdminMsg) "الأدمن" else u.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAdminMsg) Color.White.copy(alpha = 0.8f) else TrueBlue
                                            )
                                            if (!msg.messageText.isNullOrBlank()) {
                                                Text(msg.messageText, color = if (isAdminMsg) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                            }
                                            if (msg.imageUri != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                AsyncImage(
                                                    model = msg.imageUri,
                                                    contentDescription = "Attached Image",
                                                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Image preview if selected
                    if (adminAttachedImageUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().background(TrueBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(6.dp)
                        ) {
                            AsyncImage(
                                model = adminAttachedImageUri,
                                contentDescription = "Preview",
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text("صورة مرفقة جاهزة للإرسال", fontSize = 11.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { adminAttachedImageUri = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }

                    // Input & Attach Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val sampleImages = listOf(
                                    "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957",
                                    "https://images.unsplash.com/photo-1508921912186-1d1a45ebb3c1",
                                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d"
                                )
                                adminAttachedImageUri = sampleImages.random()
                                Toast.makeText(context, "تم إرفاق صورة من ذاكرة الهاتف", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = "Attach Image", tint = TrueBlue)
                        }

                        OutlinedTextField(
                            value = adminChatMessageText,
                            onValueChange = { adminChatMessageText = it },
                            placeholder = { Text("اكتب رسالة للمستخدم...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp)
                        )

                        IconButton(
                            onClick = {
                                if (adminChatMessageText.isNotBlank() || adminAttachedImageUri != null) {
                                    viewModel.sendChatMessage(
                                        rideId = chatRoomId,
                                        text = adminChatMessageText,
                                        imageUri = adminAttachedImageUri,
                                        senderId = "admin",
                                        receiverId = u.id
                                    )
                                    adminChatMessageText = ""
                                    adminAttachedImageUri = null
                                    Toast.makeText(context, "تم إرسال الرسالة بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = TrueBlue)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToChatWith = null
                        adminChatMessageText = ""
                        adminAttachedImageUri = null
                    }
                ) {
                    Text("إغلاق المحادثة")
                }
            }
        )
    }

    // User Details & Activity Dialog
    if (userToViewDetails != null) {
        val u = userToViewDetails!!
        val userTopups = topUpRequests.filter { it.userId == u.id }

        AlertDialog(
            onDismissRequest = { userToViewDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = TrueBlue)
                    Text("تفاصيل ونشاط المستخدم: ${u.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Header Card
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
                                    Text("رصيد المحفظة: ${u.walletPoints} نقطة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrueBlue)
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
                        Text("• التوثيق: ${if (u.isVerified) "حساب موثق للهوية ورقم الهاتف" else "غير موثق"}", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { userToViewDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        )
    }
}
