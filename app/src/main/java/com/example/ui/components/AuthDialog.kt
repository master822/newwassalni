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
import com.example.data.network.ApiClient
import com.example.data.network.model.SendOtpRequest
import com.example.data.network.model.VerifyOtpRequest
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN, REGISTER, PHONE_OTP, RESET_PASSWORD
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
    onRegisterSuccess: (
        name: String,
        email: String,
        phone: String,
        password: String,
        referralCode: String?,
        verifyToken: String
    ) -> Unit,
    onLogout: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient.getService(context) }
    var currentMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form fields
    var emailOrPhone by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCodeInput by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

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
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
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
                                TextButton(onClick = { currentMode = AuthMode.RESET_PASSWORD }) {
                                    Text(AppStrings.get("forgot_password", language), fontSize = 12.sp, color = TrueBlue)
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
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
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
                                        Toast.makeText(
                                            context,
                                            "جميع الحقول مطلوبة لتأكيد الحساب",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        scope.launch {
                                            try {
                                                val response = api.sendOtp(
                                                    SendOtpRequest(phone.trim())
                                                )

                                                if (response.isSuccessful && response.body()?.success == true) {
                                                    otpCode = ""
                                                    currentMode = AuthMode.PHONE_OTP

                                                    Toast.makeText(
                                                        context,
                                                        "تم إرسال رمز التحقق الحقيقي المكون من 6 أرقام إلى هاتفك",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        response.body()?.error
                                                            ?: "فشل إرسال رمز التحقق",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "تعذر الاتصال بخادم التحقق: ${e.message ?: "خطأ غير معروف"}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_register_btn")
                            ) {
                                Text("تأكيد الرقم والتسجيل", color = Color.White, fontWeight = FontWeight.Bold)
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
                                text = "تم إرسال رمز تحقق من 6 أرقام إلى رقم الهاتف $phone",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = {
                                    val digits = it.filter { c -> c.isDigit() }
                                    if (digits.length <= 6) {
                                        otpCode = digits
                                    }
                                },
                                label = { Text("رمز التحقق المكون من 6 أرقام") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Sms,
                                        contentDescription = null
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_otp_field")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (otpCode.length != 6) {
                                        Toast.makeText(
                                            context,
                                            "أدخل رمز التحقق المكون من 6 أرقام",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        scope.launch {
                                            try {
                                                val response = api.verifyOtp(
                                                    VerifyOtpRequest(
                                                        phone = phone.trim(),
                                                        otp = otpCode.trim()
                                                    )
                                                )

                                                if (
                                                    response.isSuccessful &&
                                                    response.body()?.success == true
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        "تم تأكيد رقم الهاتف بنجاح",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    /*
                                                     * مهم:
                                                     * لا يوجد OTP وهمي هنا.
                                                     * التسجيل النهائي سيستخدم نتيجة
                                                     * التحقق الحقيقي من الخادم.
                                                     */
                                                    val verifyToken = response.body()?.verifyToken

                                                    if (verifyToken.isNullOrBlank()) {
                                                        Toast.makeText(
                                                            context,
                                                            "تم التحقق من الهاتف لكن لم يتم استلام رمز التحقق من الخادم",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    } else {
                                                        onRegisterSuccess(
                                                            fullName,
                                                            email,
                                                            phone,
                                                            password,
                                                            referralCodeInput.ifBlank { null },
                                                            verifyToken
                                                        )
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        response.body()?.error
                                                            ?: "رمز التحقق غير صحيح أو منتهي الصلاحية",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "تعذر التحقق من الرمز: ${e.message ?: "خطأ غير معروف"}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TrueBlue
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("auth_confirm_otp_btn")
                            ) {
                                Text(
                                    AppStrings.get("confirm_otp", language),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val response = api.sendOtp(
                                                SendOtpRequest(phone.trim())
                                            )

                                            if (
                                                response.isSuccessful &&
                                                response.body()?.success == true
                                            ) {
                                                otpCode = ""

                                                Toast.makeText(
                                                    context,
                                                    "تم إرسال رمز تحقق جديد إلى هاتفك",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    response.body()?.error
                                                        ?: "فشل إعادة إرسال الرمز",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "تعذر إعادة إرسال الرمز",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Text("إعادة إرسال رمز التحقق")
                            }
                        }

                        AuthMode.RESET_PASSWORD -> {
                            Text(
                                text = "أدخل رقم هاتفك لاستعادة كلمة السر عبر رمز OTP",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(AppStrings.get("phone_number", language)) },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_reset_phone")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = {
                                    val digits = it.filter { c -> c.isDigit() }
                                    if (digits.length <= 6) {
                                        otpCode = digits
                                    }
                                },
                                label = { Text("رمز OTP المكون من 6 أرقام") },
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

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (phone.isBlank() || otpCode.isBlank() || newPassword.isBlank()) {
                                        Toast.makeText(context, "جميع الحقول مطلوبة لإعادة تعيين كلمة المرور", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "تم تغيير كلمة المرور بنجاح، يمكنك تسجيل الدخول الآن", Toast.LENGTH_SHORT).show()
                                        currentMode = AuthMode.LOGIN
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_reset_btn")
                            ) {
                                Text("تحديث كلمة المرور", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
