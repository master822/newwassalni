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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    // Dialog state
    var startCity by remember { mutableStateOf("دمشق") }
    var endCity by remember { mutableStateOf("حلب") }
    var departureDate by remember { mutableStateOf("2026-08-09") }
    var departureTime by remember { mutableStateOf("08:00 AM") }
    var menCount by remember { mutableIntStateOf(1) }
    var womenCount by remember { mutableIntStateOf(0) }
    var childrenCount by remember { mutableIntStateOf(0) }

    val popularCities = listOf("دمشق", "حلب", "حمص", "اللاذقية", "طرطوس", "حماة", "بيروت", "إسطنبول", "عمان")

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📌 ميزة تثبيت الطلب",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrueBlue
                        )
                        Text(
                            text = "إذا لم تجد رحلة تناسب مواعيدك، يمكنك تثبيت طلب رحلتك هنا مع تحديد عدد الرجال، النساء، والأطفال. وسيتمكن السائقون من الاطلاع على طلبك وقبوله فوراً!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (requestedTrips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد طلبات رحلات مثبتة حالياً.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(requestedTrips, key = { "${it.id}_${it.createdAt}" }) { req ->
                    RequestedTripCard(
                        req = req,
                        isOwner = req.userId == currentUserId,
                        onAccept = {
                            onAcceptRequest(req.id)
                            Toast.makeText(context, "تم قبول الطلب بنجاح! ستظهر الرحلة في رحلاتك", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            onDeleteRequest(req.id)
                            Toast.makeText(context, "تم إلغاء وحذف طلب الرحلة", Toast.LENGTH_SHORT).show()
                        }
                    )
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

                    // From dropdown
                    OutlinedTextField(
                        value = startCity,
                        onValueChange = { startCity = it },
                        label = { Text("نقطة الانطلاق") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // To dropdown
                    OutlinedTextField(
                        value = endCity,
                        onValueChange = { endCity = it },
                        label = { Text("الوجهة") },
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

                    // Men counter
                    CounterRow(label = "👨 عدد الرجال", count = menCount, onCountChange = { menCount = it })

                    // Women counter
                    CounterRow(label = "👩 عدد النساء", count = womenCount, onCountChange = { womenCount = it })

                    // Children counter
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
    onAccept: () -> Unit,
    onDelete: () -> Unit
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
                            text = "مقبول من ${req.acceptedByDriverName ?: "سائق"}",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف/إلغاء")
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
                        Text("قبول الطلب (سأكون السائق)")
                    }
                }
            }
        }
    }
}
