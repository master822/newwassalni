package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.model.AppLanguage
import com.example.ui.components.normalizeSyrianPhoneNumber
import com.example.ui.components.isValidSyrianPhoneNumber
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class ScreenAuthMode {
    LOGIN,
    REGISTER,
    PHONE_OTP,
    RESET_PASSWORD_SMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    language: AppLanguage,
    isDarkMode: Boolean,
    onLoginSuccess: (emailOrPhone: String, password: String) -> Unit,
    onRegisterSuccess: (name: String, email: String, phone: String, password: String, referralCode: String?, verifyToken: String?) -> Unit,
    onSendPhoneOtp: suspend (phone: String) -> Pair<Boolean, String>,
    onVerifyPhoneOtp: suspend (phone: String, otp: String) -> Pair<Boolean, String>,
    onResetPasswordWithPhone: suspend (phone: String, otp: String, newPass: String) -> Pair<Boolean, String>,
    onToggleDarkMode: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf(ScreenAuthMode.LOGIN) }

    // Form inputs
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

    // Loading states
    var isLoading by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }

    // Physical Back Button navigation in Auth
    BackHandler(enabled = currentMode != ScreenAuthMode.LOGIN) {
        when (currentMode) {
            ScreenAuthMode.PHONE_OTP -> currentMode = ScreenAuthMode.REGISTER
            ScreenAuthMode.REGISTER -> currentMode = ScreenAuthMode.LOGIN
            ScreenAuthMode.RESET_PASSWORD_SMS -> currentMode = ScreenAuthMode.LOGIN
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dark mode toggle
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = if (isDarkMode) GoldAccentLight else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Branding Header
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryGreen, DarkGreen)
                            )
                        )
                        .shadow(8.dp, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = "Wasalni Logo",
                        tint = GoldAccentLight,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "وصلني",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryGreen
                )

                Text(
                    text = "شبكة السفر التشاركي الآمن والاقتصادي في سوريا",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Card Container with Tab Buttons
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Segmented Switcher for Login / Register (Only when not in OTP or Reset mode)
                        if (currentMode == ScreenAuthMode.LOGIN || currentMode == ScreenAuthMode.REGISTER) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    Button(
                                        onClick = { currentMode = ScreenAuthMode.LOGIN },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("auth_tab_login"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentMode == ScreenAuthMode.LOGIN) PrimaryGreen else Color.Transparent,
                                            contentColor = if (currentMode == ScreenAuthMode.LOGIN) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        elevation = if (currentMode == ScreenAuthMode.LOGIN) ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp)
                                    ) {
                                        Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    }

                                    Button(
                                        onClick = { currentMode = ScreenAuthMode.REGISTER },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .testTag("auth_tab_register"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (currentMode == ScreenAuthMode.REGISTER) PrimaryGreen else Color.Transparent,
                                            contentColor = if (currentMode == ScreenAuthMode.REGISTER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        elevation = if (currentMode == ScreenAuthMode.REGISTER) ButtonDefaults.buttonElevation(2.dp) else ButtonDefaults.buttonElevation(0.dp)
                                    ) {
                                        Text("حساب جديد", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // MODE 1: LOGIN
                        // ==========================================
                        if (currentMode == ScreenAuthMode.LOGIN) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "مرحباً بعودتك! 👋",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "سجل دخولك لمتابعة وحجز ونشر رحلاتك",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = emailOrPhone,
                                    onValueChange = { emailOrPhone = it },
                                    label = { Text("البريد الإلكتروني أو رقم الهاتف") },
                                    placeholder = { Text("example@domain.com أو 9639xxxxxxxx") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_login_identifier")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("كلمة المرور") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryGreen) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_login_password")
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        currentMode = ScreenAuthMode.RESET_PASSWORD_SMS
                                    }) {
                                        Text(
                                            text = "نسيت كلمة المرور؟ (SMS)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (emailOrPhone.isBlank() || password.isBlank()) {
                                            Toast.makeText(context, "يرجى كتابة البريد/الرقم وكلمة المرور", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isLoading = true
                                            onLoginSuccess(emailOrPhone.trim(), password)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_screen_login_submit"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تسجيل الدخول", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // MODE 2: REGISTER
                        // ==========================================
                        if (currentMode == ScreenAuthMode.REGISTER) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "إنشاء حساب جديد 🚀",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "احصل على 50 نقطة ترحيبية مجاناً في محفظتك!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("الاسم الكامل") },
                                    placeholder = { Text("مثال: سامر الحمصي") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_register_name")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("البريد الإلكتروني") },
                                    placeholder = { Text("name@example.com") },
                                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_register_email")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("رقم الهاتف السوري (9639xxxxxxxx)") },
                                    placeholder = { Text("963988123456") },
                                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_register_phone")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("كلمة المرور (6 محارف فأكثر)") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryGreen) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_register_password")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = referralCodeInput,
                                    onValueChange = { referralCodeInput = it },
                                    label = { Text("كود الإحالة (اختياري +50 نقطة إضافية)") },
                                    placeholder = { Text("WASALNI-100") },
                                    leadingIcon = { Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = GoldAccent) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_register_referral")
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                        if (fullName.isBlank()) {
                                            Toast.makeText(context, "يرجى كتابة الاسم الكامل", Toast.LENGTH_SHORT).show()
                                        } else if (email.isBlank() || !email.contains("@")) {
                                            Toast.makeText(context, "يرجى كتابة بريد إلكتروني صالح", Toast.LENGTH_SHORT).show()
                                        } else if (!isValidSyrianPhoneNumber(normPhone)) {
                                            Toast.makeText(context, "يرجى كتابة رقم سوري صالح بصيغة 9639xxxxxxxx", Toast.LENGTH_LONG).show()
                                        } else if (password.length < 6) {
                                            Toast.makeText(context, "يجب أن تكون كلمة المرور 6 محارف على الأقل", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isSendingOtp = true
                                            coroutineScope.launch {
                                                val res = onSendPhoneOtp(normPhone)
                                                isSendingOtp = false
                                                if (res.first) {
                                                    Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                                    currentMode = ScreenAuthMode.PHONE_OTP
                                                } else {
                                                    Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_screen_register_submit"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    if (isSendingOtp) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تسجيل", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // MODE 3: PHONE OTP
                        // ==========================================
                        if (currentMode == ScreenAuthMode.PHONE_OTP) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
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
                                        Icon(
                                            Icons.Filled.MarkEmailRead,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "تم إرسال رمز التحقق SMS",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = PrimaryGreen
                                        )
                                        Text(
                                            text = "يرجى إدخال رمز التحقق (6 أرقام) المرسل إلى هاتفك:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = normalizeSyrianPhoneNumber(phone.trim()),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("رمز التحقق (6 أرقام)") },
                                    placeholder = { Text("123456") },
                                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryGreen) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 4.sp
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_screen_otp_code")
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                        if (otpCode.trim().length != 6) {
                                            Toast.makeText(context, "يرجى كتابة رمز التحقق المؤلف من 6 أرقام", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isLoading = true
                                            coroutineScope.launch {
                                                val verifyRes = onVerifyPhoneOtp(normPhone, otpCode.trim())
                                                isLoading = false
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("auth_screen_otp_verify_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("تأكيد الرمز وإنشاء الحساب", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                            coroutineScope.launch {
                                                val res = onSendPhoneOtp(normPhone)
                                                Toast.makeText(context, res.second, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("إعادة إرسال الرمز", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                                    }

                                    TextButton(
                                        onClick = {
                                            currentMode = ScreenAuthMode.REGISTER
                                            otpCode = ""
                                        }
                                    ) {
                                        Text("تعديل البيانات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // MODE 4: RESET PASSWORD SMS
                        // ==========================================
                        if (currentMode == ScreenAuthMode.RESET_PASSWORD_SMS) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "استعادة كلمة المرور عبر SMS 🔑",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "أدخل رقم هاتفك لاستلام رمز التحقق وتعيين كلمة مرور جديدة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("رقم الهاتف السوري (9639xxxxxxxx)") },
                                    placeholder = { Text("963988123456") },
                                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                        if (!isValidSyrianPhoneNumber(normPhone)) {
                                            Toast.makeText(context, "يرجى كتابة رقم سوري صالح بصيغة 9639xxxxxxxx", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isSendingOtp = true
                                            coroutineScope.launch {
                                                val res = onSendPhoneOtp(normPhone)
                                                isSendingOtp = false
                                                Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (isSendingOtp) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("إرسال رمز التحقق SMS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("رمز التحقق (6 أرقام)") },
                                    placeholder = { Text("123456") },
                                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryGreen) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("كلمة المرور الجديدة") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("تأكيد كلمة المرور الجديدة") },
                                    leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null, tint = PrimaryGreen) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val normPhone = normalizeSyrianPhoneNumber(phone.trim())
                                        if (otpCode.length != 6) {
                                            Toast.makeText(context, "يرجى كتابة رمز التحقق المكون من 6 أرقام", Toast.LENGTH_SHORT).show()
                                        } else if (newPassword.length < 6) {
                                            Toast.makeText(context, "يجب أن تكون كلمة المرور 6 محارف على الأقل", Toast.LENGTH_SHORT).show()
                                        } else if (newPassword != confirmPassword) {
                                            Toast.makeText(context, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isLoading = true
                                            coroutineScope.launch {
                                                val res = onResetPasswordWithPhone(normPhone, otpCode.trim(), newPassword)
                                                isLoading = false
                                                Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                                if (res.first) {
                                                    currentMode = ScreenAuthMode.LOGIN
                                                    emailOrPhone = normPhone
                                                    password = newPassword
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("تأكيد وتغيير كلمة المرور", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = { currentMode = ScreenAuthMode.LOGIN },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("العودة لتسجيل الدخول", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Trust & Security Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بيانات مشفرة وآمنة • توثيق فوري للهاتف عبر SMS",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
