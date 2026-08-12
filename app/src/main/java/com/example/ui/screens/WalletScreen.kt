package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue
import com.example.ui.theme.TrueBlueLight

data class PointsPackage(val points: Int, val priceUsd: Double, val textKey: String)

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
        PointsPackage(50, 0.50, "package_50"),
        PointsPackage(100, 0.75, "package_100"),
        PointsPackage(200, 1.75, "package_200"),
        PointsPackage(500, 4.00, "package_500")
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
        // Title
        item {
            Text(
                text = AppStrings.get("my_wallet", language),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Points Header Hero Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = TrueBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = AppStrings.get("current_points", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "$userPoints",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = TrueBlue
                        )
                        Text(
                            text = AppStrings.get("points", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrueBlue,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = { onToggleTopUpModal(true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp)
                            .testTag("open_topup_modal_btn")
                    ) {
                        Icon(Icons.Filled.AddCard, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("topup_balance", language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Referral Card: Invite Friend & Earn 100 Points (Requirements 6 & 7)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth().testTag("invite_friend_referral_card"),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
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
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = TrueBlue)
                            Text(
                                text = "🎁 قم بدعوة صديق واكسب 100 نقطة!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TrueBlue
                            )
                        }

                        Surface(
                            color = TrueBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = userReferralCode,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = TrueBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "عند قيام صديقك بالتسجيل في تطبيق وصلني باستخدام رمز الإحالة الخاص بك، يضاف تلقائياً 100 نقطة لمجموع محفظتك!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val shareMsg = "انضم إلى تطبيق وصلني للرحلات التشاركية واستخدم رمز الإحالة الخاص بي ($userReferralCode) لاحتساب 100 نقطة مجاناً! 🚗💨\n\nحمل التطبيق من الرابط المباشر:\n$appDownloadUrl"
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, shareMsg)
                                        type = "text/plain"
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "مشاركة دعوة صديق واكسب 100 نقطة").apply {
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // Green Accent
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(46.dp).testTag("invite_whatsapp_btn")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة الدعوة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareMsg = "انضم إلى تطبيق وصلني للرحلات التشاركية واستخدم رمز الإحالة الخاص بي ($userReferralCode) لاحتساب 100 نقطة مجاناً! 🚗💨\n\nحمل التطبيق من الرابط المباشر:\n$appDownloadUrl"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Wasalni Invite", shareMsg)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود ورابط الدعوة بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخ النص", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Transaction History Header
        item {
            Text(
                text = AppStrings.get("transaction_history", language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد عمليات شحن أو خصم سابقة.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(transactions) { tx ->
                val isTopUp = tx.type == "TOP_UP"

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isTopUp) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTopUp) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isTopUp) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                            }

                            Column {
                                Text(
                                    text = tx.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isTopUp) "شحن حساب" else "خصم نقاط",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${if (isTopUp) "+" else "-"}${tx.points} ${AppStrings.get("points", language)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (isTopUp) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }

    // Top-Up Packages & Cham Cash Modal BottomSheet / Dialog
    if (showTopUpModal) {
        ModalBottomSheet(
            onDismissRequest = {
                onToggleTopUpModal(false)
                selectedPackage = null
                receiptImagePath = null
                isSubmitted = false
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedPackage == null) {
                    // Step 1: Package Selection
                    Text(
                        text = AppStrings.get("select_package", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    packages.forEach { pkg ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPackage = pkg }
                                .testTag("package_item_${pkg.points}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${pkg.points} ${AppStrings.get("points", language)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = TrueBlue
                                    )
                                    Text(
                                        text = AppStrings.get(pkg.textKey, language),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { selectedPackage = pkg },
                                    colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(AppStrings.get("buy_now", language), fontSize = 13.sp)
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
                        Text(
                            text = AppStrings.get("cham_cash_payment", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { selectedPackage = null }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }

                    // Package Summary Banner
                    Surface(
                        color = TrueBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "الباقة المختارة: ${selectedPackage?.points} نقطة بسعر $${selectedPackage?.priceUsd}",
                            fontWeight = FontWeight.Bold,
                            color = TrueBlue,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Fixed Cham Cash Account Number & Copy Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = AppStrings.get("account_number", language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = shamCashCode,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TrueBlue,
                            modifier = Modifier.testTag("cham_account_number_text")
                        )

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Cham Cash Account", shamCashCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, AppStrings.get("account_copied", language), Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("copy_cham_account_btn")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("copy_account", language), color = Color.White)
                        }
                    }

                    // Instructions
                    Text(
                        text = "يرجى تحويل المبلغ المكتوب أعلاه إلى حساب شام كاش الموحد، ثم التقاط صورة لإشعار الدفع وإرفاقها يدوياً من هاتفك للتأكيد.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Attach Receipt Button
                    OutlinedButton(
                        onClick = { receiptPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_receipt_btn")
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = TrueBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (receiptImagePath != null) "✓ تم إرفاق صورة الإشعار بنجاح" else "إرفاق صورة إشعار الدفع من الهاتف يدوياً",
                            fontWeight = if (receiptImagePath != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (receiptImagePath != null) TrueBlue else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (receiptImagePath != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(90.dp)
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
                                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Column {
                                    Text("معاينة إشعار الدفع المرفق", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TrueBlue)
                                    Text("جاهز للإرسال والمراجعة من الأدمن", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Submit Request Button
                    Button(
                        onClick = {
                            val pkg = selectedPackage ?: return@Button
                            if (receiptImagePath.isNullOrBlank()) {
                                Toast.makeText(context, "يرجى اختيار وإرفاق صورة إشعار الدفع الصادر من شام كاش يدوياً من هاتفك أولاً", Toast.LENGTH_LONG).show()
                            } else {
                                onSubmitTopUpRequest(pkg.points, pkg.priceUsd, receiptImagePath!!)
                                isSubmitted = true
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_topup_request_btn")
                    ) {
                        Text(
                            text = AppStrings.get("submit_request", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // Step 3: Success Confirmation
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = AppStrings.get("request_under_review", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تم إرسال إشعار الدفع للأدمن بنجاح. سيتم إضافة النقاط إلى محفظتك فور التأكيد.",
                            fontSize = 13.sp,
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
