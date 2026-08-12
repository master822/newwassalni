package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue

@Composable
fun PublishRideScreen(
    userPoints: Int,
    language: AppLanguage,
    onPublish: (
        startCity: String,
        endCity: String,
        date: String,
        time: String,
        seats: Int,
        price: Double,
        carModel: String,
        carColor: String,
        carPlate: String,
        womenOnly: Boolean,
        luggage: Boolean
    ) -> Boolean,
    onOpenTopUpModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startCity by remember { mutableStateOf("دمشق") }
    var endCity by remember { mutableStateOf("حلب") }
    var date by remember { mutableStateOf("2026-08-15") }
    var time by remember { mutableStateOf("09:00 AM") }
    var availableSeats by remember { mutableStateOf(3) }
    var priceText by remember { mutableStateOf("8.50") }

    // Vehicle Details (New Features)
    var carModel by remember { mutableStateOf("تويوتا كامري 2022") }
    var carColor by remember { mutableStateOf("فضي (Silver)") }
    var carPlate by remember { mutableStateOf("دمشق 892103") }

    var isWomenOnly by remember { mutableStateOf(false) }
    var allowsLuggage by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = AppStrings.get("publish", language),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Points Deduction Note Banner
        Surface(
            color = TrueBlue.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = TrueBlue)
                Column {
                    Text(
                        text = AppStrings.get("publish_cost_note", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrueBlue
                    )
                    Text(
                        text = "${AppStrings.get("current_points", language)}: $userPoints ${AppStrings.get("points", language)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Ride Creation Form Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "بيانات خط السير والوقت",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrueBlue
                )

                // Start City
                OutlinedTextField(
                    value = startCity,
                    onValueChange = { startCity = it },
                    label = { Text(AppStrings.get("start_point", language)) },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TrueBlue) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_start_input")
                )

                // Destination
                OutlinedTextField(
                    value = endCity,
                    onValueChange = { endCity = it },
                    label = { Text(AppStrings.get("destination", language)) },
                    leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, tint = TrueBlue) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_end_input")
                )

                // Date & Time Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(AppStrings.get("date", language)) },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, tint = TrueBlue) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text(AppStrings.get("time", language)) },
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, tint = TrueBlue) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "مواصفات السيارة واللوحة والمقاعد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrueBlue
                )

                // Car Model Input
                OutlinedTextField(
                    value = carModel,
                    onValueChange = { carModel = it },
                    label = { Text("نوع وموديل السيارة (مثال: كيا سيراتو 2021)") },
                    leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = TrueBlue) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_car_model_input")
                )

                // Car Color & Plate Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = carColor,
                        onValueChange = { carColor = it },
                        label = { Text("لون السيارة") },
                        leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null, tint = TrueBlue) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("publish_car_color_input")
                    )

                    OutlinedTextField(
                        value = carPlate,
                        onValueChange = { carPlate = it },
                        label = { Text("رقم اللوحة") },
                        leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null, tint = TrueBlue) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).testTag("publish_car_plate_input")
                    )
                }

                // Available Seats Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("available_seats", language),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { if (availableSeats > 1) availableSeats-- }) {
                            Icon(Icons.Filled.Remove, contentDescription = null)
                        }
                        Text(
                            text = "$availableSeats مقاعد",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrueBlue
                        )
                        IconButton(onClick = { if (availableSeats < 8) availableSeats++ }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                        }
                    }
                }

                // Price Input
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(AppStrings.get("suggested_price", language)) },
                    leadingIcon = { Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = TrueBlue) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_price_input")
                )

                // Toggles Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStrings.get("women_only", language), fontSize = 14.sp)
                    Switch(
                        checked = isWomenOnly,
                        onCheckedChange = { isWomenOnly = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStrings.get("luggage_allowed", language), fontSize = 14.sp)
                    Switch(
                        checked = allowsLuggage,
                        onCheckedChange = { allowsLuggage = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        val price = priceText.toDoubleOrNull() ?: 5.0
                        onPublish(
                            startCity,
                            endCity,
                            date,
                            time,
                            availableSeats,
                            price,
                            carModel,
                            carColor,
                            carPlate,
                            isWomenOnly,
                            allowsLuggage
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("publish_submit_btn")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("publish_button", language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
