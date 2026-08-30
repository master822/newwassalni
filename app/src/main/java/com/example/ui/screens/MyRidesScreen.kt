package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.RideBookingEntity
import com.example.data.model.RideEntity
import com.example.data.model.RideStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue

@Composable
fun MyRidesScreen(
    driverRides: List<RideEntity>,
    passengerBookings: List<RideBookingEntity>,
    allRides: List<RideEntity>,
    language: AppLanguage,
    onCancelRide: (String) -> Unit,
    onOpenChat: (RideEntity) -> Unit,
    onDeleteBooking: (bookingId: String, rideId: String) -> Unit = { _, _ -> },
    onDeleteRide: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Driver, 1 = Passenger
    var rideToCancel by remember { mutableStateOf<RideEntity?>(null) }
    var bookingToDelete by remember { mutableStateOf<Pair<RideBookingEntity, RideEntity>?>(null) }
    var driverRideToDelete by remember { mutableStateOf<RideEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = AppStrings.get("my_rides", language),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 2 Tabs Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            indicator = {},
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(AppStrings.get("as_driver", language), fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("my_rides_tab_driver")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(AppStrings.get("as_passenger", language), fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("my_rides_tab_passenger")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Driver Rides Tab
            if (driverRides.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تقم بنشر أي رحلة كسائق بعد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(driverRides) { ride ->
                        RideItemCard(
                            ride = ride,
                            isDriverMode = true,
                            language = language,
                            onCancelClick = { rideToCancel = ride },
                            onDeleteClick = { driverRideToDelete = ride },
                            onChatClick = { onOpenChat(ride) }
                        )
                    }
                }
            }
        } else {
            // Passenger Rides Tab
            if (passengerBookings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تقم بحجز أي رحلة كراكب بعد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(passengerBookings) { booking ->
                        val ride = allRides.find { it.id == booking.rideId }
                        if (ride != null) {
                            RideItemCard(
                                ride = ride,
                                isDriverMode = false,
                                language = language,
                                onCancelClick = { rideToCancel = ride },
                                onDeleteClick = { bookingToDelete = Pair(booking, ride) },
                                onChatClick = { onOpenChat(ride) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancel Dialog
    if (rideToCancel != null) {
        AlertDialog(
            onDismissRequest = { rideToCancel = null },
            title = { Text(AppStrings.get("cancel_ride", language), fontWeight = FontWeight.Bold) },
            text = { Text(AppStrings.get("cancel_confirm", language)) },
            confirmButton = {
                Button(
                    onClick = {
                        rideToCancel?.let { onCancelRide(it.id) }
                        rideToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.get("cancel_ride", language), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rideToCancel = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Booking Dialog (Passenger)
    if (bookingToDelete != null) {
        val (booking, ride) = bookingToDelete!!
        AlertDialog(
            onDismissRequest = { bookingToDelete = null },
            icon = {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("حذف الرحلة من السجل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف وحفظ أرشفة هذه الرحلة (${ride.startCity} ➔ ${ride.endCity}) من سجل رحلاتك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBooking(booking.id, ride.id)
                        bookingToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف من السجل", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { bookingToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Driver Ride Dialog
    if (driverRideToDelete != null) {
        val ride = driverRideToDelete!!
        AlertDialog(
            onDismissRequest = { driverRideToDelete = null },
            icon = {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("حذف الرحلة من السجل", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف هذه الرحلة المنتهية (${ride.startCity} ➔ ${ride.endCity}) من سجلك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRide(ride.id)
                        driverRideToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { driverRideToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun RideItemCard(
    ride: RideEntity,
    isDriverMode: Boolean,
    language: AppLanguage,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onChatClick: () -> Unit
) {
    val statusText = when (ride.status) {
        RideStatus.UPCOMING.name -> AppStrings.get("upcoming", language)
        RideStatus.COMPLETED.name -> AppStrings.get("completed", language)
        else -> AppStrings.get("cancelled", language)
    }

    val statusBg = when (ride.status) {
        RideStatus.UPCOMING.name -> Color(0xFFD1FAE5)
        RideStatus.COMPLETED.name -> TrueBlue.copy(alpha = 0.15f)
        else -> Color(0xFFFEE2E2)
    }

    val statusColor = when (ride.status) {
        RideStatus.UPCOMING.name -> Color(0xFF065F46)
        RideStatus.COMPLETED.name -> TrueBlue
        else -> Color(0xFF991B1B)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${ride.startCity} ➔ ${ride.endCity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    contentColor = statusColor
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${ride.departureDate} • ${ride.departureTime}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${ride.pricePerSeat}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TrueBlue
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDriverMode) "🚗 ${ride.carModel} • ${ride.availableSeats} مقاعد" else "👤 السائق: ${ride.driverName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onChatClick) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = "Chat", tint = TrueBlue)
                    }

                    if (ride.status == RideStatus.UPCOMING.name) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(AppStrings.get("cancel_ride", language), fontSize = 12.sp)
                        }
                    } else {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "حذف من السجل",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
