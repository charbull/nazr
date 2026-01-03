package com.wellbeing.nazr

import android.content.pm.ApplicationInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.content.ComponentName
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.app.usage.UsageStatsManager // Added
import java.util.Calendar // Added

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)

fun getInstalledUserApplications(context: Context): List<AppInfo> {
    val packageManager = context.packageManager
    val appList: MutableList<AppInfo> = mutableListOf()

    val installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    for (appInfo in installedApplications) {
        // Filter for enabled apps that have a launcher activity
        val isLaunchable = packageManager.getLaunchIntentForPackage(appInfo.packageName) != null

        if (appInfo.enabled && isLaunchable) {
            val packageName = appInfo.packageName
            val appName = appInfo.loadLabel(packageManager).toString()
            val icon = appInfo.loadIcon(packageManager)
            appList.add(AppInfo(packageName, appName, icon))
        }
    }

    // Sort by app name
    appList.sortBy { it.appName }

    return appList
}

fun isAccessibilityServiceEnabled(context: Context, serviceComponent: ComponentName): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    for (service in enabledServices) {
        if (serviceComponent == ComponentName(service.resolveInfo.serviceInfo.packageName, service.resolveInfo.serviceInfo.name)) {
            return true
        }
    }
    return false
}

fun isAppUsageExceeded(context: Context, packageName: String): Boolean {
    val selectedApps = AppPreferences.loadSelectedApps(context)
    if (!selectedApps.contains(packageName)) {
        return false // Not a selected app, so not restricted
    }

    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendar.timeInMillis
    val endOfDay = System.currentTimeMillis()

    val usageStats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, endOfDay)
    val appUsageStats = usageStats?.get(packageName)

    if (appUsageStats != null) {
        val totalTimeInForeground = appUsageStats.totalTimeInForeground / (1000 * 60) // Convert ms to minutes
        val limitMinutes = AppPreferences.loadUsageLimit(context, packageName)
        val exceeded = limitMinutes != -1 && totalTimeInForeground > limitMinutes
        return exceeded
    }
    return false
}

fun getForegroundAppPackageName(context: Context): String? {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val currentTime = System.currentTimeMillis()
    // Query for usage stats in a small window leading up to the current time
    // This helps identify the most recent foreground app
    val usageEvents = usageStatsManager.queryEvents(currentTime - (1000 * 10), currentTime) // last 10 seconds
    val event = android.app.usage.UsageEvents.Event()
    var lastForegroundApp: String? = null
    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)
        if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
            lastForegroundApp = event.packageName
        }
    }
    return lastForegroundApp
}