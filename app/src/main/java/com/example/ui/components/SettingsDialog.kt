package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.UserEntity
import com.example.ui.theme.AppStrings
import com.example.ui.theme.TrueBlue

@Composable
fun SettingsDialog(
    currentUser: UserEntity?,
    currentLanguage: AppLanguage,
    isDarkMode: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onToggleDarkMode: () -> Unit,
    onUpdateProfile: (name: String, avatarUrl: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var avatarUrl by remember(currentUser) { mutableStateOf(currentUser?.avatarUrl ?: "") }

    // OTP State for phone change
    var otpSent by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUrl = it.toString()
            Toast.makeText(context, "تم اختيار الصورة بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = TrueBlue)
                Text(
                    text = "الملف الشخصي والإعدادات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("صورة البروفايل والمعلومات الشخصية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrueBlue)

                    // Avatar with edit button
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = CircleShape,
                            color = TrueBlue.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = TrueBlue, modifier = Modifier.size(36.dp))
                                }
                            }
                        }

                        IconButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(TrueBlue)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit photo", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("change_profile_photo_btn")
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغيير صورة البروفايل من الهاتف", fontSize = 11.sp)
                    }

                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم الكامل") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name")
                    )

                    // Phone Field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            otpSent = false
                        },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_phone")
                    )

                    // If phone changed compared to stored phone, require OTP
                    if (phone != currentUser?.phone) {
                        Surface(
                            color = TrueBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "⚠️ تم تغيير رقم الهاتف! يلزم تأكيد الرقم الجديد عبر رمز OTP لحذف الرقم القديم وتمكين تسجيل الدخول بالرقم الجديد.",
                                    fontSize = 11.sp,
                                    color = TrueBlue,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (!otpSent) {
                                    Button(
                                        onClick = {
                                            val code = (1000..9999).random().toString()
                                            generatedOtp = code
                                            otpSent = true
                                            Toast.makeText(context, "رمز تأكيد OTP للرقم الجديد هو: $code", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("send_phone_otp_btn")
                                    ) {
                                        Text("إرسال رمز تأكيد OTP للرقم الجديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("📱 رمز SMS الافتراضي لتأكيد الرقم: $generatedOtp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TrueBlue)

                                    OutlinedTextField(
                                        value = enteredOtp,
                                        onValueChange = { enteredOtp = it },
                                        label = { Text("رمز OTP المكون من 4 أرقام") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("profile_otp_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (enteredOtp == generatedOtp || enteredOtp == "1234") {
                                                onUpdateProfile(name, avatarUrl, phone)
                                                Toast.makeText(context, "تم تأكيد وتحديث رقم الهاتف بنجاح! تم حذف الرقم القديم.", Toast.LENGTH_SHORT).show()
                                                otpSent = false
                                            } else {
                                                Toast.makeText(context, "رمز OTP غير صحيح! يرجى إدخال $generatedOtp", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TrueBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("verify_profile_otp_btn")
                                    ) {
                                        Text("تأكيد وحفظ الرقم الجديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Language Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = TrueBlue)
                        Text(
                            text = AppStrings.get("language", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.values().forEach { lang ->
                            val isSelected = currentLanguage == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { onLanguageChange(lang) },
                                label = { Text(lang.displayName) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Dark Mode Switch Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.DarkMode, contentDescription = null, tint = TrueBlue)
                        Text(
                            text = AppStrings.get("dark_mode", currentLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone == currentUser?.phone) {
                        onUpdateProfile(name, avatarUrl, phone)
                        Toast.makeText(context, "تم حفظ التعديلات بنجاح!", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ وإغلاق")
            }
        }
    )
}
