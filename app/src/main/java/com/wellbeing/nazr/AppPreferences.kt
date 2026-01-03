package com.wellbeing.nazr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.TimeUnit

object AppPreferences {
    private const val PREFS_NAME = "app_prefs"
    private const val SELECTED_APPS_KEY = "selected_apps"
    private const val USAGE_LIMIT_PREFIX = "usage_limit_" // Prefix for usage limit keys
    private const val PASSCODE_KEY = "passcode"
    private const val UNBLOCK_TIMESTAMP_PREFIX = "unblock_timestamp_"

    private fun isTest(): Boolean {
        return try {
            Class.forName("org.junit.Test")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences { // Return SharedPreferences
        if (isTest()) {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSelectedApps(context: Context, selectedApps: Set<String>) {
        val prefs = getEncryptedSharedPreferences(context) // Use encrypted prefs
        prefs.edit().putStringSet(SELECTED_APPS_KEY, selectedApps).apply()
    }

    fun loadSelectedApps(context: Context): Set<String> {
        val prefs = getEncryptedSharedPreferences(context) // Use encrypted prefs
        return prefs.getStringSet(SELECTED_APPS_KEY, emptySet()) ?: emptySet()
    }

    fun saveUsageLimit(context: Context, packageName: String, limitMinutes: Int) {
        val prefs = getEncryptedSharedPreferences(context) // Use encrypted prefs
        prefs.edit().putInt(USAGE_LIMIT_PREFIX + packageName, limitMinutes).apply()
    }

    fun loadUsageLimit(context: Context, packageName: String): Int {
        val prefs = getEncryptedSharedPreferences(context) // Use encrypted prefs
        return prefs.getInt(USAGE_LIMIT_PREFIX + packageName, -1) // -1 indicates no limit set
    }

    fun savePasscode(context: Context, passcode: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().putString(PASSCODE_KEY, passcode).apply()
    }

    fun loadPasscode(context: Context): String? {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(PASSCODE_KEY, null)?.takeIf { it.isNotEmpty() }
    }

    fun setTemporaryUnblock(context: Context, packageName: String, durationMinutes: Int) {
        val prefs = getEncryptedSharedPreferences(context)
        val unblockUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        prefs.edit().putLong(UNBLOCK_TIMESTAMP_PREFIX + packageName, unblockUntil).apply()
    }

    fun isTemporarilyUnblocked(context: Context, packageName: String): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        val unblockUntil = prefs.getLong(UNBLOCK_TIMESTAMP_PREFIX + packageName, 0L)
        return System.currentTimeMillis() < unblockUntil
    }

    fun clearTemporaryUnblock(context: Context, packageName: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().remove(UNBLOCK_TIMESTAMP_PREFIX + packageName).apply()
    }
}
