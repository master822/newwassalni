package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.RideEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue
import com.example.ui.theme.TrueBlueLight

@Composable
fun RideDetailScreen(
    ride: RideEntity,
    language: AppLanguage,
    onBookRide: (RideEntity, Int) -> Unit,
    onOpenChat: (RideEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var seatsToBook by remember { mutableStateOf(1) }
    var bookingConfirmed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.get("ride_details", language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Route Map Preview Canvas Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            cornerRadius = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Background Simulated Map Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    val startX = size.width * 0.2f
                    val startY = size.height * 0.5f
                    val endX = size.width * 0.8f
                    val endY = size.height * 0.5f

                    // Route line
                    drawLine(
                        color = TrueBlue,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 6f,
                        pathEffect = pathEffect
                    )

                    // Origin Pin Circle
                    drawCircle(
                        color = TrueBlue,
                        radius = 16f,
                        center = Offset(startX, startY)
                    )

                    // Destination Pin Circle
                    drawCircle(
                        color = Color(0xFFEF4444),
                        radius = 16f,
                        center = Offset(endX, endY)
                    )
                }

                // Origin City Label
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TrueBlue)
                    Text(
                        text = ride.startCity,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Destination City Label
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color(0xFFEF4444))
                    Text(
                        text = ride.endCity,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Time / Distance Badge in middle
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(20.dp),
                    color = TrueBlue,
                    contentColor = Color.White
                ) {
                    Text(
                        text = "${ride.departureTime} • ${ride.duration}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Driver Info Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = AppStrings.get("driver_info", language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(TrueBlueLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = TrueBlue, modifier = Modifier.size(28.dp))
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ride.driverName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (ride.driverVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = TrueBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(
                                text = "⭐ ${ride.driverRating} • ${ride.driverTripCount} ${AppStrings.get("trip_count", language)}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Direct Chat Button
                    OutlinedButton(
                        onClick = { onOpenChat(ride) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("chat_with_driver_btn")
                    ) {
                        Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("messages", language), fontSize = 12.sp)
                    }
                }
            }
        }

        // Vehicle Details Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = AppStrings.get("car_info", language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = TrueBlue)
                        Text(text = ride.carModel, fontWeight = FontWeight.Medium)
                    }
                    Text(text = ride.carColor, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Pin, contentDescription = null, tint = TrueBlue)
                        Text(text = "رقم اللوحة:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = ride.carPlate, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Amenities & Rules Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = AppStrings.get("amenities", language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ride.allowsLuggage) {
                        AssistChip(
                            onClick = {},
                            label = { Text(AppStrings.get("luggage_allowed", language)) },
                            leadingIcon = { Icon(Icons.Filled.Luggage, contentDescription = null) }
                        )
                    }
                    if (ride.acceptWallet) {
                        AssistChip(
                            onClick = {},
                            label = { Text(AppStrings.get("wallet_payment", language)) },
                            leadingIcon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) }
                        )
                    }
                }
            }
        }

        // Booking Card & Action
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = AppStrings.get("passengers", language),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { if (seatsToBook > 1) seatsToBook-- }) {
                                Icon(Icons.Filled.Remove, contentDescription = null)
                            }
                            Text(text = "$seatsToBook", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (seatsToBook < ride.availableSeats) seatsToBook++ }) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "المبلغ الإجمالي (نقدًا للسائق)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${ride.pricePerSeat * seatsToBook}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TrueBlue
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        Text(
                            text = "الدفع نقدًا (Cash) مباشرة للكابتن عند بدء الرحلة. لا يتم خصم نقاط من محفظتك.",
                            fontSize = 11.sp,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (bookingConfirmed) {
                    Surface(
                        color = Color(0xFFD1FAE5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = AppStrings.get("booking_success", language),
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            onBookRide(ride, seatsToBook)
                            bookingConfirmed = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_booking_btn")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("book_now", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
