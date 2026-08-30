package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.RequestedTripEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.TrueBlue
import com.example.ui.theme.TrueBlueLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestedTripsScreen(
    currentUserId: String,
    requestedTrips: List<RequestedTripEntity>,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onPublishRequest: (start: String, end: String, date: String, time: String, men: Int, women: Int, children: Int) -> Unit,
    onAcceptRequest: (requestId: String) -> Unit,
    onDeleteRequest: (requestId: String) -> Unit,
    onCancelAcceptedRequest: ((requestId: String) -> Unit)? = null,
    onOpenChat: ((userId: String, name: String, avatar: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialog state
    var startCity by remember { mutableStateOf("دمشق") }
    var endCity by remember { mutableStateOf("حلب") }
    var departureDate by remember { mutableStateOf("2026-08-09") }
    var departureTime by remember { mutableStateOf("08:00 AM") }
    var menCount by remember { mutableIntStateOf(1) }
    var womenCount by remember { mutableIntStateOf(0) }
    var childrenCount by remember { mutableIntStateOf(0) }

    val openRequests = remember(requestedTrips, currentUserId) {
        requestedTrips.filter { it.status == "OPEN" && it.userId != currentUserId }
    }
    val myRequests = remember(requestedTrips, currentUserId) {
        requestedTrips.filter { it.userId == currentUserId }
    }
    val acceptedByMeRequests = remember(requestedTrips, currentUserId) {
        requestedTrips.filter { it.acceptedByDriverId == currentUserId }
    }

    val currentList = when (selectedTabIndex) {
        0 -> openRequests
        1 -> myRequests
        2 -> acceptedByMeRequests
        else -> openRequests
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الرحلات المطلوبة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.PinDrop, contentDescription = null, tint = Color.White) },
                text = { Text("تثبيت طلب رحلة جديدة", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = TrueBlue,
                modifier = Modifier.testTag("create_trip_request_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TrueBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = TrueBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الطلبات المتاحة", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal)
                            if (openRequests.isNotEmpty()) {
                                Badge(containerColor = TrueBlue, contentColor = Color.White) {
                                    Text("${openRequests.size}")
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("طلباتي المثبتة", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal)
                            if (myRequests.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White) {
                                    Text("${myRequests.size}")
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("قبلتها كسائق", fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal)
                            if (acceptedByMeRequests.isNotEmpty()) {
                                Badge(containerColor = Color(0xFF2E7D32), contentColor = Color.White) {
                                    Text("${acceptedByMeRequests.size}")
                                }
                            }
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTabIndex == 0) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "📌 ميزة تثبيت الطلب للركاب والسائقين",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrueBlue
                                )
                                Text(
                                    text = "هذه القائمة تضم ركاباً يبحثون عن وسيلة نقل. كسائق، يمكنك قبول أي طلب لنقله وإضافته إلى رحلاتك مع خصم 50 نقطة كابتن. وإذا اعتذرت يتم إعادة فتح الطلب وإرجاع النقاط لمحفظتك.",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                if (currentList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Text(
                                    text = when (selectedTabIndex) {
                                        0 -> "لا توجد طلبات رحلات مفتوحة حالياً."
                                        1 -> "لم تقم بتثبيت أي طلب رحلة بعد."
                                        else -> "لم تقبل أي طلب رحلة حتى الآن."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(currentList, key = { "${it.id}_${it.status}_${it.acceptedByDriverId}" }) { req ->
                        RequestedTripCard(
                            req = req,
                            isOwner = req.userId == currentUserId,
                            isAcceptedByMe = req.acceptedByDriverId == currentUserId,
                            onAccept = {
                                onAcceptRequest(req.id)
                                Toast.makeText(context, "تم قبول الطلب بنجاح! انتقل إلى تبويب 'قبلتها كسائق' أو 'رحلاتي' للتواصل مع الراكب.", Toast.LENGTH_LONG).show()
                            },
                            onCancelAccept = {
                                onCancelAcceptedRequest?.invoke(req.id)
                                Toast.makeText(context, "تم إلغاء قبول الطلب وإعادة فتحه في القائمة واسترجاع 50 نقطة", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                onDeleteRequest(req.id)
                                Toast.makeText(context, "تم حذف وإلغاء طلب الرحلة بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onChatWithOther = {
                                if (req.userId == currentUserId && !req.acceptedByDriverId.isNullOrBlank()) {
                                    // Owner chatting with driver
                                    onOpenChat?.invoke(req.acceptedByDriverId!!, req.acceptedByDriverName ?: "الكابتن", "")
                                } else if (req.acceptedByDriverId == currentUserId) {
                                    // Driver chatting with passenger
                                    onOpenChat?.invoke(req.userId, req.userName, req.userAvatar)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Create Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text("تثبيت طلب رحلة جديدة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("أدخل تفاصيل رحلتك المطلوبة ليراها السائقون:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = startCity,
                        onValueChange = { startCity = it },
                        label = { Text("نقطة الانطلاق (مثل: دمشق)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = endCity,
                        onValueChange = { endCity = it },
                        label = { Text("الوجهة (مثل: حلب)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = departureDate,
                            onValueChange = { departureDate = it },
                            label = { Text("تاريخ الانطلاق") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = departureTime,
                            onValueChange = { departureTime = it },
                            label = { Text("توقيت الانطلاق") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("تفاصيل الركاب والمشتركين:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    CounterRow(label = "👨 عدد الرجال", count = menCount, onCountChange = { menCount = it })
                    CounterRow(label = "👩 عدد النساء", count = womenCount, onCountChange = { womenCount = it })
                    CounterRow(label = "👶 عدد الأطفال", count = childrenCount, onCountChange = { childrenCount = it })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (startCity.isBlank() || endCity.isBlank()) {
                            Toast.makeText(context, "يرجى تحديد نقطة الانطلاق والوجهة", Toast.LENGTH_SHORT).show()
                        } else {
                            onPublishRequest(startCity, endCity, departureDate, departureTime, menCount, womenCount, childrenCount)
                            showCreateDialog = false
                            selectedTabIndex = 1 // Switch to "My Requests"
                            Toast.makeText(context, "تم نشر وتثبيت طلبك بنجاح في القائمة!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue)
                ) {
                    Text("تأكيد ونشر الطلب", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun CounterRow(
    label: String,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (count > 0) onCountChange(count - 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Decrease", tint = TrueBlue)
            }
            Text("$count", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(
                onClick = { if (count < 8) onCountChange(count + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.AddCircleOutline, contentDescription = "Increase", tint = TrueBlue)
            }
        }
    }
}

@Composable
fun RequestedTripCard(
    req: RequestedTripEntity,
    isOwner: Boolean,
    isAcceptedByMe: Boolean = false,
    onAccept: () -> Unit,
    onCancelAccept: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onChatWithOther: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header User Info & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (req.userAvatar.isNotBlank()) {
                        AsyncImage(
                            model = req.userAvatar,
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TrueBlueLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = req.userName.take(1),
                                fontWeight = FontWeight.Bold,
                                color = TrueBlue
                            )
                        }
                    }
                    Column {
                        Text(req.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("رقم التواصل: ${req.userPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (req.status == "OPEN") {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "مفتوح بانتظار سائق",
                            color = Color(0xFF2E7D32),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isAcceptedByMe) "مقبول بواسطتك (كسائق)" else "مقبول من ${req.acceptedByDriverName ?: "سائق"}",
                            color = TrueBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Route & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = TrueBlue, modifier = Modifier.size(18.dp))
                    Text("${req.startCity} ➔ ${req.endCity}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text("${req.departureDate} | ${req.departureTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Passenger Breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("👨 ${req.menCount} رجال", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("👩 ${req.womenCount} نساء", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("👶 ${req.childrenCount} أطفال", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwner) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (req.status == "ACCEPTED" && !req.acceptedByDriverId.isNullOrBlank()) {
                            Button(
                                onClick = { onChatWithOther?.invoke() },
                                colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مراسلة الكابتن (${req.acceptedByDriverName ?: "السائق"})", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف الطلب", fontSize = 12.sp)
                        }
                    }
                } else if (req.status == "OPEN") {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قبول الطلب (-50 نقطة كابتن)", fontSize = 12.sp)
                    }
                } else if (isAcceptedByMe) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onChatWithOther?.invoke() },
                            colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مراسلة الراكب (${req.userName})", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onCancelAccept?.invoke() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إلغاء قبولي (+50 نقطة)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
