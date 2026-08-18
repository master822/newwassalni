package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppLanguage
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, REGISTER, PHONE_OTP, RESET_PASSWORD
}

enum class ResetPasswordMethod {
    EMAIL_MAILGUN, PHONE_SMS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    language: AppLanguage,
    isLoggedIn: Boolean = false,
    isMandatory: Boolean = false,
    userName: String = "أحمد المحمد",
    userEmail: String = "ahmed@wasalni.app",
    userPhone: String = "+963 988 123 456",
    userPoints: Int = 50,
    onLoginSuccess: (emailOrPhone: String, password: String) -> Unit,
    onRegisterSuccess: (name: String, email: String, phone: String, password: String, referralCode: String?, verifyToken: String?) -> Unit,
    onSendPhoneOtp: (suspend (phone: String) -> Pair<Boolean, String>)? = null,
    onVerifyPhoneOtp: (suspend (phone: String, otp: String) -> Pair<Boolean, String>)? = null,
    onResetPasswordPhone: (suspend (phone: String, otp: String, newPass: String) -> Pair<Boolean, String>)? = null,
    onForgotPasswordEmail: (suspend (email: String) -> Pair<Boolean, String>)? = null,
    onResetPasswordEmail: (suspend (email: String, otp: String, newPass: String) -> Pair<Boolean, String>)? = null,
    onLogout: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var resetMethod by remember { mutableStateOf(ResetPasswordMethod.PHONE_SMS) }

    // Form fields
    var emailOrPhone by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCodeInput by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Loading & state indicators
    var isSendingPhoneOtp by remember { mutableStateOf(false) }
    var isVerifyingPhoneOtp by remember { mutableStateOf(false) }
    var phoneOtpSent by remember { mutableStateOf(false) }

    // Email reset state
    var resetEmail by remember { mutableStateOf("") }
    var emailOtpSent by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isSubmittingReset by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isMandatory && isLoggedIn) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLoggedIn) "حسابك الشخصي" else when (currentMode) {
                            AuthMode.LOGIN -> AppStrings.get("login", language)
                            AuthMode.REGISTER -> AppStrings.get("register", language)
                            AuthMode.PHONE_OTP -> AppStrings.get("verify_phone", language)
                            AuthMode.RESET_PASSWORD -> AppStrings.get("reset_password", language)
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrueBlue
                    )

                    if (!isMandatory) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoggedIn) {
                    // Logged in user view -> Displays Profile and Sign Out Button
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TrueBlue.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = TrueBlue,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = userName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = userEmail,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("رقم الهاتف:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(userPhone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("رصيد النقاط:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$userPoints نقطة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrueBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج من الحساب", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Auth Forms
                    when (currentMode) {
                        AuthMode.LOGIN -> {
                            OutlinedTextField(
                                value = emailOrPhone,
                                onValueChange = { emailOrPhone = it },
                                label = { Text(AppStrings.get("email_or_phone", language)) },
                                placeholder = { Text("example@domain.com أو 0988123456") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = false,
                                maxLines = 2,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 17.sp),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(AppStrings.get("password", language)) },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_field")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { 
                                    resetEmail = if (emailOrPhone.contains("@")) emailOrPhone else ""
                                    currentMode = AuthMode.RESET_PASSWORD 
                                }) {
                                    Text("نسيت كلمة المرور؟ استعادة عبر البريد", fontSize = 12.sp, color = TrueBlue, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (emailOrPhone.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "يرجى تعبئة كافة الحقول", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onLoginSuccess(emailOrPhone.trim(), password)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_login_btn")
                            ) {
                                Text(AppStrings.get("login", language), color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ليس لديك حساب؟ ", fontSize = 13.sp)
                                Text(
                                    text = AppStrings.get("register", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrueBlue,
                                    modifier = Modifier.clickable { currentMode = AuthMode.REGISTER }
                                )
                            }
                        }

                        AuthMode.REGISTER -> {
                            // Register Mode
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text(AppStrings.get("full_name", language)) },
                                leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_register_name")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text(AppStrings.get("email", language)) },
                                placeholder = { Text("example@domain.com") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = false,
                                maxLines = 2,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 17.sp),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_register_email")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(AppStrings.get("phone_number", language)) },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_register_phone")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(AppStrings.get("password", language)) },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_register_pass")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Referral Code Field: 50 points to both new user and referrer!
                            OutlinedTextField(
                                value = referralCodeInput,
                                onValueChange = { referralCodeInput = it },
                                label = { Text("رمز الإحالة (احصل على 50 نقطة هدية)") },
                                placeholder = { Text("مثال: WASALNI-100") },
                                leadingIcon = { Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = TrueBlue) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_register_referral_code")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (fullName.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                                        Toast.makeText(context, "جميع الحقول مطلوبة لتأكيد الحساب", Toast.LENGTH_SHORT).show()
                                    } else if (!email.contains("@")) {
                                        Toast.makeText(context, "يرجى إدخال بريد إلكتروني صحيح", Toast.LENGTH_SHORT).show()
                                    } else if (password.length < 6) {
                                        Toast.makeText(context, "كلمة المرور يجب أن لا تقل عن 6 أحرف", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isSendingPhoneOtp = true
                                        coroutineScope.launch {
                                            val result = onSendPhoneOtp?.invoke(phone.trim())
                                                ?: Pair(true, "تم إرسال رمز التحقق المؤلف من 6 أرقام عبر SMS")
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
                                colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_register_btn")
                            ) {
                                if (isSendingPhoneOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارٍ إرسال SMS التحقق...", color = Color.White)
                                } else {
                                    Icon(Icons.Filled.Sms, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إرسال رمز التحقق SMS والتسجيل", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("لديك حساب بالفعل؟ ", fontSize = 13.sp)
                                Text(
                                    text = AppStrings.get("login", language),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrueBlue,
                                    modifier = Modifier.clickable { currentMode = AuthMode.LOGIN }
                                )
                            }
                        }

                        AuthMode.PHONE_OTP -> {
                            Text(
                                text = "تم إرسال رمز التحقق المؤلف من 6 أرقام عبر رسالة SMS إلى هاتفك $phone",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "يرجى إدخال الرمز المكون من 6 أرقام المستلم على هاتفك لتأكيد حسابك",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { if (it.length <= 6) otpCode = it },
                                label = { Text("رمز التحقق SMS (6 أرقام)") },
                                placeholder = { Text("123456") },
                                leadingIcon = { Icon(Icons.Filled.Sms, contentDescription = null, tint = TrueBlue) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_otp_field")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (otpCode.isBlank() || otpCode.trim().length != 6) {
                                        Toast.makeText(context, "يرجى إدخال رمز التحقق المؤلف من 6 أرقام المستلم عبر SMS", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isVerifyingPhoneOtp = true
                                        coroutineScope.launch {
                                            val verifyRes = onVerifyPhoneOtp?.invoke(phone.trim(), otpCode.trim())
                                                ?: Pair(false, "فشل التحقق من رقم الهاتف")
                                            isVerifyingPhoneOtp = false
                                            if (verifyRes.first) {
                                                onRegisterSuccess(
                                                    fullName.trim(),
                                                    email.trim(),
                                                    phone.trim(),
                                                    password,
                                                    referralCodeInput.trim().ifBlank { null },
                                                    verifyRes.second
                                                )
                                            } else {
                                                Toast.makeText(context, verifyRes.second, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isVerifyingPhoneOtp,
                                colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_confirm_otp_btn")
                            ) {
                                if (isVerifyingPhoneOtp) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارٍ التحقق وتأكيد الحساب...", color = Color.White)
                                } else {
                                    Text(AppStrings.get("confirm_otp", language), color = Color.White, fontWeight = FontWeight.Bold)
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
                                        isSendingPhoneOtp = true
                                        coroutineScope.launch {
                                            val res = onSendPhoneOtp?.invoke(phone.trim())
                                                ?: Pair(true, "تمت إعادة إرسال رمز التحقق")
                                            isSendingPhoneOtp = false
                                            Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isSendingPhoneOtp
                                ) {
                                    Text("إعادة إرسال رمز SMS", fontSize = 12.sp, color = TrueBlue)
                                }

                                TextButton(onClick = { currentMode = AuthMode.REGISTER }) {
                                    Text("تعديل البيانات", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        AuthMode.RESET_PASSWORD -> {
                            // Method Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = resetMethod == ResetPasswordMethod.PHONE_SMS,
                                    onClick = { resetMethod = ResetPasswordMethod.PHONE_SMS },
                                    label = { Text("📱 عبر الهاتف (SMS)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = resetMethod == ResetPasswordMethod.EMAIL_MAILGUN,
                                    onClick = { resetMethod = ResetPasswordMethod.EMAIL_MAILGUN },
                                    label = { Text("📧 عبر البريد (Mailgun)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (resetMethod == ResetPasswordMethod.EMAIL_MAILGUN) {
                                // Mailgun Email Reset
                                Text(
                                    text = "أدخل بريدك الإلكتروني ليصلك رمز OTP لاستعادة كلمة المرور عبر Mailgun",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = resetEmail,
                                    onValueChange = { resetEmail = it },
                                    label = { Text("البريد الإلكتروني المسجل") },
                                    placeholder = { Text("name@example.com") },
                                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = false,
                                    maxLines = 2,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 17.sp),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_reset_email_field")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (!emailOtpSent) {
                                    Button(
                                        onClick = {
                                            if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                                                Toast.makeText(context, "يرجى كتابة بريد إلكتروني صحيح", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isSendingOtp = true
                                                coroutineScope.launch {
                                                    val result = onForgotPasswordEmail?.invoke(resetEmail.trim())
                                                        ?: Pair(true, "تم إرسال رمز التحقق إلى بريدك الإلكتروني")
                                                    isSendingOtp = false
                                                    Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                                    if (result.first) {
                                                        emailOtpSent = true
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isSendingOtp,
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        if (isSendingOtp) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جارٍ الإرسال عبر Mailgun...", color = Color.White)
                                        } else {
                                            Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إرسال رمز التحقق إلى البريد", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Enter OTP and new password
                                    OutlinedTextField(
                                        value = otpCode,
                                        onValueChange = { if (it.length <= 6) otpCode = it },
                                        label = { Text("رمز التحقق OTP (6 أرقام من البريد)") },
                                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("كلمة المرور الجديدة") },
                                        leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("تأكيد كلمة المرور الجديدة") },
                                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            if (otpCode.isBlank() || newPassword.isBlank()) {
                                                Toast.makeText(context, "يرجى إدخال رمز التحقق وكلمة المرور الجديدة", Toast.LENGTH_SHORT).show()
                                            } else if (confirmPassword.isNotBlank() && newPassword != confirmPassword) {
                                                Toast.makeText(context, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isSubmittingReset = true
                                                coroutineScope.launch {
                                                    val result = onResetPasswordEmail?.invoke(resetEmail.trim(), otpCode.trim(), newPassword)
                                                        ?: Pair(true, "تم تعيين كلمة المرور بنجاح")
                                                    isSubmittingReset = false
                                                    Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                                    if (result.first) {
                                                        emailOrPhone = resetEmail
                                                        currentMode = AuthMode.LOGIN
                                                        emailOtpSent = false
                                                        otpCode = ""
                                                        newPassword = ""
                                                        confirmPassword = ""
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isSubmittingReset,
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        if (isSubmittingReset) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جارٍ تحديث كلمة المرور...", color = Color.White)
                                        } else {
                                            Text("تأكيد وحفظ كلمة المرور الجديدة", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(onClick = { emailOtpSent = false }) {
                                        Text("إعادة إرسال رمز التحقق", fontSize = 12.sp, color = TrueBlue)
                                    }
                                }
                            } else {
                                // Phone SMS Reset (via MSGPlus 6-digit SMS)
                                Text(
                                    text = "أدخل رقم هاتفك ليصلك رمز التحقق المؤلف من 6 أرقام عبر رسالة SMS",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text(AppStrings.get("phone_number", language)) },
                                    placeholder = { Text("0988123456") },
                                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_reset_phone")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                if (!phoneOtpSent) {
                                    Button(
                                        onClick = {
                                            if (phone.isBlank()) {
                                                Toast.makeText(context, "يرجى إدخال رقم الهاتف أولاً", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isSendingPhoneOtp = true
                                                coroutineScope.launch {
                                                    val res = onSendPhoneOtp?.invoke(phone.trim())
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
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_send_sms_reset_btn")
                                    ) {
                                        if (isSendingPhoneOtp) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جارٍ إرسال SMS...", color = Color.White)
                                        } else {
                                            Icon(Icons.Filled.Sms, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إرسال رمز OTP عبر SMS", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = otpCode,
                                        onValueChange = { if (it.length <= 6) otpCode = it },
                                        label = { Text("رمز التحقق المستلم عبر SMS (6 أرقام)") },
                                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("auth_reset_otp")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it },
                                        label = { Text("كلمة المرور الجديدة") },
                                        leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("auth_new_pass")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        label = { Text("تأكيد كلمة المرور الجديدة") },
                                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (phone.isBlank() || otpCode.isBlank() || newPassword.isBlank()) {
                                                Toast.makeText(context, "جميع الحقول مطلوبة لإعادة تعيين كلمة المرور", Toast.LENGTH_SHORT).show()
                                            } else if (confirmPassword.isNotBlank() && newPassword != confirmPassword) {
                                                Toast.makeText(context, "كلمتا المرور غير متطابقتين", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isSubmittingReset = true
                                                coroutineScope.launch {
                                                    val res = onResetPasswordPhone?.invoke(phone.trim(), otpCode.trim(), newPassword)
                                                        ?: Pair(true, "تم تغيير كلمة المرور بنجاح")
                                                    isSubmittingReset = false
                                                    Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                                                    if (res.first) {
                                                        emailOrPhone = phone
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
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_reset_btn")
                                    ) {
                                        if (isSubmittingReset) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جارٍ حفظ كلمة المرور...", color = Color.White)
                                        } else {
                                            Text("تحديث كلمة المرور", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    TextButton(onClick = { phoneOtpSent = false }) {
                                        Text("إعادة إرسال رمز التحقق SMS", fontSize = 12.sp, color = TrueBlue)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(onClick = { currentMode = AuthMode.LOGIN }) {
                                Text("العودة لتسجيل الدخول", fontSize = 13.sp, color = TrueBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
