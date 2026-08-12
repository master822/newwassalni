package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.ChatMessageEntity
import com.example.data.model.RideEntity
import com.example.ui.components.RatingDialog
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue
import com.example.ui.theme.TrueBlueLight
import com.example.ui.theme.WarningAmber

data class ConversationItem(
    val ride: RideEntity,
    val contactName: String,
    val contactAvatar: String,
    val contactRole: String, // "سائق" or "راكب"
    val contactRating: Float,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    ride: RideEntity?,
    messages: List<ChatMessageEntity>,
    allRides: List<RideEntity>,
    language: AppLanguage,
    currentUserId: String = "user_current",
    onSelectConversation: (RideEntity) -> Unit,
    onSendMessage: (text: String, imageUri: String?, isLocation: Boolean) -> Unit,
    onSendPaymentReminder: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<String?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }

    val chatPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachedImage = it.toString()
            Toast.makeText(context, "تم اختيار الصورة للدردشة!", Toast.LENGTH_SHORT).show()
        }
    }

    // Build list of active conversations from available rides
    val conversationsList = remember(allRides) {
        if (allRides.isNotEmpty()) {
            allRides.mapIndexed { index, r ->
                ConversationItem(
                    ride = r,
                    contactName = r.driverName,
                    contactAvatar = r.driverAvatar,
                    contactRole = if (r.isWomenOnly) "سائقة (رحلة نسائية)" else "سائق",
                    contactRating = r.driverRating,
                    lastMessage = when (index) {
                        0 -> "أنا بانتظارك عند نقطة الانطلاق في المكان المخصص."
                        1 -> "سيارتي كيا ريو جاهزة باللون الأبيض."
                        2 -> "مرحباً بك، هل معك أمتعة كبيرة؟"
                        else -> "الرحلة مؤكدة غداً صباحاً إن شاء الله."
                    },
                    lastTime = "${8 + index}:15 ص",
                    unreadCount = if (index == 0) 1 else 0
                )
            }
        } else {
            emptyList()
        }
    }

    val filteredConversations = conversationsList.filter {
        it.contactName.contains(searchQuery, ignoreCase = true) ||
                it.ride.startCity.contains(searchQuery, ignoreCase = true) ||
                it.ride.endCity.contains(searchQuery, ignoreCase = true)
    }

    if (ride == null) {
        // Mode 1: Conversations List View
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = AppStrings.get("conversations", language),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = TrueBlue.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${conversationsList.size} محادثات نشطة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrueBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar for conversations
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن اسم أو مدينة...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TrueBlue) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("conversation_search_field")
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد محادثات مطابقة للبحث",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredConversations) { item ->
                        Surface(
                            onClick = { onSelectConversation(item.ride) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversation_item_${item.ride.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar with Status Indicator
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(TrueBlueLight.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = TrueBlue,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .align(Alignment.BottomEnd)
                                    )
                                }

                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.contactName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = item.lastTime,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = TrueBlue.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${item.ride.startCity} ➔ ${item.ride.endCity}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrueBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(12.dp))
                                            Text(" ${item.contactRating}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = item.lastMessage,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                if (item.unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(TrueBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${item.unreadCount}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Mode 2: Chat Conversation View with Driver / Passenger
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Chat Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBackToList) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list")
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TrueBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = TrueBlue)
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = ride.driverName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                Text("${ride.driverRating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "${ride.startCity} ➔ ${ride.endCity} (${ride.departureTime})",
                                fontSize = 11.sp,
                                color = TrueBlue
                            )
                        }
                    }

                    // Action buttons: Phone Call & Rate Driver
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+963988123456"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call", tint = TrueBlue)
                        }

                        IconButton(onClick = { showRatingDialog = true }) {
                            Icon(Icons.Filled.Star, contentDescription = "Rate", tint = WarningAmber)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chat Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // If empty messages, show default welcome message from driver
                val displayMessages = if (messages.isEmpty()) {
                    listOf(
                        ChatMessageEntity(
                            id = "msg_welcome",
                            rideId = ride.id,
                            senderId = ride.driverId,
                            receiverId = "user_default",
                            messageText = "مرحباً! أنا ${ride.driverName} سائق رحلة ${ride.startCity} إلى ${ride.endCity}. تسعدني تلبية رحلتك!"
                        )
                    )
                } else messages

                items(displayMessages) { msg ->
                    val isMe = msg.senderId == "user_default"

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        if (msg.isPaymentReminder) {
                            // Payment Reminder Card
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Payments, contentDescription = null, tint = Color(0xFFD97706))
                                    Text(
                                        text = AppStrings.get("payment_reminder", language),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = if (isMe) TrueBlue else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (msg.imageUri != null) {
                                        Surface(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .size(200.dp, 140.dp)
                                                .padding(bottom = 6.dp)
                                        ) {
                                            AsyncImage(
                                                model = msg.imageUri,
                                                contentDescription = "Chat Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                    }

                                    if (msg.isLocation) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = if (isMe) Color.White else TrueBlue)
                                            Text(
                                                text = "موقع جغرافي مباشر (33.5138, 36.2765)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = msg.messageText,
                                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Attached preview
            if (attachedImage != null) {
                Surface(
                    color = TrueBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                AsyncImage(
                                    model = attachedImage,
                                    contentDescription = "Selected Image",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text("📷 صورة مرفقة من الهاتف جاهزة للإرسال", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TrueBlue)
                        }
                        IconButton(onClick = { attachedImage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = TrueBlue)
                        }
                    }
                }
            }

            // Actions & Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        AssistChip(
                            onClick = { chatPhotoPickerLauncher.launch("image/*") },
                            label = { Text(AppStrings.get("attach_photo", language), fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(13.dp)) }
                        )
                        if (ride?.driverId == currentUserId) {
                            AssistChip(
                                onClick = onSendPaymentReminder,
                                label = { Text("تذكير بالدفع", fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(13.dp)) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text(AppStrings.get("type_message", language)) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field")
                        )

                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank() || attachedImage != null) {
                                    onSendMessage(messageText, attachedImage, false)
                                    messageText = ""
                                    attachedImage = null
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(TrueBlue)
                                .testTag("send_chat_msg_btn")
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = AppStrings.get("send", language),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Rating Dialog
            if (showRatingDialog) {
                RatingDialog(
                    targetUserName = ride.driverName,
                    language = language,
                    onRatingSubmitted = { rating, comment ->
                        Toast.makeText(context, "تم حفظ تقييمك لـ ${ride.driverName} ($rating ⭐)", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showRatingDialog = false }
                )
            }
        }
    }
}
