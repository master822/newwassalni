package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue
import com.example.ui.theme.TrueBlueLight

sealed class NavTab(val route: String, val titleKey: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    object Search : NavTab("search", "search", Icons.Filled.Search, Icons.Outlined.Search)
    object RequestedTrips : NavTab("requested_trips", "requested_trips", Icons.Filled.PinDrop, Icons.Outlined.PinDrop)
    object Publish : NavTab("publish", "publish", Icons.Filled.Add, Icons.Outlined.Add)
    object MyRides : NavTab("my_rides", "my_rides", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar)
    object Messages : NavTab("messages", "messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
    object Wallet : NavTab("wallet", "my_wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTab.Search,
        NavTab.RequestedTrips,
        NavTab.Publish,
        NavTab.MyRides,
        NavTab.Messages,
        NavTab.Wallet
    )

    val isDark = isSystemInDarkTheme()
    val navBg = if (isDark) Color(0xFF1A1A1A) else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = navBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(62.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route

                if (tab == NavTab.Publish) {
                    // Center prominent "+" button
                    Box(
                        modifier = Modifier
                            .offset(y = (-8).dp)
                            .size(48.dp)
                            .shadow(6.dp, CircleShape, spotColor = TrueBlue)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(TrueBlueLight, TrueBlue)
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 24.dp),
                                onClick = { onTabSelected(tab) }
                            )
                            .testTag("nav_tab_publish"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = AppStrings.get(tab.titleKey, language),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    val icon = if (isSelected) tab.filledIcon else tab.outlinedIcon
                    val tint = if (isSelected) TrueBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = { onTabSelected(tab) }
                            )
                            .testTag("nav_tab_${tab.route}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) TrueBlue.copy(alpha = 0.12f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = AppStrings.get(tab.titleKey, language),
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = AppStrings.get(tab.titleKey, language),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
