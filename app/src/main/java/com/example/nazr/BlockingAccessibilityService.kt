package com.example.nazr

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.app.AppOpsManager // Keep if used in MainActivity or AppPreferences
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class BlockingAccessibilityService : AccessibilityService() {

    private var currentForegroundRestrictedApp: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val USAGE_CHECK_INTERVAL_MILLIS = 1000L * 5 // Check every 5 seconds for testing

    private val usageCheckRunnable = object : Runnable {
        override fun run() {
            currentForegroundRestrictedApp?.let { packageName ->
                Log.d("BlockingService", "Handler checking usage for $packageName")
                if (com.example.nazr.isAppUsageExceeded(applicationContext, packageName)) {
                    Log.d("BlockingService", "Usage limit exceeded for $packageName. Triggering blocking screen from Handler.")
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("blocked_app_package_name", packageName)
                    }
                    applicationContext.startActivity(intent)
                    handler.removeCallbacks(this) // Stop checking once blocked
                    currentForegroundRestrictedApp = null
                } else {
                    // Schedule next check
                    handler.postDelayed(this, USAGE_CHECK_INTERVAL_MILLIS)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()

            // If a restricted app was previously in the foreground, but now another app or system UI is
            if (packageName == null || packageName == this.packageName || packageName.startsWith("com.android.systemui")) {
                currentForegroundRestrictedApp?.let {
                    Log.d("BlockingService", "Stopping Handler for $it (app went to background/system UI active).")
                    handler.removeCallbacks(usageCheckRunnable)
                }
                currentForegroundRestrictedApp = null
                return
            }

            // Check if the current app is a restricted app
            val selectedApps = AppPreferences.loadSelectedApps(applicationContext)
            if (selectedApps.contains(packageName)) {
                if (AppPreferences.isTemporarilyUnblocked(applicationContext, packageName)) {
                    Log.d("BlockingService", "App $packageName is temporarily unblocked.")
                    handler.removeCallbacks(usageCheckRunnable) // Stop handler if temporarily unblocked
                    currentForegroundRestrictedApp = null
                    return // Don't block if temporarily unblocked
                }

                // Initial check: if already exceeded before this session started, block immediately
                if (com.example.nazr.isAppUsageExceeded(applicationContext, packageName)) {
                    Log.d("BlockingService", "Usage limit already exceeded for $packageName on launch. Launching blocking screen.")
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("blocked_app_package_name", packageName)
                    }
                    startActivity(intent)
                    handler.removeCallbacks(usageCheckRunnable) // Stop handler if already blocked
                    currentForegroundRestrictedApp = null
                    return
                }

                // If not already exceeded, and it's a restricted app, start Handler-based monitoring
                if (packageName != currentForegroundRestrictedApp) {
                    // Stop any existing handler for previous app
                    handler.removeCallbacks(usageCheckRunnable)
                    Log.d("BlockingService", "Starting Handler for $packageName.")
                    currentForegroundRestrictedApp = packageName
                    handler.postDelayed(usageCheckRunnable, USAGE_CHECK_INTERVAL_MILLIS)
                }
            } else {
                // If the current foreground app is NOT a restricted app, stop any existing handler
                currentForegroundRestrictedApp?.let {
                    Log.d("BlockingService", "Stopping Handler for $it (non-restricted app in foreground).")
                    handler.removeCallbacks(usageCheckRunnable)
                }
                currentForegroundRestrictedApp = null
            }
        }
    }

    override fun onInterrupt() {
        Log.d("BlockingService", "onInterrupt")
        handler.removeCallbacks(usageCheckRunnable) // Ensure handler is stopped on interrupt
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // We want to receive events from all packages
            packageNames = null
        }
        this.serviceInfo = info
        Log.d("BlockingService", "Service connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(usageCheckRunnable) // Ensure handler is stopped when service is destroyed
    }
}