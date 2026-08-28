package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Collections
import kotlin.math.abs

/**
 * Manages external Android system notifications with full support for:
 * - App icon branding (Large icon in notification drawer + monochrome status bar icon)
 * - Android 8.0+ High Importance Channels (Heads-up banner, sound, vibration, badge)
 * - Android 13+ runtime POST_NOTIFICATIONS permission
 * - Anti-spam deduplication to prevent repeated alerts during rapid background sync loops
 */
object AppNotificationManager {

    private const val TAG = "WassalniNotification"

    const val CHANNEL_MESSAGES = "wassalni_messages"
    const val CHANNEL_RIDES = "wassalni_rides"
    const val CHANNEL_GENERAL = "wassalni_general"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val EXTRA_RIDE_ID = "extra_ride_id"
    const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications"

    private const val PREFS_NAME = "wassalni_notified_cache"
    private const val KEY_NOTIFIED_IDS = "notified_ids_set"

    // In-memory set of already displayed notification IDs
    private val notifiedIds = Collections.synchronizedSet(mutableSetOf<String>())
    private var isInitialized = false

    /**
     * Initializes notification channels and loads notified history
     */
    fun init(context: Context) {
        if (isInitialized) return
        createChannels(context)
        loadCachedIds(context)
        isInitialized = true
    }

    /**
     * Creates modern Android Notification Channels with sound and vibration
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Messages Channel (High Importance Heads-Up)
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "رسائل ومحادثات وصلني",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية عند وصول رسالة جديدة من السائق أو الراكب"
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary_green)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 250)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 2. Rides & Bookings Channel (High Importance)
            val ridesChannel = NotificationChannel(
                CHANNEL_RIDES,
                "تحديثات الرحلات والحجوزات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات قبول الطلب، بدء الرحلة، والوصول"
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary_green)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. General & Wallet Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "تنبيهات عامة والمحفظة",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات شحن الرصيد والإعلانات العامة من الإدارة"
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary_green)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, ridesChannel, generalChannel)
            )
            Log.d(TAG, "Notification channels initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create notification channels", e)
        }
    }

    /**
     * Seeds existing IDs on first sync so historical notifications don't trigger alerts
     */
    fun seedExistingIds(context: Context, ids: Collection<String>) {
        if (ids.isEmpty()) return
        notifiedIds.addAll(ids)
        saveCachedIds(context)
    }

    /**
     * Shows an external system notification on the user's phone with the custom app icon
     */
    fun showSystemNotification(
        context: Context,
        id: String,
        title: String,
        message: String,
        type: String = "SYSTEM",
        rideId: String? = null,
        forceShow: Boolean = false
    ) {
        init(context)

        // Check deduplication
        if (!forceShow && notifiedIds.contains(id)) {
            return
        }

        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification")
                return
            }
        }

        // Check if notifications are enabled globally
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled for this app")
            return
        }

        try {
            // Select channel and category based on notification type
            val (channelId, category, priority) = when (type.uppercase()) {
                "CHAT", "MESSAGE" -> Triple(
                    CHANNEL_MESSAGES,
                    NotificationCompat.CATEGORY_MESSAGE,
                    NotificationCompat.PRIORITY_HIGH
                )
                "BOOKING", "APPROVAL", "RIDE", "REMINDER" -> Triple(
                    CHANNEL_RIDES,
                    NotificationCompat.CATEGORY_EVENT,
                    NotificationCompat.PRIORITY_HIGH
                )
                else -> Triple(
                    CHANNEL_GENERAL,
                    NotificationCompat.CATEGORY_STATUS,
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }

            // Click Intent to open MainActivity and route to relevant view
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NOTIFICATION_ID, id)
                putExtra(EXTRA_NOTIFICATION_TYPE, type)
                putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
                if (!rideId.isNullOrBlank()) {
                    putExtra(EXTRA_RIDE_ID, rideId)
                }
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val requestCode = abs(id.hashCode() % 100000)
            val pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                pendingIntentFlags
            )

            // Get custom app icon bitmap for the LargeIcon display
            val appIconBitmap = getAppIconBitmap(context)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(context, R.color.primary_green))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priority)
                .setCategory(category)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            // Apply high-res app icon as LargeIcon if available
            if (appIconBitmap != null) {
                builder.setLargeIcon(appIconBitmap)
            }

            val notificationIntId = abs(id.hashCode() % 1000000) + 1000
            notificationManagerCompat.notify(notificationIntId, builder.build())

            // Mark as notified
            notifiedIds.add(id)
            saveCachedIds(context)
            Log.d(TAG, "Notification successfully posted: $id | $title")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while showing notification", e)
        } catch (e: Throwable) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    /**
     * Decodes and formats the official Wassalni app icon as a smooth circular/rounded Bitmap
     */
    private fun getAppIconBitmap(context: Context): Bitmap? {
        return try {
            val original = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.app_icon_wasalni_1787735992579
            ) ?: BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

            original?.let { getRoundedCroppedBitmap(it) }
        } catch (e: Throwable) {
            Log.w(TAG, "Could not decode app icon for notification: ${e.message}")
            null
        }
    }

    /**
     * Creates a circular masked bitmap suitable for Android notification large icons
     */
    private fun getRoundedCroppedBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    /**
     * Shows an immediate test notification with the app icon so the user can verify
     * that external notifications appear in their device's notification bar.
     */
    fun showTestNotification(context: Context) {
        val testId = "test_notif_${System.currentTimeMillis()}"
        showSystemNotification(
            context = context,
            id = testId,
            title = "وصلني - إشعار خارجي",
            message = "تم تفعيل وتجربة الإشعارات الخارجية بنجاح! ستصلك التنبيهات مع أيقونة التطبيق مباشرة على هاتفك.",
            type = "SYSTEM",
            forceShow = true
        )
    }

    private fun loadCachedIds(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())
            if (saved != null) {
                notifiedIds.addAll(saved)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error loading cached notification IDs", e)
        }
    }

    private fun saveCachedIds(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Trim to keep only recent 300 IDs to avoid unbounded growth
            val setCopy = synchronized(notifiedIds) {
                if (notifiedIds.size > 300) {
                    notifiedIds.toList().takeLast(250).toSet()
                } else {
                    notifiedIds.toSet()
                }
            }
            prefs.edit().putStringSet(KEY_NOTIFIED_IDS, setCopy).apply()
        } catch (e: Throwable) {
            Log.w(TAG, "Error saving cached notification IDs", e)
        }
    }
}
