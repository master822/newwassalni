package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, REGISTER, PHONE_OTP, RESET_PASSWORD_SMS
}

fun normalizeSyrianPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.startsWith("00963") -> digits.removePrefix("00")
        digits.startsWith("963") && digits.length >= 12 -> digits
        digits.startsWith("09") && digits.length == 10 -> "963" + digits.removePrefix("0")
        digits.startsWith("9") && digits.length == 9 -> "963$digits"
        digits.startsWith("9639") -> digits
        else -> digits
    }
}

fun isValidSyrianPhoneNumber(phone: String): Boolean {
    val norm = normalizeSyrianPhoneNumber(phone)
    return norm.startsWith("9639") && norm.length == 12
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    language: AppLanguage,
    isLoggedIn: Boolean = false,
    isMandatory: Boolean = false,
    userName: String = "أحمد المحمد",
    userEmail: String = "ahmed@wasalni.app",
    userPhone: String = "963988123456",
    userPoints: Int = 50,
    onLoginSuccess: (emailOrPhone: String, password: String) -> Unit,
    onRegisterSuccess: (name: String, email: String, phone: String, password: String, referralCode: String?, verifyToken: String?) -> Unit,
    onSendPhoneOtp: (suspend (phone: String) -> Pair<Boolean, String>)? = null,
    onVerifyPhoneOtp: (suspend (phone: String, otp: String) -> Pair<Boolean, String>)? = null,
    onResetPasswordPhone: (suspend (phone: String, otp: String, newPass: String) -> Pair<Boolean, String>)? = null,
    onForgotPasswordEmail: (suspend (email: String) -> Pair<Boolean, String>)? = null,
    onResetPasswordEmail: (suspend (email: String, otp: String, newPass: String) -> Pair<Boolean, String>)? = null,
    onOpenWallet: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form fields
    var emailOrPhone by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var referralCodeInput by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Loading & state indicators
    var isSendingPhoneOtp by remember { mutableStateOf(false) }
    var isVerifyingPhoneOtp by remember { mutableStateOf(false) }
    var phoneOtpSent by remember { mutableStateOf(false) }
    var isSubmittingReset by remember { mutableStateOf(false) }

    BackHandler(enabled = !isMandatory || currentMode != AuthMode.LOGIN) {
        when (currentMode) {
            AuthMode.LOGIN -> {
                if (!isMandatory) onDismiss()
            }
            AuthMode.REGISTER,
            AuthMode.PHONE_OTP,
            AuthMode.RESET_PASSWORD_SMS -> {
                currentMode = AuthMode.LOGIN
                otpCode = ""
                phoneOtpSent = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isMandatory && isLoggedIn) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Banner & Branding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryGreen, DarkGreen)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (currentMode) {
                                    AuthMode.LOGIN -> Icons.Filled.Lock
                                    AuthMode.REGISTER -> Icons.Filled.PersonAdd
                                    AuthMode.PHONE_OTP -> Icons.Filled.VerifiedUser
                                    AuthMode.RESET_PASSWORD_SMS -> Icons.Filled.LockReset
                                },
                                contentDescription = null,
                                tint = GoldAccentLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isLoggedIn) "الملف الشخصي" else when (currentMode) {
                                    AuthMode.LOGIN -> "تسجيل الدخول"
                                    AuthMode.REGISTER -> "حساب جديد"
                                    AuthMode.PHONE_OTP -> "تأكيد رمز SMS"
                                    AuthMode.RESET_PASSWORD_SMS -> "استعادة كلمة المرور"
                                },
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isLoggedIn) "إدارة الحساب والرصيد" else "تطبيق وسلني للسفر التشاركي",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isMandatory) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isLoggedIn) {
                    // Logged in user profile card
                    Surface(
                        color = LightContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(PrimaryGreen, DarkGreen))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = userEmail,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                    Text("رقم الهاتف:", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(userPhone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                    Text("رصيد النقاط:", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("$userPoints نقطة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated "My Wallet" Section
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(GoldAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "قسم محفظتي والرصيد 💳",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "شحن النقاط، سجل العمليات، والمكافآت",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = PrimaryGreen,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "$userPoints نقطة",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "يمكنك إدارة نقاطك لشحن الرصيد وحجز الرحلات بدون نقد، أو كسب نقاط إضافية عبر رمز الدعوة الخاص بك.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Button(
                                onClick = onOpenWallet,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("auth_open_wallet_button")
                            ) {
                                Icon(
                                    Icons.Filled.Payments,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "فتح قسم محفظتي وشحن الرصيد",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج من الحساب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    // AUTHENTICATION FORMS
                    when (currentMode) {
                        AuthMode.LOGIN -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = emailOrPhone,
                                    onValueChange = { emailOrPhone = it },
                                    label = { Text("البريد الإلكتروني أو رقم الهاتف") },
                                    placeholder = { Text("9639xxxxxxxx أو user@domain.com") },
                                    supportingText = { Text("للدخول بالهاتف: اكتب 9639xxxxxxxx", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            if (emailOrPhone.contains("@")) Icons.Filled.Email else Icons.Filled.PhoneAndroid,
                                            contentDescription = null,
                                            tint = PrimaryGreen
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (emailOrPhone.contains("@")) KeyboardType.Email else KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_email_field")
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(AppStrings.get("password", language)) },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryGreen) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور"
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_password_field")
                                )

                                // Forgot Password Link (Phone SMS Only - Email option removed per request)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            val digits = emailOrPhone.filter { it.isDigit() }
                                            if (digits.isNotEmpty()) {
                                                phone = normalizeSyrianPhoneNumber(digits)
                                            }
                                            currentMode = AuthMode.RESET_PASSWORD_SMS
                                        }
                                    ) {
                                        Icon(Icons.Filled.LockReset, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "نسيت كلمة المرور؟ استعادة عبر SMS",
                                            fontSize = 12.sp,
                                            color = PrimaryGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val trimmedId = emailOrPhone.trim()
                                        if (trimmedId.isBlank() || password.isBlank()) {
                                            Toast.makeText(context, "يرجى تعبئة كافة الحقول المطلوبة", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val finalIdentifier = if (trimmedId.contains("@")) {
                                                trimmedId.lowercase()
                                            } else {
                                                normalizeSyrianPhoneNumber(trimmedId)
                                            }
                                            onLoginSuccess(finalIdentifier, password)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryGreen
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .shadow(4.dp, RoundedCornerShape(16.dp))
                                        .testTag("auth_login_btn")
                                ) {
                                    Icon(Icons.Filled.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        AppStrings.get("login", language),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ليس لديك حساب؟ ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { currentMode = AuthMode.REGISTER }) {
                                        Text(
                                            text = AppStrings.get("register", language),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }
                                }
                            }
                        }

                        AuthMode.REGISTER -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("الاسم الكامل") },
                                    placeholder = { Text("مثال: سامر الحمصي") },
                                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_register_name")
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("البريد الإلكتروني") },
                                    placeholder = { Text("user@domain.com") },
                                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = PrimaryGreen) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_register_email")
                                )

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("رقم الهاتف (9639xxxxxxxx)") },
                                    placeholder = { Text("9639xxxxxxxx") },
                                    supportingText = {
                                        Text(
                                            "الصيغة الموحدة: 9639xxxxxxxx (12 رقماً تبدأ بـ 9639)",
                                            fontSize = 11.sp,
                                            color = if (phone.isNotBlank() && !isValidSyrianPhoneNumber(phone)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = PrimaryGreen) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_register_phone")
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("كلمة المرور (6 أحرف على الأقل)") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryGreen) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_register_pass")
                                )

                                OutlinedTextField(
                                    value = referralCodeInput,
                                    onValueChange = { referralCodeInput = it },
                                    label = { Text("رمز الإحالة (اختياري - 50 نقطة هدية)") },
                                    placeholder = { Text("مثال: WASALNI-100") },
                                    leadingIcon = { Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = GoldAccent) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_register_referral_code")
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                        if (fullName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                                            Toast.makeText(context, "جميع الحقول مطلوبة لإنشاء الحساب", Toast.LENGTH_SHORT).show()
                                        } else if (!email.contains("@")) {
                                            Toast.makeText(context, "يرجى كتابة بريد إلكتروني صحيح", Toast.LENGTH_SHORT).show()
                                        } else if (!isValidSyrianPhoneNumber(normPhone)) {
                                            Toast.makeText(context, "يرجى كتابة رقم الهاتف بالصيغة السورية الموحدة 9639xxxxxxxx (مثال: 963988123456)", Toast.LENGTH_LONG).show()
                                        } else if (password.length < 6) {
                                            Toast.makeText(context, "كلمة المرور يجب أن تكون 6 خانات على الأقل", Toast.LENGTH_SHORT).show()
                                        } else {
                                            phone = normPhone
                                            isSendingPhoneOtp = true
                                            coroutineScope.launch {
                                                val result = onSendPhoneOtp?.invoke(normPhone)
                                                    ?: Pair(true, "تم إرسال رمز التحقق SMS بنجاح")
                                                isSendingPhoneOtp = false
                                                Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                                if (result.first) {
                                                    otpCode = ""
                                                    currentMode = AuthMode.PHONE_OTP
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSendingPhoneOtp,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .shadow(4.dp, RoundedCornerShape(16.dp))
                                        .testTag("auth_submit_register_btn")
                                ) {
                                    if (isSendingPhoneOtp) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جارٍ إرسال رمز SMS...", color = Color.White, fontSize = 14.sp)
                                    } else {
                                        Icon(Icons.Filled.Sms, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("إرسال رمز التحقق SMS والتسجيل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("لديك حساب بالفعل؟ ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { currentMode = AuthMode.LOGIN }) {
                                        Text(
                                            text = AppStrings.get("login", language),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }
                                }
                            }
                        }

                        AuthMode.PHONE_OTP -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = LightContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "📩 تم إرسال رمز التحقق SMS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = PrimaryGreen
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "تم إرسال رمز مؤلف من 6 أرقام إلى هاتفك:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = phone,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("رمز التحقق (6 أرقام)") },
                                    placeholder = { Text("123456") },
                                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryGreen) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 4.sp
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_otp_field")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                        if (otpCode.trim().length != 6) {
                                            Toast.makeText(context, "يرجى كتابة رمز التحقق المؤلف من 6 أرقام", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isVerifyingPhoneOtp = true
                                            coroutineScope.launch {
                                                val verifyRes = onVerifyPhoneOtp?.invoke(normPhone, otpCode.trim())
                                                    ?: Pair(false, "فشل التحقق من رقم الهاتف")
                                                isVerifyingPhoneOtp = false
                                                if (verifyRes.first) {
                                                    val token = verifyRes.second.ifBlank { "verified_sms_$normPhone" }
                                                    onRegisterSuccess(
                                                        fullName.trim(),
                                                        email.trim().lowercase(),
                                                        normPhone,
                                                        password,
                                                        referralCodeInput.trim().ifBlank { null },
                                                        token
                                                    )
                                                } else {
                                                    Toast.makeText(context, verifyRes.second, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isVerifyingPhoneOtp,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .shadow(4.dp, RoundedCornerShape(16.dp))
                                        .testTag("auth_confirm_otp_btn")
                                ) {
                                    if (isVerifyingPhoneOtp) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("جارٍ تأكيد الحساب وإنشاؤه...", color = Color.White)
                                    } else {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تأكيد الرمز وإنشاء الحساب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                            isSendingPhoneOtp = true
                                            coroutineScope.launch {
                                                val res = onSendPhoneOtp?.invoke(normPhone)
                                                    ?: Pair(true, "تمت إعادة إرسال رمز التحقق عبر SMS")
                                                isSendingPhoneOtp = false
                                                Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        enabled = !isSendingPhoneOtp
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إعادة إرسال رمز SMS", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                    }

                                    TextButton(onClick = { currentMode = AuthMode.REGISTER }) {
                                        Text("تعديل البيانات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        AuthMode.RESET_PASSWORD_SMS -> {
                            // SMS-Only Password Reset (Email password reset option deleted)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = LightContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Sms, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                        Text(
                                            "أدخل رقم هاتفك بصيغة 9639xxxxxxxx لاستلام رمز استعادة كلمة المرور عبر SMS",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("رقم الهاتف (9639xxxxxxxx)") },
                                    placeholder = { Text("9639xxxxxxxx") },
                                    supportingText = { Text("مثال: 963988123456", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = PrimaryGreen) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_reset_phone")
                                )

                                if (!phoneOtpSent) {
                                    Button(
                                        onClick = {
                                            val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                            if (!isValidSyrianPhoneNumber(normPhone)) {
                                                Toast.makeText(context, "يرجى كتابة رقم هاتف صحيح بالصيغة 9639xxxxxxxx", Toast.LENGTH_SHORT).show()
                                            } else {
                                                phone = normPhone
                                                isSendingPhoneOtp = true
                                                coroutineScope.launch {
                                                    val res = onSendPhoneOtp?.invoke(normPhone)
                                                        ?: Pair(true, "تم إرسال رمز التحقق عبر SMS")
                                                    isSendingPhoneOtp = false
                                                    Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                                    if (res.first) {
                                                        phoneOtpSent = true
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isSendingPhoneOtp,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .shadow(4.dp, RoundedCornerShape(16.dp))
                                            .testTag("auth_send_sms_reset_btn")
                                    ) {
                                        if (isSendingPhoneOtp) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جارٍ إرسال SMS...", color = Color.White)
                                        } else {
                                            Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("إرسال رمز الاستعادة عبر SMS", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = otpCode,
                                        onValueChange = { if (it.length <= 6) otpCode = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("رمز التحقق SMS (6 أرقام)") },
                                        placeholder = { Text("123456") },
                                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryGreen) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_reset_otp")
                                    )

                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("كلمة المرور الجديدة") },
                                        leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null, tint = PrimaryGreen) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_new_pass")
                                    )

                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("تأكيد كلمة المرور الجديدة") },
                                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryGreen) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                            if (normPhone.isBlank() || otpCode.isBlank() || newPassword.isBlank()) {
                                                Toast.makeText(context, "جميع الحقول مطلوبة لإعادة تعيين كلمة المرور", Toast.LENGTH_SHORT).show()
                                            } else if (newPassword.length < 6) {
                                                Toast.makeText(context, "كلمة المرور يجب أن لا تقل عن 6 أحرف", Toast.LENGTH_SHORT).show()
                                            } else if (confirmPassword.isNotBlank() && newPassword != confirmPassword) {
                                                Toast.makeText(context, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isSubmittingReset = true
                                                coroutineScope.launch {
                                                    val res = onResetPasswordPhone?.invoke(normPhone, otpCode.trim(), newPassword)
                                                        ?: Pair(true, "تم تعيين كلمة المرور بنجاح")
                                                    isSubmittingReset = false
                                                    Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                                    if (res.first) {
                                                        emailOrPhone = normPhone
                                                        currentMode = AuthMode.LOGIN
                                                        phoneOtpSent = false
                                                        otpCode = ""
                                                        newPassword = ""
                                                        confirmPassword = ""
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isSubmittingReset,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .shadow(4.dp, RoundedCornerShape(16.dp))
                                            .testTag("auth_submit_reset_btn")
                                    ) {
                                        if (isSubmittingReset) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جارٍ حفظ كلمة المرور...", color = Color.White)
                                        } else {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تحديث كلمة المرور والدخول", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        TextButton(onClick = { phoneOtpSent = false }) {
                                            Text("إعادة إرسال رمز التحقق SMS", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(onClick = { currentMode = AuthMode.LOGIN }) {
                                        Text("العودة لتسجيل الدخول", fontSize = 13.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
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
