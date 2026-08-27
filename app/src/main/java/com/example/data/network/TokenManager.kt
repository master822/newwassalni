package com.example.data.network

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wassalni_secure_auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedInFlow = MutableStateFlow(isLoggedIn())
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val _userRoleFlow = MutableStateFlow(getUserRole())
    val userRoleFlow: StateFlow<String> = _userRoleFlow.asStateFlow()

    private val _isImpersonatingFlow = MutableStateFlow(isImpersonating())
    val isImpersonatingFlow: StateFlow<Boolean> = _isImpersonatingFlow.asStateFlow()

    fun saveAuthTokens(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        userName: String,
        userEmail: String,
        userPhone: String,
        userRole: String,
        isImpersonating: Boolean = false
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, userName)
            .putString(KEY_USER_EMAIL, userEmail)
            .putString(KEY_USER_PHONE, userPhone)
            .putString(KEY_USER_ROLE, userRole)
            .putBoolean(KEY_IS_IMPERSONATING, isImpersonating)
            .apply()

        _isLoggedInFlow.value = true
        _userRoleFlow.value = userRole
        _isImpersonatingFlow.value = isImpersonating
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "USER") ?: "USER"

    fun isAdmin(): Boolean {
        if (!isLoggedIn()) return false
        val role = getUserRole()
        return (role == "ADMIN" || role == "SUPER_ADMIN") && !isImpersonating()
    }

    fun isSuperAdmin(): Boolean {
        if (!isLoggedIn()) return false
        val role = getUserRole()
        return role == "SUPER_ADMIN" && !isImpersonating()
    }

    fun isImpersonating(): Boolean = prefs.getBoolean(KEY_IS_IMPERSONATING, false)

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
        _isLoggedInFlow.value = false
        _userRoleFlow.value = "USER"
        _isImpersonatingFlow.value = false
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "jwt_access_token"
        private const val KEY_REFRESH_TOKEN = "jwt_refresh_token"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_NAME = "auth_user_name"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_USER_PHONE = "auth_user_phone"
        private const val KEY_USER_ROLE = "auth_user_role"
        private const val KEY_IS_IMPERSONATING = "auth_is_impersonating"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
