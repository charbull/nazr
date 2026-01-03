package com.wellbeing.nazr

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.app.AppOpsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.os.Build
import com.wellbeing.nazr.AppPreferences
import com.wellbeing.nazr.R


class BlockingAccessibilityService : AccessibilityService() {

    private var currentForegroundRestrictedApp: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val USAGE_CHECK_INTERVAL_MILLIS = 1000L * 5 // Check every 5 seconds for testing

    companion object {
        var isBlockingScreenCurrentlyActive = false
        private const val NOTIFICATION_CHANNEL_ID = "NazrServiceChannel"
        private const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel() // Ensure channel is created
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY // Ensures service restarts if killed by system
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            // Delete existing channel to ensure fresh creation with updated settings
            manager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID)

            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Nazr Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private val usageCheckRunnable = object : Runnable {
        override fun run() {
            currentForegroundRestrictedApp?.let { packageName ->
                val usageExceeded = com.wellbeing.nazr.isAppUsageExceeded(applicationContext, packageName)
                if (usageExceeded) {
                    if (!BlockingAccessibilityService.isBlockingScreenCurrentlyActive) { // Only launch if not already active
                        BlockingAccessibilityService.isBlockingScreenCurrentlyActive = true // Set flag BEFORE launching activity
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("blocked_app_package_name", packageName)
                        }
                        applicationContext.startActivity(intent)
                    } else {
                    }
                    handler.removeCallbacks(this) // Stop checking once blocked
                    currentForegroundRestrictedApp = null
                } else {
                    // Schedule next check
                    handler.postDelayed(this, USAGE_CHECK_INTERVAL_MILLIS)
                }
            }
        }
    }

    private fun handlePackageChange(packageName: String?) {
        // If blocking screen is active, but MainActivity is not the foreground app,
        // it means the blocking screen was killed or dismissed. Reset the flag.
        if (BlockingAccessibilityService.isBlockingScreenCurrentlyActive && packageName != this.packageName) {
            BlockingAccessibilityService.isBlockingScreenCurrentlyActive = false
        }

        // Early exit if blocking screen is already active
        if (BlockingAccessibilityService.isBlockingScreenCurrentlyActive) {
            return
        }

        // If a restricted app was previously in the foreground, but now another app or system UI is
        if (packageName == null || packageName == this.packageName || packageName.startsWith("com.android.systemui")) {
            currentForegroundRestrictedApp?.let {
                handler.removeCallbacks(usageCheckRunnable)
            }
            currentForegroundRestrictedApp = null
            return
        }

        // Check if the current app is a restricted app
        val selectedApps = AppPreferences.loadSelectedApps(applicationContext)
        if (selectedApps.contains(packageName)) {
            val isUnblocked = AppPreferences.isTemporarilyUnblocked(applicationContext, packageName)
            if (isUnblocked) {
                handler.removeCallbacks(usageCheckRunnable) // Stop handler if temporarily unblocked
                currentForegroundRestrictedApp = null
                return // Don't block if temporarily unblocked
            }

            // Initial check: if already exceeded before this session started, block immediately
            val usageExceeded = com.wellbeing.nazr.isAppUsageExceeded(applicationContext, packageName)
            if (usageExceeded) {
                if (!BlockingAccessibilityService.isBlockingScreenCurrentlyActive) { // Only launch if not already active
                    BlockingAccessibilityService.isBlockingScreenCurrentlyActive = true // Set flag BEFORE launching activity
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("blocked_app_package_name", packageName)
                    }
                    startActivity(intent)
                } else {
                }
                handler.removeCallbacks(usageCheckRunnable) // Stop handler if already blocked
                currentForegroundRestrictedApp = null
                return
            }

            // If not already exceeded, and it's a restricted app, start Handler-based monitoring
            if (packageName != currentForegroundRestrictedApp) {
                // Stop any existing handler for previous app
                handler.removeCallbacks(usageCheckRunnable)
                currentForegroundRestrictedApp = packageName
                handler.postDelayed(usageCheckRunnable, USAGE_CHECK_INTERVAL_MILLIS)
            } else {
            }
        } else {
            // If the current foreground app is NOT a restricted app, stop any existing handler
            currentForegroundRestrictedApp?.let {
                handler.removeCallbacks(usageCheckRunnable)
            }
            currentForegroundRestrictedApp = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            handlePackageChange(packageName)
        }
    }

    override fun onInterrupt() {

        handler.removeCallbacks(usageCheckRunnable) // Ensure handler is stopped on interrupt
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BlockingService", "onServiceConnected: Foreground service started.")
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // We want to receive events from all packages
            packageNames = null
        }
        this.serviceInfo = info

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Nazr is Active")
            .setContentText("Monitoring app usage to help you stay focused.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Changed to HIGH
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (notification == null) {
            Log.e("BlockingService", "Notification object is null, cannot start foreground.")
            return
        }
        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d("BlockingService", "startForeground called successfully.")
        } catch (e: Exception) {
            Log.e("BlockingService", "Error calling startForeground: ${e.message}", e)
        }

        // Check if a restricted app is already in the foreground upon connection
        rootInActiveWindow?.packageName?.toString()?.let { packageName ->
            handlePackageChange(packageName)
        }
        Log.d("BlockingService", "Service connected and initial check performed")

    }

    override fun onDestroy() {

        super.onDestroy()
        handler.removeCallbacks(usageCheckRunnable) // Ensure handler is stopped when service is destroyed
        stopForeground(true) // Remove notification when service is destroyed
    }
}