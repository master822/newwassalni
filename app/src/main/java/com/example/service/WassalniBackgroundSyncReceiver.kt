package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.network.ApiClient
import com.example.data.network.TokenManager
import com.example.data.repository.WassalniRepository
import com.example.util.AppNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WassalniBackgroundSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Background sync triggered by action: ${intent?.action}")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val tokenManager = TokenManager.getInstance(context)
                val repository = WassalniRepository(
                    context = context,
                    dao = db.appDao(),
                    api = ApiClient.getService(context),
                    tokenManager = tokenManager
                )

                // Initialize notification manager and channels
                AppNotificationManager.init(context)

                // 1. Sync recent chat messages & notifications
                repository.syncAllChatMessages()
                if (tokenManager.isLoggedIn()) {
                    repository.fetchNotifications()
                }

                Log.d(TAG, "Background sync completed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Background sync warning: ${e.message}")
            } finally {
                // Re-schedule next alarm
                scheduleNextSync(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WassalniBgSync"
        private const val REQUEST_CODE = 8842
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L // Every 15 minutes

        fun schedule(context: Context) = scheduleNextSync(context)

        fun scheduleNextSync(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, WassalniBackgroundSyncReceiver::class.java).apply {
                    action = "com.example.ACTION_BACKGROUND_SYNC"
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
                val triggerAt = System.currentTimeMillis() + SYNC_INTERVAL_MS

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
                Log.d(TAG, "Scheduled next background sync in 15 mins")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule background sync alarm: ${e.message}")
            }
        }
    }
}
