package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun SearchResultsScreen(
    fromCity: String,
    toCity: String,
    rides: List<RideEntity>,
    language: AppLanguage,
    onRideClick: (RideEntity) -> Unit,
    onBack: () -> Unit,
    onOpenRequestedTrips: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var filterVerifiedOnly by remember { mutableStateOf(false) }
    var filterWomenOnly by remember { mutableStateOf(false) }
    var filterHighRating by remember { mutableStateOf(false) }

    val filteredRides = rides.filter { ride ->
        val matchesRoute = (ride.startCity.contains(fromCity, true) || fromCity.isEmpty()) &&
                (ride.endCity.contains(toCity, true) || toCity.isEmpty())
        val matchesVerified = !filterVerifiedOnly || ride.driverVerified
        val matchesWomen = !filterWomenOnly || ride.isWomenOnly
        val matchesRating = !filterHighRating || ride.driverRating >= 4.5f

        matchesRoute && matchesVerified && matchesWomen && matchesRating
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Route Bar
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
            Column {
                Text(
                    text = "$fromCity ➔ $toCity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${filteredRides.size} ${AppStrings.get("search_rides", language)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Filters Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = filterVerifiedOnly,
                    onClick = { filterVerifiedOnly = !filterVerifiedOnly },
                    label = { Text(AppStrings.get("verified_only", language)) },
                    leadingIcon = { Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = filterWomenOnly,
                    onClick = { filterWomenOnly = !filterWomenOnly },
                    label = { Text(AppStrings.get("women_only", language)) },
                    leadingIcon = { Icon(Icons.Filled.Female, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = filterHighRating,
                    onClick = { filterHighRating = !filterHighRating },
                    label = { Text(AppStrings.get("rating_4plus", language)) },
                    leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        if (filteredRides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = AppStrings.get("no_rides_found", language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "قم بتثبيت رحلتك في الرحلات المطلوبه لعرضها على السائقين",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TrueBlue,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredRides) { ride ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRideClick(ride) }
                            .testTag("ride_result_card_${ride.id}"),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Driver Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PersonPin,
                                        contentDescription = null,
                                        tint = TrueBlue,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = ride.driverName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
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

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${ride.pricePerSeat}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TrueBlue
                                    )
                                    Text(
                                        text = AppStrings.get("per_seat", language),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Route & Duration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${ride.startCity} ➔ ${ride.endCity}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "⏱️ ${ride.departureTime} (${ride.duration})",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "🚗 ${ride.carModel}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Badges & Seats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (ride.isWomenOnly) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(AppStrings.get("women_only", language), fontSize = 11.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFFFCE4EC),
                                                labelColor = Color(0xFFC2185B)
                                            )
                                        )
                                    }
                                    if (ride.allowsLuggage) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(AppStrings.get("luggage_allowed", language), fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Badge(
                                    containerColor = TrueBlue.copy(alpha = 0.15f),
                                    contentColor = TrueBlue,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(
                                        text = "${ride.availableSeats} ${AppStrings.get("seats_left", language)}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
