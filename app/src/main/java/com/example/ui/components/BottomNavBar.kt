package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    val leftTabs = listOf(
        NavTab.Search,
        NavTab.RequestedTrips,
        NavTab.MyRides
    )

    val rightTabs = buildList {
        add(NavTab.Messages)
        add(NavTab.Wallet)
        if (isAdmin) {
            add(NavTab.Admin)
        }
    }

    val isDark = isSystemInDarkTheme()
    val navBg = if (isDark) Color(0xFF141D1A) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0x33269675) else Color(0x1F1E7A5F)
    val isPublishSelected = currentRoute == NavTab.Publish.route

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Navigation Bar Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(22.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), spotColor = PrimaryGreen.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
            color = navBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(68.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
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

                // Center Column (Space under the elevated + button with Title)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(NavTab.Publish) }
                        )
                        .padding(bottom = 6.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = AppStrings.get("publish", language),
                        fontSize = 10.sp,
                        fontWeight = if (isPublishSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPublishSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }

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

        // Prominent Elevated Floating '+' Button in the Center
        val scale by animateFloatAsState(
            targetValue = if (isPublishSelected) 1.08f else 1.0f,
            animationSpec = tween(200),
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
                .offset(y = (-20).dp)
                .scale(scale)
                .size(56.dp)
                .shadow(elevationDp.dp, CircleShape, spotColor = PrimaryGreen)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isPublishSelected) {
                            listOf(Color(0xFF34D399), PrimaryGreen, DarkGreen)
                        } else {
                            listOf(Color(0xFF269675), PrimaryGreen, DarkGreen)
                        }
                    )
                )
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(
                        colors = if (isPublishSelected) {
                            listOf(GoldAccent, Color.White, GoldAccent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.9f), Color(0xFFD4AF37).copy(alpha = 0.6f))
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
            .testTag("nav_tab_${tab.route}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) PrimaryGreen.copy(alpha = 0.14f)
                    else if (tab == NavTab.Admin) GoldAccent.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = ErrorRed,
                            contentColor = Color.White
                        ) {
                            Text(if (badgeCount > 9) "9+" else "$badgeCount", fontSize = 9.sp)
                        }
                    } else if (isAdminBadge) {
                        Badge(
                            containerColor = GoldAccent,
                            contentColor = DarkGreen
                        ) {
                            Text("Admin", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (tab == NavTab.Admin) "لوحة الأدمن" else AppStrings.get(tab.titleKey, language),
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        val title = when (tab) {
            NavTab.Admin -> "الأدمن"
            else -> AppStrings.get(tab.titleKey, language)
        }

        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tintColor,
            maxLines = 1
        )
    }
}
