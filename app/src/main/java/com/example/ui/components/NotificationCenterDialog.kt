package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.NotificationEntity
import com.example.ui.theme.AppStrings
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TrueBlue
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterDialog(
    notifications: List<NotificationEntity>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onDeleteAllNotifications: () -> Unit,
    onTestNotification: (() -> Unit)? = null
) {
    var selectedNotificationForDetail by remember { mutableStateOf<NotificationEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = AppStrings.get("notifications", language),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "اضغط على أي إشعار لفتحه وقراءته كاملاً",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (notifications.isNotEmpty()) {
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.12f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${notifications.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // External Notification Banner with instant test capability
                if (onTestNotification != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryGreen.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTestNotification() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إشعارات الهاتف الخارجية مفعلة 🔔",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = "انقر لتجربة إرسال إشعار فوري يظهر في شريط هاتفك بأيقونة وصلني",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.NotificationsNone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = AppStrings.get("no_notifications", language),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                        items(
                            items = notifications,
                            key = { it.id }
                        ) { notif ->
                            val icon: ImageVector = when {
                                notif.type.contains("WELCOME", ignoreCase = true) -> Icons.Filled.Celebration
                                notif.type.contains("RIDE", ignoreCase = true) -> Icons.Filled.DirectionsCar
                                notif.type.contains("WALLET", ignoreCase = true) || notif.type.contains("POINTS", ignoreCase = true) -> Icons.Filled.AccountBalanceWallet
                                notif.type.contains("CHAT", ignoreCase = true) -> Icons.Filled.Chat
                                notif.type.contains("RATING", ignoreCase = true) -> Icons.Filled.Star
                                else -> Icons.Filled.NotificationsActive
                            }

                            val iconColor = when {
                                notif.type.contains("WELCOME", ignoreCase = true) -> Color(0xFF8B5CF6)
                                notif.type.contains("RIDE", ignoreCase = true) -> TrueBlue
                                notif.type.contains("WALLET", ignoreCase = true) || notif.type.contains("POINTS", ignoreCase = true) -> PrimaryGreen
                                notif.type.contains("CHAT", ignoreCase = true) -> Color(0xFF0284C7)
                                notif.type.contains("RATING", ignoreCase = true) -> GoldAccent
                                else -> WarningAmber
                            }

                            val formattedDate = remember(notif.timestamp) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("ar"))
                                sdf.format(Date(notif.timestamp))
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedNotificationForDetail = notif }
                                    .testTag("notif_item_${notif.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(iconColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = notif.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Icon(
                                                Icons.Filled.ChevronLeft,
                                                contentDescription = "فتح",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = notif.message,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp,
                                            maxLines = 2
                                        )

                                        Text(
                                            text = formattedDate,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteNotification(notif.id) },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .testTag("delete_notif_${notif.id}")
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "حذف الإشعار",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notifications.isNotEmpty()) {
                    TextButton(
                        onClick = onDeleteAllNotifications,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حذف الكل", fontSize = 13.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("تم", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    )

    // Dedicated Full-View Modal when opening any Notification
    selectedNotificationForDetail?.let { notif ->
        val formattedDate = remember(notif.timestamp) {
            val sdf = SimpleDateFormat("EEEE، dd MMMM yyyy - hh:mm a", Locale("ar"))
            sdf.format(Date(notif.timestamp))
        }

        val icon: ImageVector = when {
            notif.type.contains("WELCOME", ignoreCase = true) -> Icons.Filled.Celebration
            notif.type.contains("RIDE", ignoreCase = true) -> Icons.Filled.DirectionsCar
            notif.type.contains("WALLET", ignoreCase = true) || notif.type.contains("POINTS", ignoreCase = true) -> Icons.Filled.AccountBalanceWallet
            notif.type.contains("CHAT", ignoreCase = true) -> Icons.Filled.Chat
            notif.type.contains("RATING", ignoreCase = true) -> Icons.Filled.Star
            else -> Icons.Filled.NotificationsActive
        }

        val iconColor = when {
            notif.type.contains("WELCOME", ignoreCase = true) -> Color(0xFF8B5CF6)
            notif.type.contains("RIDE", ignoreCase = true) -> TrueBlue
            notif.type.contains("WALLET", ignoreCase = true) || notif.type.contains("POINTS", ignoreCase = true) -> PrimaryGreen
            notif.type.contains("CHAT", ignoreCase = true) -> Color(0xFF0284C7)
            notif.type.contains("RATING", ignoreCase = true) -> GoldAccent
            else -> WarningAmber
        }

        AlertDialog(
            onDismissRequest = { selectedNotificationForDetail = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = notif.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notif.message,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedDate,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedNotificationForDetail = null },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onDeleteNotification(notif.id)
                        selectedNotificationForDetail = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف الإشعار", fontSize = 12.sp)
                }
            }
        )
    }
}
