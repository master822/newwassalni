package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    fromCity: String,
    toCity: String,
    date: String,
    passengers: Int,
    suggestedRides: List<RideEntity>,
    language: AppLanguage,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onSwap: () -> Unit,
    onPassengersChange: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onRideClick: (RideEntity) -> Unit,
    onOpenRequestedTrips: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val popularCities = listOf("دمشق", "حلب", "حمص", "اللاذقية", "طرطوس", "حماة", "بيروت", "إسطنبول", "عمان")
    var isSwapped by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(targetValue = if (isSwapped) 180f else 0f, label = "swap_rotate")

    var expandedFromDropdown by remember { mutableStateOf(false) }
    var expandedToDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Requested Trips Banner Card
        if (onOpenRequestedTrips != null) {
            item {
                Surface(
                    onClick = { onOpenRequestedTrips() },
                    shape = RoundedCornerShape(16.dp),
                    color = TrueBlue,
                    modifier = Modifier.fillMaxWidth().testTag("open_requested_trips_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, tint = Color.White)
                            Column {
                                Text("📌 تثبيت طلب رحلة خاصة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("لم تجد رحلة مناسبة؟ ين يمكنك نشر طلبك ليراه جميع السائقين", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        // Hero Banner Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = AppStrings.get("search_rides", language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // From Field
                    Box {
                        OutlinedTextField(
                            value = fromCity,
                            onValueChange = {
                                onFromChange(it)
                            },
                            label = { Text(AppStrings.get("from", language)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Place, contentDescription = null, tint = TrueBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = { expandedFromDropdown = !expandedFromDropdown }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_from_input")
                        )

                        DropdownMenu(
                            expanded = expandedFromDropdown,
                            onDismissRequest = { expandedFromDropdown = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            popularCities.filter { it != toCity }.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city) },
                                    onClick = {
                                        onFromChange(city)
                                        expandedFromDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Swap Icon Button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        IconButton(
                            onClick = {
                                isSwapped = !isSwapped
                                onSwap()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TrueBlue)
                                .rotate(rotationAngle)
                                .testTag("swap_cities_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = AppStrings.get("swap", language),
                                tint = Color.White
                            )
                        }
                    }

                    // To Field
                    Box {
                        OutlinedTextField(
                            value = toCity,
                            onValueChange = {
                                onToChange(it)
                            },
                            label = { Text(AppStrings.get("to", language)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Navigation, contentDescription = null, tint = TrueBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = { expandedToDropdown = !expandedToDropdown }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_to_input")
                        )

                        DropdownMenu(
                            expanded = expandedToDropdown,
                            onDismissRequest = { expandedToDropdown = false },
                            properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            popularCities.filter { it != fromCity }.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city) },
                                    onClick = {
                                        onToChange(city)
                                        expandedToDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Date & Passengers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Chip
                        OutlinedTextField(
                            value = date,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStrings.get("date", language)) },
                            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, tint = TrueBlue) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Passengers Counter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { if (passengers > 1) onPassengersChange(passengers - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = null)
                                }

                                Text(
                                    text = "$passengers ${AppStrings.get("passengers", language)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = { if (passengers < 6) onPassengersChange(passengers + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Large Search Button
                    Button(
                        onClick = onSearchClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("search_submit_btn")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("search_rides", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Popular Quick Cities Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = AppStrings.get("select_city", language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(popularCities) { city ->
                        FilterChip(
                            selected = city == fromCity || city == toCity,
                            onClick = { onToChange(city) },
                            label = { Text(city) },
                            leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // Suggested & Nearby Rides Header
        item {
            Text(
                text = AppStrings.get("suggested_rides", language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Suggested Rides Cards
        items(suggestedRides) { ride ->
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRideClick(ride) },
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Driver Info Row
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(TrueBlueLight.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = TrueBlue)
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ride.driverName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (ride.driverVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Filled.Verified,
                                            contentDescription = "Verified",
                                            tint = TrueBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "⭐ ${ride.driverRating} • ${ride.driverTripCount} ${AppStrings.get("trip_count", language)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "$${ride.pricePerSeat}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TrueBlue
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Route & Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = ride.departureTime,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${ride.startCity} ➔ ${ride.endCity}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }

                        AssistChip(
                            onClick = {},
                            label = { Text("${ride.availableSeats} ${AppStrings.get("seats_left", language)}") },
                            leadingIcon = { Icon(Icons.Filled.EventSeat, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }
    }
}
