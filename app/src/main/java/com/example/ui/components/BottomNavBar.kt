package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.*

sealed class NavTab(val route: String, val titleKey: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    object Search : NavTab("search", "search", Icons.Filled.Search, Icons.Outlined.Search)
    object RequestedTrips : NavTab("requested_trips", "requested_trips", Icons.Filled.PinDrop, Icons.Outlined.PinDrop)
    object MyRides : NavTab("my_rides", "my_rides", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar)
    object Publish : NavTab("publish", "publish", Icons.Filled.Add, Icons.Outlined.Add)
    object Messages : NavTab("messages", "messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
    object Wallet : NavTab("wallet", "my_wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    object Admin : NavTab("admin", "super_admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    language: AppLanguage,
    isAdmin: Boolean = false,
    unreadMessagesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    // Symmetrical layout to guarantee the center FAB never overlaps any side items
    val leftTabs = if (isAdmin) {
        listOf(NavTab.Search, NavTab.RequestedTrips, NavTab.MyRides)
    } else {
        listOf(NavTab.Search, NavTab.RequestedTrips)
    }

    val rightTabs = if (isAdmin) {
        listOf(NavTab.Messages, NavTab.Wallet, NavTab.Admin)
    } else {
        listOf(NavTab.MyRides, NavTab.Messages)
    }

    val isDark = isSystemInDarkTheme()
    val navBg = if (isDark) Color(0xFF121B17) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0x33269675) else Color(0x1F1E7A5F)
    val isPublishSelected = currentRoute == NavTab.Publish.route

    // Pulsing subtle glow for elevated '+' button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Navigation Bar Surface with Clean Rounded Elevation
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    spotColor = PrimaryGreen.copy(alpha = 0.24f),
                    ambientColor = PrimaryGreen.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ),
            color = navBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(66.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Tabs
                leftTabs.forEach { tab ->
                    NavTabItem(
                        tab = tab,
                        isSelected = currentRoute == tab.route,
                        language = language,
                        badgeCount = 0,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Dedicated Center Slot for the Elevated Floating '+' FAB
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                )

                // Right Tabs
                rightTabs.forEach { tab ->
                    NavTabItem(
                        tab = tab,
                        isSelected = currentRoute == tab.route,
                        language = language,
                        badgeCount = if (tab == NavTab.Messages) unreadMessagesCount else 0,
                        isAdminBadge = tab == NavTab.Admin,
                        onTabSelected = onTabSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Prominently Elevated Floating '+' Action Button (Raised gracefully above the navbar)
        val fabScale by animateFloatAsState(
            targetValue = if (isPublishSelected) 1.1f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "fab_scale"
        )

        val elevationDp by animateFloatAsState(
            targetValue = if (isPublishSelected) 14f else 8f,
            animationSpec = tween(200),
            label = "fab_elevation"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .scale(fabScale * (if (isPublishSelected) 1f else pulseGlow))
                .size(56.dp)
                .shadow(
                    elevation = elevationDp.dp,
                    shape = CircleShape,
                    spotColor = PrimaryGreen,
                    ambientColor = GoldAccent.copy(alpha = 0.35f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (isPublishSelected) {
                            listOf(Color(0xFF34D399), PrimaryGreen, DarkGreen)
                        } else {
                            listOf(Color(0xFF269675), PrimaryGreen, Color(0xFF0D352B))
                        }
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isPublishSelected) {
                            listOf(GoldAccent, Color.White, GoldAccent)
                        } else {
                            listOf(Color.White, GoldAccent.copy(alpha = 0.85f), Color.White.copy(alpha = 0.8f))
                        }
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 28.dp),
                    onClick = { onTabSelected(NavTab.Publish) }
                )
                .testTag("nav_tab_publish"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = AppStrings.get("publish", language),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun NavTabItem(
    tab: NavTab,
    isSelected: Boolean,
    language: AppLanguage,
    badgeCount: Int = 0,
    isAdminBadge: Boolean = false,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = if (isSelected) tab.filledIcon else tab.outlinedIcon
    
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryGreen else if (tab == NavTab.Admin) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        animationSpec = tween(200),
        label = "tab_tint"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "icon_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = { onTabSelected(tab) }
            )
            .padding(vertical = 2.dp)
            .testTag("nav_tab_${tab.route}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) PrimaryGreen.copy(alpha = 0.12f)
                    else if (tab == NavTab.Admin) GoldAccent.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = ErrorRed,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (badgeCount > 9) "9+" else "$badgeCount",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isAdminBadge) {
                        Badge(
                            containerColor = GoldAccent,
                            contentColor = DarkGreen
                        ) {
                            Text("Admin", fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (tab == NavTab.Admin) "لوحة الأدمن" else AppStrings.get(tab.titleKey, language),
                    tint = tintColor,
                    modifier = Modifier
                        .size(21.dp)
                        .scale(iconScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        val title = when (tab) {
            NavTab.Admin -> "الأدمن"
            NavTab.RequestedTrips -> "الطلبات"
            else -> AppStrings.get(tab.titleKey, language)
        }

        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tintColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}
