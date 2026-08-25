package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.ChatMessageEntity
import com.example.data.model.RideEntity
import com.example.ui.components.RatingDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ConversationItem(
    val ride: RideEntity,
    val contactName: String,
    val contactAvatar: String,
    val contactRole: String,
    val contactRating: Float,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int = 0
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    ride: RideEntity?,
    messages: List<ChatMessageEntity>,
    allRides: List<RideEntity>,
    allChatMessages: List<ChatMessageEntity> = emptyList(),
    language: AppLanguage,
    currentUserId: String = "user_current",
    onSelectConversation: (RideEntity) -> Unit,
    onSendMessage: (text: String, imageUri: String?, audioUri: String?, audioDuration: Int, isLocation: Boolean) -> Unit,
    onDeleteConversation: (rideId: String) -> Unit = {},
    onDeleteMessage: (messageId: String) -> Unit = {},
    onSendPaymentReminder: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var attachedImage by remember { mutableStateOf<String?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }

    // Voice Message Recording & Playback States
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var playingAudioId by remember { mutableStateOf<String?>(null) }
    var audioPlaybackProgress by remember { mutableFloatStateOf(0f) }

    // Dialog state for deleting conversation
    var conversationToDelete by remember { mutableStateOf<RideEntity?>(null) }
    var messageToDelete by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var openedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var openedPhotoMessage by remember { mutableStateOf<ChatMessageEntity?>(null) }

    val chatPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            attachedImage = it.toString()
            Toast.makeText(context, "تم إرفاق الصورة للدردشة", Toast.LENGTH_SHORT).show()
        }
    }

    // Build list of active conversations from available rides and dynamic DB messages
    val conversationsList = remember(allRides, allChatMessages) {
        if (allRides.isNotEmpty()) {
            allRides.mapIndexed { index, r ->
                val rideMsgs = allChatMessages.filter { it.rideId == r.id }.sortedBy { it.timestamp }
                val lastMsg = rideMsgs.lastOrNull()
                val lastMsgText = when {
                    lastMsg == null -> when (index % 4) {
                        0 -> "أهلاً بك! جاهز للانطلاق من نقطة التجمع المحددة."
                        1 -> "السيارة مكيفة ومريحة ومجهزة بالكامل."
                        2 -> "مرحباً، هل لديك حقائب أو أمتعة إضافية؟"
                        else -> "موعدنا غداً في الوقت المحدد إن شاء الله."
                    }
                    lastMsg.audioUri != null -> "🎙️ تسجيل صوتي (${lastMsg.audioDurationSeconds} ث)"
                    lastMsg.imageUri != null -> "📷 صورة مرفقة"
                    lastMsg.isLocation -> "📍 مشاركة الموقع الجغرافي"
                    else -> lastMsg.messageText
                }
                val lastMsgTime = if (lastMsg != null) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastMsg.timestamp))
                } else {
                    "${(7 + (index * 2)) % 12 + 1}:30 م"
                }
                val unread = rideMsgs.count { it.receiverId == currentUserId && it.senderId != currentUserId }
                ConversationItem(
                    ride = r,
                    contactName = r.driverName,
                    contactAvatar = r.driverAvatar,
                    contactRole = if (r.isWomenOnly) "سائقة (رحلة نسائية)" else "سائق معتمد",
                    contactRating = r.driverRating,
                    lastMessage = lastMsgText,
                    lastTime = lastMsgTime,
                    unreadCount = if (unread > 0) unread else if (index == 0) 1 else 0
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
        // ==========================================
        // Mode 1: Conversations List View
        // ==========================================
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Title & Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "المحادثات والرسائل",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "تواصل مباشرة وفورياً مع السائقين والركاب",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = PrimaryGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                        )
                        Text(
                            text = "${filteredConversations.size} محادثة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar for conversations
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن محادثة بالاسم أو المدينة...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryGreen) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "مسح البحث", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد محادثات تطابق بحثك" else "لا توجد محادثات حالياً",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "يمكنك فتح أي رحلة وبدء الدردشة مع السائق فوراً",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredConversations, key = { it.ride.id }) { item ->
                        Surface(
                            onClick = { onSelectConversation(item.ride) },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversation_item_${item.ride.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Contact Avatar with Online Indicator
                                Box {
                                    if (item.contactAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = item.contactAvatar,
                                            contentDescription = item.contactName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryGreen.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    // Online green badge
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .align(Alignment.BottomEnd)
                                    )
                                }

                                // Details column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.contactName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = WarningAmber,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    " ${item.contactRating}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WarningAmber
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.lastTime,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Route pill
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = TrueBlue.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.DirectionsCar,
                                                    contentDescription = null,
                                                    tint = TrueBlue,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "${item.ride.startCity} ➔ ${item.ride.endCity}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TrueBlue
                                                )
                                            }
                                        }

                                        Text(
                                            text = item.contactRole,
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Last message snippet
                                    Text(
                                        text = item.lastMessage,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                // Delete Conversation Button
                                IconButton(
                                    onClick = { conversationToDelete = item.ride },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_conv_${item.ride.id}")
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "حذف المحادثة",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dialog for deleting conversation
            if (conversationToDelete != null) {
                AlertDialog(
                    onDismissRequest = { conversationToDelete = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("حذف المحادثة", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Text(
                            text = "هل أنت متأكد من حذف محادثة رحلة ${conversationToDelete?.startCity} إلى ${conversationToDelete?.endCity} مع السائق ${conversationToDelete?.driverName}؟ سيتم مسح جميع الرسائل.",
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                conversationToDelete?.let { r ->
                                    onDeleteConversation(r.id)
                                    Toast.makeText(context, "تم حذف المحادثة بنجاح", Toast.LENGTH_SHORT).show()
                                }
                                conversationToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("نعم، حذف", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { conversationToDelete = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
        }
    } else {
        // ==========================================
        // Mode 2: Active Chat Conversation Screen
        // ==========================================
        val listState = rememberLazyListState()

        val displayMessages = remember(messages, ride) {
            if (messages.isEmpty()) {
                listOf(
                    ChatMessageEntity(
                        id = "msg_welcome_${ride.id}",
                        rideId = ride.id,
                        senderId = ride.driverId,
                        receiverId = currentUserId,
                        messageText = "مرحباً بك! أنا ${ride.driverName}، سائق رحلة ${ride.startCity} إلى ${ride.endCity}. يسعدني الرد على أي استفسار حول نقطة التجمع والأمتعة.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                messages
            }
        }

        // Recording timer effect
        LaunchedEffect(isRecordingAudio) {
            if (isRecordingAudio) {
                recordingDurationSeconds = 0
                while (isRecordingAudio) {
                    kotlinx.coroutines.delay(1000)
                    recordingDurationSeconds++
                }
            }
        }

        // Voice Message interactive playback effect
        LaunchedEffect(playingAudioId) {
            val currentAudio = playingAudioId
            if (currentAudio != null) {
                val matchedMsg = displayMessages.find { it.id == currentAudio }
                val targetDuration = if ((matchedMsg?.audioDurationSeconds ?: 0) > 0) matchedMsg!!.audioDurationSeconds else 6
                val stepTime = 100L
                val totalSteps = (targetDuration * 10).coerceAtLeast(1)
                for (step in 1..totalSteps) {
                    if (playingAudioId != currentAudio) break
                    audioPlaybackProgress = step.toFloat() / totalSteps.toFloat()
                    kotlinx.coroutines.delay(stepTime)
                }
                if (playingAudioId == currentAudio) {
                    playingAudioId = null
                    audioPlaybackProgress = 0f
                }
            } else {
                audioPlaybackProgress = 0f
            }
        }

        // Auto scroll to bottom when messages update
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            // Chat Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBackToList) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }

                        // Avatar
                        Box {
                            if (ride.driverAvatar.isNotBlank()) {
                                AsyncImage(
                                    model = ride.driverAvatar,
                                    contentDescription = ride.driverName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryGreen)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                                    .align(Alignment.BottomEnd)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = ride.driverName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = "موثق",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = "${ride.startCity} ➔ ${ride.endCity} (${ride.carModel})",
                                fontSize = 11.sp,
                                color = TrueBlue,
                                maxLines = 1
                            )
                        }
                    }

                    // Action buttons: Direct Call, Rate, and Delete Conversation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val phone = "+963988123456"
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "اتصال", tint = PrimaryGreen)
                        }

                        IconButton(onClick = { showRatingDialog = true }) {
                            Icon(Icons.Filled.Star, contentDescription = "تقييم", tint = WarningAmber)
                        }

                        IconButton(onClick = { conversationToDelete = ride }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف المحادثة", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Chat Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(displayMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUserId ||
                                (currentUserId.isBlank() && msg.senderId == "user_default") ||
                                msg.senderId == "user_me"

                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val timeStr = timeFormat.format(Date(msg.timestamp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            if (msg.isPaymentReminder) {
                                // Payment Reminder Banner Card
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF59E0B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "تذكير تسوية الدفع",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = msg.messageText,
                                                fontSize = 12.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }
                            } else if (!msg.audioUri.isNullOrBlank() || msg.audioDurationSeconds > 0) {
                                // ==========================================
                                // WhatsApp-style Voice Note Audio Bubble
                                // ==========================================
                                val isPlaying = playingAudioId == msg.id
                                val duration = msg.audioDurationSeconds.takeIf { it > 0 } ?: 6
                                val currentProgress = if (isPlaying) audioPlaybackProgress else 0f
                                val displaySecs = if (isPlaying) ((duration * currentProgress).toInt()).coerceIn(0, duration) else duration
                                val durStr = String.format(Locale.US, "%d:%02d", displaySecs / 60, displaySecs % 60)

                                Surface(
                                    color = if (isMe) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = if (isMe) 18.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 18.dp
                                    ),
                                    shadowElevation = 1.5.dp,
                                    modifier = Modifier
                                        .widthIn(min = 230.dp, max = 310.dp)
                                        .padding(vertical = 3.dp)
                                        .combinedClickable(
                                            onClick = {
                                                playingAudioId = if (isPlaying) null else msg.id
                                            },
                                            onLongClick = { messageToDelete = msg }
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        // Sender name if received
                                        if (!isMe) {
                                            Text(
                                                text = ride.driverName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Play / Pause Circle Button
                                            IconButton(
                                                onClick = {
                                                    playingAudioId = if (isPlaying) null else msg.id
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isMe) Color.White else PrimaryGreen)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل الرسالة الصوتية",
                                                    tint = if (isMe) PrimaryGreen else Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            // Waveform & Info
                                            Column(modifier = Modifier.weight(1f)) {
                                                // Simulated dynamic waveform bars
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(24.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                                                ) {
                                                    val barHeights = listOf(6, 12, 18, 24, 14, 20, 10, 16, 22, 12, 18, 24, 14, 8, 18, 22, 12, 16, 20, 10)
                                                    barHeights.forEachIndexed { idx, barH ->
                                                        val barProgressFraction = idx.toFloat() / barHeights.size
                                                        val isBarPlayed = currentProgress >= barProgressFraction
                                                        val activeColor = if (isMe) Color.White else PrimaryGreen
                                                        val inactiveColor = if (isMe) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(barH.dp)
                                                                .clip(RoundedCornerShape(1.dp))
                                                                .background(if (isBarPlayed) activeColor else inactiveColor)
                                                        )
                                                    }
                                                }

                                                // Duration & Mic Indicator
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = durStr,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isMe) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Mic,
                                                            contentDescription = null,
                                                            tint = if (isMe) Color.White.copy(alpha = 0.85f) else PrimaryGreen,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            "صوتي",
                                                            fontSize = 10.sp,
                                                            color = if (isMe) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            // Delete voice message button
                                            IconButton(
                                                onClick = { messageToDelete = msg },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = "حذف الرسالة الصوتية",
                                                    tint = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Timestamp & Status
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .padding(top = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = timeStr,
                                                fontSize = 10.sp,
                                                color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isMe) {
                                                Icon(
                                                    Icons.Filled.DoneAll,
                                                    contentDescription = "تم التسليم",
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Standard Chat Bubble
                                Column(
                                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                ) {
                                    // Bubble Container
                                    @OptIn(ExperimentalFoundationApi::class)
                                    Surface(
                                        color = if (isMe) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = if (isMe) 18.dp else 4.dp,
                                            bottomEnd = if (isMe) 4.dp else 18.dp
                                        ),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier
                                            .widthIn(max = 290.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (!msg.imageUri.isNullOrBlank()) {
                                                        openedPhotoUrl = msg.imageUri
                                                        openedPhotoMessage = msg
                                                    }
                                                },
                                                onLongClick = {
                                                    messageToDelete = msg
                                                }
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 10.dp
                                            )
                                        ) {
                                            // Sender label for incoming message
                                            if (!isMe) {
                                                Text(
                                                    text = ride.driverName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryGreen,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }

                                            // Attached Image preview (Click to open, Long Click to delete)
                                            if (!msg.imageUri.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .size(240.dp, 160.dp)
                                                        .padding(bottom = 6.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .combinedClickable(
                                                            onClick = {
                                                                openedPhotoUrl = msg.imageUri
                                                                openedPhotoMessage = msg
                                                            },
                                                            onLongClick = {
                                                                messageToDelete = msg
                                                            }
                                                        )
                                                ) {
                                                    Box {
                                                        AsyncImage(
                                                            model = msg.imageUri,
                                                            contentDescription = "صورة مرفقة - انقر لفتحها",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        // Visual Hint Badge for expanding image
                                                        Surface(
                                                            color = Color.Black.copy(alpha = 0.55f),
                                                            shape = CircleShape,
                                                            modifier = Modifier
                                                                .padding(8.dp)
                                                                .size(30.dp)
                                                                .align(Alignment.TopEnd)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    Icons.Filled.ZoomIn,
                                                                    contentDescription = "تكبير الصورة",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Location pin card
                                            if (msg.isLocation) {
                                                Surface(
                                                    color = if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .padding(8.dp)
                                                            .clickable {
                                                                val geoUri = "geo:33.5138,36.2765?q=33.5138,36.2765(موقع التجمع)"
                                                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                                                                context.startActivity(mapIntent)
                                                            },
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.LocationOn,
                                                            contentDescription = null,
                                                            tint = if (isMe) Color.White else Color(0xFFEF4444),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = "موقع التجمع المباشر",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "انقر لفتح الموقع على الخريطة",
                                                                fontSize = 10.sp,
                                                                color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Message text
                                            if (msg.messageText.isNotBlank()) {
                                                Text(
                                                    text = msg.messageText,
                                                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 14.sp,
                                                    lineHeight = 20.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Timestamp & Delivery status
                                            Row(
                                                modifier = Modifier.align(Alignment.End),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = timeStr,
                                                    fontSize = 10.sp,
                                                    color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (isMe) {
                                                    Icon(
                                                        Icons.Filled.DoneAll,
                                                        contentDescription = "تم التسليم",
                                                        tint = Color.White.copy(alpha = 0.9f),
                                                        modifier = Modifier.size(13.dp)
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
            }

            // Attached preview banner
            if (attachedImage != null) {
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                AsyncImage(
                                    model = attachedImage,
                                    contentDescription = "الصورة المحددة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                "📷 صورة محددة وجاهزة للإرسال",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        IconButton(onClick = { attachedImage = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "إلغاء", tint = PrimaryGreen)
                        }
                    }
                }
            }

            // Modern WhatsApp-style Chat Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    if (isRecordingAudio) {
                        // ==========================================
                        // WhatsApp Live Voice Recording Bar
                        // ==========================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Delete / Cancel button (WhatsApp style trash)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    isRecordingAudio = false
                                    recordingDurationSeconds = 0
                                    Toast.makeText(context, "تم إلغاء التسجيل الصوتي", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                IconButton(
                                    onClick = {
                                        isRecordingAudio = false
                                        recordingDurationSeconds = 0
                                        Toast.makeText(context, "تم إلغاء التسجيل الصوتي", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "حذف وإلغاء التسجيل",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text("إلغاء", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }

                            // Live Timer & Animated pulsing waveform
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Pulsing red dot
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = alpha))
                                )

                                val minutes = recordingDurationSeconds / 60
                                val seconds = recordingDurationSeconds % 60
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Animated audio wave bars
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                                ) {
                                    val heights = listOf(8, 16, 24, 12, 28, 18, 10, 22, 14, 26, 12, 20)
                                    heights.forEachIndexed { i, h ->
                                        val dynamicH = remember(recordingDurationSeconds, i) {
                                            ((h * ((recordingDurationSeconds + i) % 4 + 2)) / 4).coerceIn(6, 26)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(dynamicH.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(PrimaryGreen)
                                        )
                                    }
                                }
                            }

                            // Send Voice Message button
                            IconButton(
                                onClick = {
                                    val finalDuration = recordingDurationSeconds.coerceAtLeast(1)
                                    isRecordingAudio = false
                                    recordingDurationSeconds = 0
                                    onSendMessage(
                                        "",
                                        null,
                                        "voice_note_${System.currentTimeMillis()}.m4a",
                                        finalDuration,
                                        false
                                    )
                                    Toast.makeText(context, "تم إرسال الرسالة الصوتية", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen)
                            ) {
                                Icon(
                                    Icons.Filled.Send,
                                    contentDescription = "إرسال التسجيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        // Quick Action Chips (Attach photo, Payment reminder)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            AssistChip(
                                onClick = { chatPhotoPickerLauncher.launch("image/*") },
                                label = { Text("إرفاق صورة", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp)) }
                            )

                            if (ride.driverId == currentUserId || currentUserId == "admin") {
                                AssistChip(
                                    onClick = onSendPaymentReminder,
                                    label = { Text("تذكير بالدفع", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }

                        // Input Text Row + WhatsApp Mic / Send Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = { Text("اكتب رسالتك هنا...") },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field")
                            )

                            if (messageText.isBlank() && attachedImage == null) {
                                // WhatsApp-style Voice Recording Mic button
                                IconButton(
                                    onClick = {
                                        isRecordingAudio = true
                                        recordingDurationSeconds = 0
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen)
                                        .testTag("start_voice_record_btn")
                                ) {
                                    Icon(
                                        Icons.Filled.Mic,
                                        contentDescription = "تسجيل رسالة صوتية",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                // Text/Image Send button
                                IconButton(
                                    onClick = {
                                        if (messageText.isNotBlank() || attachedImage != null) {
                                            onSendMessage(messageText.trim(), attachedImage, null, 0, false)
                                            messageText = ""
                                            attachedImage = null
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen)
                                        .testTag("send_chat_msg_btn")
                                ) {
                                    Icon(
                                        Icons.Filled.Send,
                                        contentDescription = "إرسال",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Message Options / Delete Dialog
            if (messageToDelete != null) {
                val isImageMsg = !messageToDelete?.imageUri.isNullOrBlank()
                val isVoiceMsg = !messageToDelete?.audioUri.isNullOrBlank() || (messageToDelete?.audioDurationSeconds ?: 0) > 0
                AlertDialog(
                    onDismissRequest = { messageToDelete = null },
                    icon = {
                        Icon(
                            imageVector = when {
                                isImageMsg -> Icons.Filled.PhotoLibrary
                                isVoiceMsg -> Icons.Filled.Mic
                                else -> Icons.Filled.DeleteOutline
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = {
                        Text(
                            text = when {
                                isImageMsg -> "حذف الصورة"
                                isVoiceMsg -> "حذف الرسالة الصوتية"
                                else -> "خيارات الرسالة"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = when {
                                isImageMsg -> "هل تريد حذف هذه الصورة من المحادثة؟"
                                isVoiceMsg -> "هل تريد حذف هذا التسجيل الصوتي من المحادثة؟"
                                else -> "هل تريد حذف هذه الرسالة من المحادثة؟"
                            },
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                messageToDelete?.let { msg ->
                                    if (playingAudioId == msg.id) playingAudioId = null
                                    onDeleteMessage(msg.id)
                                    Toast.makeText(
                                        context,
                                        when {
                                            isImageMsg -> "تم حذف الصورة"
                                            isVoiceMsg -> "تم حذف الرسالة الصوتية"
                                            else -> "تم حذف الرسالة"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                messageToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("حذف", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { messageToDelete = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Full-screen interactive Photo Viewer Dialog
            if (openedPhotoUrl != null) {
                Dialog(
                    onDismissRequest = {
                        openedPhotoUrl = null
                        openedPhotoMessage = null
                    },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.96f))
                    ) {
                        // Top Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    openedPhotoUrl = null
                                    openedPhotoMessage = null
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = "عرض الصورة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )

                            IconButton(
                                onClick = {
                                    val msg = openedPhotoMessage
                                    openedPhotoUrl = null
                                    openedPhotoMessage = null
                                    messageToDelete = msg
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "حذف الصورة",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Full Size Image Display
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = openedPhotoUrl,
                                contentDescription = "صورة بالحجم الكامل",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Bottom Tip Bar
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TouchApp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "اضغط مطولاً على أي صورة أو رسالة لحذفها من المحادثة",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Delete Conversation confirmation
            if (conversationToDelete != null) {
                AlertDialog(
                    onDismissRequest = { conversationToDelete = null },
                    title = {
                        Text("حذف المحادثة بالكامل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    },
                    text = {
                        Text("هل أنت متأكد من حذف كامل المحادثة والرسائل الخاصة برحلة ${conversationToDelete?.startCity}؟", fontSize = 13.5.sp)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                conversationToDelete?.let { r ->
                                    onDeleteConversation(r.id)
                                    onBackToList()
                                    Toast.makeText(context, "تم حذف المحادثة", Toast.LENGTH_SHORT).show()
                                }
                                conversationToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("حذف الآن", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { conversationToDelete = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // Rating Dialog
            if (showRatingDialog) {
                RatingDialog(
                    targetUserName = ride.driverName,
                    language = language,
                    onRatingSubmitted = { rating, comment ->
                        Toast.makeText(context, "تم حفظ تقييمك ($rating ⭐)", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showRatingDialog = false }
                )
            }
        }
    }
}
