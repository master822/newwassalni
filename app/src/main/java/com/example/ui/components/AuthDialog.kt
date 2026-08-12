package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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

enum class AuthMode {
    LOGIN, REGISTER, PHONE_OTP, RESET_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDialog(
    language: AppLanguage,
    isLoggedIn: Boolean = true,
    isMandatory: Boolean = false,
    userName: String = "أحمد المحمد",
    userEmail: String = "ahmed@wasalni.app",
    userPhone: String = "+963 988 123 456",
    userPoints: Int = 50,
    onLoginSuccess: (email: String, name: String, isAdmin: Boolean) -> Unit,
    onRegisterSuccess: (name: String, email: String, phone: String, password: String, referralCode: String?) -> Unit,
    onLogout: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form fields
    var emailOrPhone by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCodeInput by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var generatedOtpCode by remember { mutableStateOf("") }
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
                                Text("رقم الهاتف:", fontSize = 13.sp)
                                Text(userPhone, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("رصيد النقاط المجانية:", fontSize = 13.sp)
                                Text("$userPoints نقطة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrueBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onLogout()
                            Toast.makeText(context, "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_logout_btn")
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تسجيل الخروج", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Not Logged In View
                    when (currentMode) {
                        AuthMode.LOGIN -> {
                            OutlinedTextField(
                                value = emailOrPhone,
                                onValueChange = { emailOrPhone = it },
                                label = { Text("البريد الإلكتروني أو رقم الهاتف") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

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
                                        val inputEmail = emailOrPhone.trim().lowercase()
                                        val isAdmin = (inputEmail == "mastersniper823@gmail.com" || inputEmail == "mastersniper823@gmil.com") &&
                                                (password == "sniper927MUHAMMAD" || password == "sniper927")

                                        if (isAdmin) {
                                            onLoginSuccess("mastersniper823@gmail.com", "Super Admin", true)
                                            Toast.makeText(context, "أهلاً بك، تم تسجيل الدخول كمشغل رئيسي (الأدمن)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val userName = if (inputEmail.contains("@")) inputEmail.substringBefore("@") else inputEmail
                                            onLoginSuccess(inputEmail, userName, false)
                                            Toast.makeText(context, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
                                        }
                                        onDismiss()
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

                        // Optional Referral Code Field (Requirement 7)
                        OutlinedTextField(
                            value = referralCodeInput,
                            onValueChange = { referralCodeInput = it },
                            label = { Text("رمز الإحالة (اختياري)") },
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
                                } else {
                                    // Generate 4-digit OTP
                                    val generatedCode = (1000..9999).random().toString()
                                    generatedOtpCode = generatedCode
                                    currentMode = AuthMode.PHONE_OTP
                                    Toast.makeText(context, "رمز التأكيد الخاص بك هو: $generatedCode", Toast.LENGTH_LONG).show()
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
                        // Phone OTP Verification Required
                        Text(
                            text = "تم إرسال رمز التحقق المؤلف من 4 أرقام إلى رقم الهاتف $phone",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (generatedOtpCode.isNotEmpty()) {
                            Surface(
                                color = TrueBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "📱 رمز SMS الافتراضي للتأكيد: $generatedOtpCode",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrueBlue,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 4) otpCode = it },
                            label = { Text("رمز التحقق OTP") },
                            leadingIcon = { Icon(Icons.Filled.Sms, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_otp_field")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (otpCode.isBlank() || (otpCode != generatedOtpCode && otpCode != "1234")) {
                                    Toast.makeText(context, "رمز التحقق غير صحيح، يرجى إدخال $generatedOtpCode", Toast.LENGTH_SHORT).show()
                                } else {
                                    onRegisterSuccess(fullName, email, phone, password, referralCodeInput.ifBlank { null })
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_confirm_otp_btn")
                        ) {
                            Text(AppStrings.get("confirm_otp", language), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    AuthMode.RESET_PASSWORD -> {
                        // Password Reset via Phone OTP
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
                            onValueChange = { if (it.length <= 4) otpCode = it },
                            label = { Text("رمز OTP المرسل للهاتف") },
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
                            label = { Text("كلمة السر الجديدة") },
                            leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("auth_reset_newpass")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (phone.isBlank() || otpCode.length < 4 || newPassword.isBlank()) {
                                    Toast.makeText(context, "يرجى تعبئة كافة الحقول بشكل صحيح", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "تمت إعادة تعيين كلمة السر بنجاح! يمكنك الآن تسجيل الدخول", Toast.LENGTH_LONG).show()
                                    currentMode = AuthMode.LOGIN
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_submit_reset_btn")
                        ) {
                            Text("تأكيد كلمة السر الجديدة", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = { currentMode = AuthMode.LOGIN }) {
                            Text("العودة لتسجيل الدخول", color = TrueBlue, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
}
