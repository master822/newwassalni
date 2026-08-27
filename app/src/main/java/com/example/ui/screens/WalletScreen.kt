package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.WalletTransactionEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

data class PointsPackage(val points: Int, val priceUsd: Double, val textKey: String, val isPopular: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    userPoints: Int,
    transactions: List<WalletTransactionEntity>,
    language: AppLanguage,
    showTopUpModal: Boolean,
    shamCashCode: String = "ba64858e96d4ad9c6096948bc2dbc970",
    userReferralCode: String = "WASALNI-100",
    appDownloadUrl: String = "https://wasalni.app/download",
    onToggleTopUpModal: (Boolean) -> Unit,
    onSubmitTopUpRequest: (packagePoints: Int, packagePriceUsd: Double, receiptImagePath: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPackage by remember { mutableStateOf<PointsPackage?>(null) }
    var receiptImagePath by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    val packages = listOf(
        PointsPackage(50, 0.50, "package_50", false),
        PointsPackage(100, 0.75, "package_100", true),
        PointsPackage(200, 1.75, "package_200", false),
        PointsPackage(500, 4.00, "package_500", false)
    )

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            receiptImagePath = it.toString()
            Toast.makeText(context, "تم إرفاق إشعار الدفع بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Bar: Title & Cash Notice
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = AppStrings.get("my_wallet", language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = "نظام المحفظة: الدفع نقدًا (Cash) للسائق، ونقاط المحفظة تُستخدم لرسوم نشر وتثبيت الرحلات والمكافآت الترويجية.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Premium VIP Digital Balance Card (Embossed & Gradient)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = PrimaryGreen.copy(alpha = 0.35f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0F3822),
                                Color(0xFF155E38),
                                Color(0xFF0A2617)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top row: Brand & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("محفظة وصلني VIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("الرصيد الإلكتروني الفعال", color = Color.White.copy(alpha = 0.7f), fontSize = 10.5.sp)
                            }
                        }

                        Surface(
                            color = GoldAccent.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(GoldAccent))
                                Text("حساب موثق", color = GoldAccent, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Balance Display
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = AppStrings.get("current_points", language),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$userPoints",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = AppStrings.get("points", language),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            // Approximate USD value
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "≈ $${String.format(java.util.Locale.US, "%.2f", userPoints * 0.008)} USD",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Bottom Quick Action: Top Up Button
                    Button(
                        onClick = { onToggleTopUpModal(true) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = Color(0xFF0A2617)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("open_topup_modal_btn")
                    ) {
                        Icon(Icons.Filled.AddCard, contentDescription = null, tint = Color(0xFF0A2617), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "شحن نقاط عبر شام كاش (Cham Cash) ⚡",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0A2617)
                        )
                    }
                }
            }
        }

        // 3. Quick Action Cards (4 Grid items)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Card 1: Top Up
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleTopUpModal(true) }
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Text("شحن فوري", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Quick Card 2: Referral code
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val shareMsg = "انضم إلى تطبيق وصلني للرحلات التشاركية واستخدم رمز الإحالة الخاص بي ($userReferralCode) لاحتساب 50 نقطة مجاناً! 🚗💨\n\nحمل التطبيق من الرابط المباشر:\n$appDownloadUrl"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Wasalni Invite", shareMsg)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ كود ورابط الدعوة بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                        }
                        Text("مكافأة 50+", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Quick Card 3: Support
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            Toast.makeText(context, "الدعم الفني متاح عبر قسم المراسلة مع إدارة التطبيق 🛡️", Toast.LENGTH_SHORT).show()
                        }
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                        }
                        Text("مساعدة ودعم", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // 4. Referral Card: Invite Friend & Earn 50 Points
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invite_friend_referral_card")
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "🎁 دعوة صديق = 50 نقطة هدية!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = userReferralCode,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "شارك رمز الإحالة الخاص بك مع أصدقائك، وسيحصل كل منكما على 50 نقطة فور تسجيله في التطبيق!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val shareMsg = "انضم إلى تطبيق وصلني للرحلات التشاركية واستخدم رمز الإحالة الخاص بي ($userReferralCode) لاحتساب 50 نقطة مجاناً! 🚗💨\n\nحمل التطبيق من الرابط المباشر:\n$appDownloadUrl"
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                                        type = "text/plain"
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة دعوة صديق واكسب 50 نقطة").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Wasalni Invite", shareMsg)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ نص ودعوة الإحالة إلى الحافظة!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("invite_whatsapp_btn")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة الدعوة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareMsg = "انضم إلى تطبيق وصلني للرحلات التشاركية واستخدم رمز الإحالة الخاص بي ($userReferralCode) لاحتساب 50 نقطة مجاناً! 🚗💨\n\nحمل التطبيق من الرابط المباشر:\n$appDownloadUrl"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Wasalni Invite", shareMsg)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود ورابط الدعوة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Transaction History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("transaction_history", language),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${transactions.size} عملية",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Text(
                                text = "لا توجد عمليات شحن أو خصم سابقة مسجلة.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(transactions) { tx ->
                val isTopUp = tx.type == "TOP_UP" || tx.type == "REWARD" || tx.points > 0

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isTopUp) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTopUp) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isTopUp) Color(0xFF065F46) else Color(0xFF991B1B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = tx.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = if (isTopUp) "شحن معتمد • شام كاش" else "خصم عمولة نشر رحلة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTopUp) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "${if (isTopUp) "+" else "-"}${Math.abs(tx.points)} نقطة",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (isTopUp) Color(0xFF065F46) else Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modern Cham Cash Top-Up Modal BottomSheet
    if (showTopUpModal) {
        ModalBottomSheet(
            onDismissRequest = {
                onToggleTopUpModal(false)
                selectedPackage = null
                receiptImagePath = null
                isSubmitted = false
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedPackage == null) {
                    // Step 1: Package Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختر باقة النقاط المناسبة 💳",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { onToggleTopUpModal(false) }) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                        }
                    }

                    packages.forEach { pkg ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (pkg.isPopular) PrimaryGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                if (pkg.isPopular) 1.5.dp else 1.dp,
                                if (pkg.isPopular) PrimaryGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPackage = pkg }
                                .testTag("package_item_${pkg.points}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (pkg.isPopular) PrimaryGreen else GoldAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Bolt,
                                            contentDescription = null,
                                            tint = if (pkg.isPopular) Color.White else Color(0xFFB45309),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "${pkg.points} نقطة",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (pkg.isPopular) {
                                                Surface(
                                                    color = GoldAccent,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("الأكثر طلباً ⭐", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A2617), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = "$${pkg.priceUsd} USD • تحويل شام كاش",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { selectedPackage = pkg },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text("اختيار ➔", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (!isSubmitted) {
                    // Step 2: Cham Cash Payment Modal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = { selectedPackage = null },
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "تحويل عبر شام كاش",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Surface(
                            color = PrimaryGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${selectedPackage?.points} نقطة ($${selectedPackage?.priceUsd})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Account Number Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "رقم حساب شام كاش الموحد للتحويل:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = shamCashCode,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen,
                                modifier = Modifier.testTag("cham_account_number_text")
                            )

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Cham Cash Account", shamCashCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, AppStrings.get("account_copied", language), Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("copy_cham_account_btn")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStrings.get("copy_account", language), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }

                    // Instructions
                    Text(
                        text = "1. قم بتحويل المبلغ لحساب شام كاش أعلاه.\n2. التقط لقطة شاشة لإشعار الدفع وأرفقها هنا لتفعيل النقاط فوراً.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    // Attach Receipt Button
                    OutlinedButton(
                        onClick = { receiptPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("upload_receipt_btn")
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (receiptImagePath != null) "✓ تم إرفاق صورة الإشعار بنجاح" else "إرفاق صورة إشعار الدفع من الاستديو 📷",
                            fontWeight = if (receiptImagePath != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (receiptImagePath != null) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp
                        )
                    }

                    if (receiptImagePath != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = receiptImagePath,
                                    contentDescription = "Receipt Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Column {
                                    Text("معاينة إشعار الدفع المرفق", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryGreen)
                                    Text("جاهز للإرسال للاعتماد الفوري من الإدارة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Submit Request Button
                    Button(
                        onClick = {
                            val pkg = selectedPackage ?: return@Button
                            if (receiptImagePath.isNullOrBlank()) {
                                Toast.makeText(context, "يرجى اختيار وإرفاق صورة إشعار الدفع الصادر من شام كاش أولاً", Toast.LENGTH_LONG).show()
                            } else {
                                onSubmitTopUpRequest(pkg.points, pkg.priceUsd, receiptImagePath!!)
                                isSubmitted = true
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_topup_request_btn")
                    ) {
                        Text(
                            text = AppStrings.get("submit_request", language),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Step 3: Success Confirmation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1FAE5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(40.dp))
                        }
                        Text(
                            text = AppStrings.get("request_under_review", language),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "تم إرسال إشعار الدفع للإدارة بنجاح. سيتم إضافة النقاط إلى محفظتك فور التأكيد.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                onToggleTopUpModal(false)
                                selectedPackage = null
                                receiptImagePath = null
                                isSubmitted = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حسناً")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
