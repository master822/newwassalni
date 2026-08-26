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
import com.example.util.AudioPlaybackManager
import com.example.util.AudioRecordManager
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
    val audioRecordManager = remember { AudioRecordManager(context) }
    val audioPlaybackManager = remember { AudioPlaybackManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlaybackManager.stopAudio()
            audioRecordManager.cancelRecording()
        }
    }

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

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioRecordManager.startRecording()
            isRecordingAudio = true
            recordingDurationSeconds = 0
        } else {
            Toast.makeText(context, "يرجى منح إذن الميكروفون لتسجيل الرسائل الصوتية 🎙️", Toast.LENGTH_SHORT).show()
        }
    }

    val isUserAdmin = currentUserId.contains("admin", ignoreCase = true)

    // Build list of active conversations (only those with messages, direct chats, or active rides)
    val conversationsList = remember(allRides, allChatMessages, currentUserId) {
        val list = mutableListOf<ConversationItem>()
        val processedRideIds = mutableSetOf<String>()

        // 1. Process rides from allRides
        for (r in allRides) {
            val isDirectChat = r.id.startsWith("chat_user_")
            val targetUserId = if (isDirectChat) r.id.removePrefix("chat_user_") else ""

            // For regular users: do not show other users' private direct chats with admin
            if (isDirectChat && !isUserAdmin && targetUserId != currentUserId) {
                continue
            }

            val rideMsgs = allChatMessages.filter { it.rideId == r.id }.sortedBy { it.timestamp }
            val hasMsgs = rideMsgs.isNotEmpty()

            if (isDirectChat || hasMsgs || r.driverId == currentUserId) {
                processedRideIds.add(r.id)
                val lastMsg = rideMsgs.lastOrNull()
                val unread = rideMsgs.count {
                    !it.isRead && it.senderId != currentUserId &&
                            (it.receiverId == currentUserId || it.receiverId.isBlank() || (isUserAdmin && it.receiverId == "admin") || it.receiverId == "passenger_id" || it.receiverId == "driver_id")
                }

                val lastMsgTime = if (lastMsg != null) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastMsg.timestamp))
                } else {
                    r.departureDate
                }

                val displayName = when {
                    isDirectChat && !isUserAdmin -> "إدارة وسلني (الدعم الفني) 🛡️"
                    isDirectChat && isUserAdmin -> r.driverName
                    currentUserId == r.driverId -> "ركاب رحلة ${r.startCity} ➔ ${r.endCity}"
                    else -> r.driverName
                }

                val displayAvatar = when {
                    isDirectChat && !isUserAdmin -> ""
                    else -> r.driverAvatar
                }

                val displayRole = when {
                    isDirectChat && !isUserAdmin -> "فريق الإشراف والدعم الفني"
                    isDirectChat && isUserAdmin -> "مستخدم التطبيق"
                    r.isWomenOnly -> "سائقة (رحلة نسائية)"
                    currentUserId == r.driverId -> "أنت (السائق)"
                    else -> "سائق معتمد"
                }

                list.add(
                    ConversationItem(
                        ride = r,
                        contactName = displayName,
                        contactAvatar = displayAvatar,
                        contactRole = displayRole,
                        contactRating = if (isDirectChat && !isUserAdmin) 5.0f else r.driverRating,
                        lastMessage = lastMsg?.messageText ?: "",
                        lastTime = lastMsgTime,
                        unreadCount = unread
                    )
                )
            }
        }

        // 2. Also include direct chats from allChatMessages if not already in allRides
        val remainingChatIds = allChatMessages.map { it.rideId }.distinct().filter { it !in processedRideIds }
        for (chatId in remainingChatIds) {
            if (chatId.startsWith("chat_user_")) {
                val targetUserId = chatId.removePrefix("chat_user_")
                if (!isUserAdmin && targetUserId != currentUserId) continue

                val rideMsgs = allChatMessages.filter { it.rideId == chatId }.sortedBy { it.timestamp }
                val lastMsg = rideMsgs.lastOrNull()
                val unread = rideMsgs.count {
                    !it.isRead && it.senderId != currentUserId &&
                            (it.receiverId == currentUserId || it.receiverId.isBlank() || (isUserAdmin && it.receiverId == "admin"))
                }
                val lastMsgTime = if (lastMsg != null) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastMsg.timestamp))
                } else "الآن"

                val syntheticRide = RideEntity(
                    id = chatId,
                    driverId = if (isUserAdmin) targetUserId else "admin",
                    driverName = if (!isUserAdmin) "إدارة وسلني (الدعم الفني)" else "المستخدم ($targetUserId)",
                    driverAvatar = "",
                    startCity = if (!isUserAdmin) "الدعم الفني" else "محادثة مباشرة",
                    endCity = if (!isUserAdmin) "الإدارة" else "العميل",
                    departureDate = "فوري",
                    departureTime = "",
                    duration = "",
                    pricePerSeat = 0.0,
                    availableSeats = 1,
                    totalSeats = 1,
                    carModel = "دعم وإشراف",
                    carColor = "أزرق",
                    carPlate = "ADMIN",
                    driverRating = 5.0f,
                    driverTripCount = 100,
                    driverVerified = true
                )

                list.add(
                    ConversationItem(
                        ride = syntheticRide,
                        contactName = if (!isUserAdmin) "إدارة وسلني (الدعم الفني) 🛡️" else "مستخدم ($targetUserId)",
                        contactAvatar = "",
                        contactRole = if (!isUserAdmin) "فريق الإشراف والدعم الفني" else "مستخدم التطبيق",
                        contactRating = 5.0f,
                        lastMessage = lastMsg?.messageText ?: "",
                        lastTime = lastMsgTime,
                        unreadCount = unread
                    )
                )
            }
        }

        list.sortedWith(
            compareByDescending<ConversationItem> { it.unreadCount > 0 }
                .thenByDescending { it.unreadCount }
        )
    }

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Unread, 2: Active

    val filteredConversations = remember(conversationsList, searchQuery, selectedFilterTab) {
        conversationsList.filter { item ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.contactName.contains(searchQuery, ignoreCase = true) ||
                        item.ride.startCity.contains(searchQuery, ignoreCase = true) ||
                        item.ride.endCity.contains(searchQuery, ignoreCase = true) ||
                        item.ride.carModel.contains(searchQuery, ignoreCase = true)
            }
            val matchesTab = when (selectedFilterTab) {
                1 -> item.unreadCount > 0
                2 -> !item.ride.id.startsWith("chat_user_")
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    if (ride == null) {
        // ==========================================
        // Mode 1: Conversations List View (قائمة المحادثات فقط بدون عرض المحتوى)
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
                    val totalUnread = conversationsList.sumOf { it.unreadCount }
                    Text(
                        text = if (totalUnread > 0) "لديك $totalUnread رسائل جديدة غير مقروءة 🔴" else "قائمة المحادثات المباشرة مع السائقين والركاب",
                        fontSize = 12.sp,
                        fontWeight = if (totalUnread > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (totalUnread > 0) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
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
                            text = "${conversationsList.size} محادثة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar for conversations
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث عن محادثة بالاسم أو مسار الرحلة...") },
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

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs: All, Unread, Active Rides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalUnread = conversationsList.count { it.unreadCount > 0 }
                FilterChip(
                    selected = selectedFilterTab == 0,
                    onClick = { selectedFilterTab = 0 },
                    label = { Text("الكل (${conversationsList.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedFilterTab == 1,
                    onClick = { selectedFilterTab = 1 },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("غير مقروءة", fontSize = 12.sp)
                            if (totalUnread > 0) {
                                Surface(
                                    color = if (selectedFilterTab == 1) Color.White else ErrorRed,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$totalUnread",
                                        color = if (selectedFilterTab == 1) ErrorRed else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorRed,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedFilterTab == 2,
                    onClick = { selectedFilterTab = 2 },
                    label = { Text("رحلات نشطة", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            text = if (searchQuery.isNotBlank()) "لا توجد محادثات تطابق بحثك" else "لا توجد محادثات في هذا القسم",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "يمكنك فتح أي رحلة وبدء الدردشة والتواصل مع السائق",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredConversations, key = { it.ride.id }) { item ->
                        val hasUnread = item.unreadCount > 0
                        Surface(
                            onClick = { onSelectConversation(item.ride) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (hasUnread) PrimaryGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
                            border = if (hasUnread) BorderStroke(1.2.dp, ErrorRed.copy(alpha = 0.8f)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            tonalElevation = if (hasUnread) 3.dp else 1.dp,
                            shadowElevation = if (hasUnread) 2.dp else 0.5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversation_item_${item.ride.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Contact Avatar with Online Green Dot & Red Unread Badge Dot
                                Box {
                                    if (item.contactAvatar.isNotBlank()) {
                                        AsyncImage(
                                            model = item.contactAvatar,
                                            contentDescription = item.contactName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryGreen.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    // Online indicator (Bottom End)
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                            .align(Alignment.BottomEnd)
                                    )

                                    // Unread indicator dot (Top End)
                                    if (hasUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(ErrorRed)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }

                                // Details column: Contact Name, Trip Route, and Role
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.contactName,
                                            fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (item.contactRating > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Star,
                                                    contentDescription = null,
                                                    tint = WarningAmber,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    " ${item.contactRating}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = WarningAmber
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = TrueBlue.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${item.ride.startCity} ➔ ${item.ride.endCity}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TrueBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = item.contactRole,
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Trailing info: Timestamp, Unread Badge, and Delete action
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.lastTime,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                                        color = if (hasUnread) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (hasUnread) {
                                            Surface(
                                                color = ErrorRed,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${item.unreadCount} جديدة",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { conversationToDelete = item.ride },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("delete_conv_${item.ride.id}")
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "حذف المحادثة",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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

        val displayMessages = remember(messages) {
            messages
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
                            val isDirectChat = ride.id.startsWith("chat_user_")
                            val headerTitle = when {
                                isDirectChat && !isUserAdmin -> "إدارة وسلني (الدعم الفني) 🛡️"
                                isDirectChat && isUserAdmin -> ride.driverName
                                currentUserId == ride.driverId -> "ركاب الرحلة (${ride.startCity})"
                                else -> ride.driverName
                            }
                            val headerSubtitle = when {
                                isDirectChat && !isUserAdmin -> "فريق الإشراف والدعم الفني المباشر"
                                isDirectChat && isUserAdmin -> "محادثة خاصة ومباشرة مع المستخدم"
                                else -> "${ride.startCity} ➔ ${ride.endCity} (${ride.carModel})"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = headerTitle,
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
                                text = headerSubtitle,
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
                    if (displayMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = PrimaryGreen.copy(alpha = 0.12f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(60.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "مرحباً بك في المحادثة!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تواصل الآن مع ${ride.driverName} عبر كتابة رسالة أو إرسال تسجيل صوتي.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    items(displayMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == currentUserId ||
                                (currentUserId.isBlank() && msg.senderId == "user_default") ||
                                (isUserAdmin && (msg.senderId.contains("admin", ignoreCase = true) || msg.senderId == "super_admin")) ||
                                msg.senderId == "user_me"

                        val isDirectChat = ride.id.startsWith("chat_user_")
                        val senderLabel = when {
                            msg.senderId.contains("admin", ignoreCase = true) -> "إدارة وسلني 🛡️"
                            isDirectChat && isUserAdmin -> ride.driverName
                            isDirectChat && !isUserAdmin -> "إدارة وسلني 🛡️"
                            msg.senderId == ride.driverId -> ride.driverName
                            else -> "الطرف الآخر"
                        }

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
                                                text = senderLabel,
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
                                                    if (isPlaying) {
                                                        audioPlaybackManager.stopAudio()
                                                        playingAudioId = null
                                                        audioPlaybackProgress = 0f
                                                    } else {
                                                        playingAudioId = msg.id
                                                        audioPlaybackProgress = 0f
                                                        audioPlaybackManager.playAudio(
                                                            uriString = msg.audioUri ?: "voice_${msg.id}.m4a",
                                                            durationSeconds = msg.audioDurationSeconds.takeIf { it > 0 } ?: 6,
                                                            onProgress = { prog ->
                                                                audioPlaybackProgress = prog
                                                            },
                                                            onCompletion = {
                                                                playingAudioId = null
                                                                audioPlaybackProgress = 0f
                                                            },
                                                            onError = {
                                                                playingAudioId = null
                                                                audioPlaybackProgress = 0f
                                                            }
                                                        )
                                                    }
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
                                                    text = senderLabel,
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
                                        audioRecordManager.cancelRecording()
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
                                    val recorded = audioRecordManager.stopRecording()
                                    isRecordingAudio = false
                                    recordingDurationSeconds = 0
                                    if (recorded != null) {
                                        onSendMessage(
                                            "",
                                            null,
                                            recorded.first,
                                            recorded.second,
                                            false
                                        )
                                        Toast.makeText(context, "تم إرسال الرسالة الصوتية بنجاح 🎙️", Toast.LENGTH_SHORT).show()
                                    }
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
                                        if (audioRecordManager.hasPermission()) {
                                            audioRecordManager.startRecording()
                                            isRecordingAudio = true
                                            recordingDurationSeconds = 0
                                        } else {
                                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
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
