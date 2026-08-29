package com.example.service

import android.util.Log
import com.example.data.network.ApiClient
import com.example.data.network.TokenManager
import com.example.data.network.model.FcmTokenRequest
import com.example.util.AppNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class WassalniFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        val tokenManager = TokenManager.getInstance(applicationContext)
        tokenManager.saveFcmToken(token)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (tokenManager.isLoggedIn()) {
                    val api = ApiClient.getService(applicationContext)
                    api.updateFcmToken(FcmTokenRequest(token))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload FCM token to server: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM notification received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: "وصلني - إشعار جديد 🚗"
        val body = notification?.body ?: data["message"] ?: data["body"] ?: ""
        val type = data["type"] ?: "SYSTEM"
        val rideId = data["ride_id"] ?: data["rideId"]
        val notifId = data["id"] ?: UUID.randomUUID().toString()

        if (title.isNotBlank() || body.isNotBlank()) {
            AppNotificationManager.showSystemNotification(
                context = applicationContext,
                id = notifId,
                title = title,
                message = body,
                type = type,
                rideId = rideId,
                forceShow = true
            )
        }
    }

    companion object {
        private const val TAG = "WassalniFCM"
    }
}
