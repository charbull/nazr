package com.example.nazr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nazr.ui.theme.NazrTheme

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay // Added
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.mutableStateMapOf
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog // Added
import androidx.compose.material3.TextButton // Added
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import android.content.ComponentName
import androidx.compose.ui.text.input.PasswordVisualTransformation


sealed class Screen {
    object AppSelection : Screen()
    object PasscodeSettings : Screen()
    object PasscodeDecision : Screen()
    object Motivation : Screen() // New state for motivation screen
    object AppDashboard : Screen() // The actual main content after motivation
}

sealed class Permission {
    object UsageStats : Permission()
    object Accessibility : Permission()
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

class MainActivity : ComponentActivity() {

    // Expose this for testing purposes or for worker to update
    var blockedAppPackageName by mutableStateOf<String?>(null)
    var currentScreen: Screen by mutableStateOf(Screen.AppSelection) // Made public for testing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize blockedAppPackageName from intent extras
        blockedAppPackageName = intent.getStringExtra("blocked_app_package_name")

        setContent {
            NazrTheme {
                var usageStatsPermissionGranted by remember { mutableStateOf(hasUsageStatsPermission(this)) }
                var accessibilityServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(this, ComponentName(this, BlockingAccessibilityService::class.java))) }
                var currentPermissionRequest by remember { mutableStateOf<Permission?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    // When returning from settings, re-check permissions
                    usageStatsPermissionGranted = hasUsageStatsPermission(this)
                    accessibilityServiceEnabled = isAccessibilityServiceEnabled(this, ComponentName(this, BlockingAccessibilityService::class.java))
                }

                val activityContext = LocalContext.current
                var hasPasscode by remember { mutableStateOf(AppPreferences.loadPasscode(activityContext) != null) }

                LaunchedEffect(usageStatsPermissionGranted, accessibilityServiceEnabled) {
                    if (!usageStatsPermissionGranted) {
                        currentPermissionRequest = Permission.UsageStats
                    } else if (!accessibilityServiceEnabled) {
                        currentPermissionRequest = Permission.Accessibility
                    } else {
                        currentPermissionRequest = null
                        hasPasscode = AppPreferences.loadPasscode(activityContext) != null
                    }
                }

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentPermissionRequest) {
                        is Permission.UsageStats -> {
                            PermissionRequestScreen(
                                permissionName = "Usage Stats",
                                permissionDescription = "This app needs Usage Stats permission to monitor app usage and enforce limits.",
                                onGrantClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    permissionLauncher.launch(intent)
                                }
                            )
                        }
                        is Permission.Accessibility -> {
                            PermissionRequestScreen(
                                permissionName = "Accessibility Service",
                                permissionDescription = "This app needs the Accessibility Service to detect app launches and block them.",
                                onGrantClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    permissionLauncher.launch(intent)
                                }
                            )
                        }
                        null -> {
                            // All permissions granted, show main content
                            val context = LocalContext.current
                            if (blockedAppPackageName != null) {
                                val appName = try {
                                    context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(blockedAppPackageName!!, 0)).toString()
                                } catch (e: Exception) {
                                    blockedAppPackageName!! // Fallback to package name
                                }
                                BlockingScreen(
                                    appName = appName,
                                    blockedAppPackageName = blockedAppPackageName!!,
                                    onUnlock = {
                                        blockedAppPackageName = null
                                    },
                                    onTemporaryUnlock = { packageName, durationMinutes ->
                                        AppPreferences.setTemporaryUnblock(context, packageName, durationMinutes)
                                        blockedAppPackageName = null
                                    }
                                )
                            } else {
                                when (currentScreen) {
                                    is Screen.AppSelection -> {
                                        val installedApps = remember(usageStatsPermissionGranted) {
                                            getInstalledUserApplications(context)
                                        }
                                        AppSelectionScreen(
                                            installedApps = installedApps,
                                            onNextClicked = {
                                                if (!hasPasscode) {
                                                    currentScreen = Screen.PasscodeSettings
                                                } else {
                                                    currentScreen = Screen.PasscodeDecision
                                                }
                                            }
                                        )
                                    }
                                    is Screen.PasscodeDecision -> {
                                        PasscodeDecisionScreen(
                                            onSetPasscodeClicked = { currentScreen = Screen.PasscodeSettings },
                                            onSkipPasscodeClicked = { currentScreen = Screen.Motivation }
                                        )
                                    }
                                    is Screen.PasscodeSettings -> {
                                        PasscodeSettingsScreen(onPasscodeSet = { currentScreen = Screen.Motivation })
                                    }
                                    is Screen.Motivation -> {
                                        MotivationScreen(onFinish = { finish() })
                                    }
                                    is Screen.AppDashboard -> {
                                        AppSelectionScreen(
                                            installedApps = remember(usageStatsPermissionGranted) { getInstalledUserApplications(context) },
                                            onNextClicked = { /* No-op, user is in app dashboard */ }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppSelectionScreen(installedApps: List<AppInfo>, onNextClicked: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val selectedApps = rememberSaveable(saver = Saver(
        save = { state ->
            state.filterValues { it }.keys.toSet()
        },
        restore = { savedSet ->
            mutableStateMapOf<String, Boolean>().apply {
                savedSet.forEach { put(it, true) }
            }
        }
    )) {
        mutableStateMapOf<String, Boolean>().apply {
            AppPreferences.loadSelectedApps(context).forEach { put(it, true) }
        }
    }

    val usageLimits = rememberSaveable(saver = Saver(
        save = { state -> state.mapValues { it.value }.filterValues { it.isNotBlank() && it.toIntOrNull() != null } },
        restore = { savedMap ->
            mutableStateMapOf<String, String>().apply {
                savedMap.forEach { put(it.key, it.value) }
            }
        }
    )) {
        mutableStateMapOf<String, String>().apply {
            installedApps.forEach { app ->
                val limit = AppPreferences.loadUsageLimit(context, app.packageName)
                if (limit != -1) {
                    put(app.packageName, limit.toString())
                }
            }
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Passcode and Confirmation Dialog States for Usage Limit Change
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var appToChangeLimitFor by remember { mutableStateOf<AppInfo?>(null) }
    var newLimitValue by remember { mutableStateOf("") } // This is for the TextField
    var pendingAppInfoForLimitChange by remember { mutableStateOf<AppInfo?>(null) }
    var pendingNewLimitValue by remember { mutableStateOf<String?>(null) }

    // Debounce effect for limit changes
    LaunchedEffect(pendingNewLimitValue) {
        if (pendingNewLimitValue != null) {
            delay(700L) // Debounce delay
            val app = pendingAppInfoForLimitChange
            val newValue = pendingNewLimitValue
            if (app != null && newValue != null) {
                if (AppPreferences.loadPasscode(context) != null) {
                    // Passcode is set, require passcode verification
                    appToChangeLimitFor = app
                    newLimitValue = newValue
                    showPasscodeDialog = true
                } else {
                    // No passcode set, directly save the limit
                    val limit = newValue.toIntOrNull()
                    AppPreferences.saveUsageLimit(context, app.packageName, limit ?: -1)
                    usageLimits[app.packageName] = newValue // Update local state immediately
                }
            }
            // Reset pending states after processing
            pendingAppInfoForLimitChange = null
            pendingNewLimitValue = null
        }
    }

    // Passcode Dialog State for Deactivating App Limit
    var showDeactivationPasscodeDialog by remember { mutableStateOf(false) }
    var appToDeactivate by remember { mutableStateOf<AppInfo?>(null) }


    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search apps") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(filteredApps) { app ->
                val isSelected = selectedApps[app.packageName] ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // If trying to deactivate (unselect) and passcode is set, require passcode
                            if (isSelected && AppPreferences.loadPasscode(context) != null) {
                                appToDeactivate = app
                                showDeactivationPasscodeDialog = true
                            } else {
                                val newSelection = !isSelected
                                selectedApps[app.packageName] = newSelection
                                AppPreferences.saveSelectedApps(context, selectedApps.filterValues { it }.keys)
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberDrawablePainter(app.icon),
                        contentDescription = app.appName,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(text = app.appName, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (!checked && AppPreferences.loadPasscode(context) != null) {
                                // Trying to deactivate (uncheck) and passcode is set, require passcode
                                appToDeactivate = app
                                showDeactivationPasscodeDialog = true
                            } else {
                                // Activating (check) or no passcode set, proceed directly
                                selectedApps[app.packageName] = checked
                                AppPreferences.saveSelectedApps(context, selectedApps.filterValues { it }.keys)
                            }
                        }
                    )
                }
                if (isSelected) {
                    OutlinedTextField(
                        value = usageLimits[app.packageName] ?: "",
                        onValueChange = { newValue ->
                            if (newValue.toIntOrNull() != null || newValue.isBlank()) { // Only allow valid numbers or empty
                                usageLimits[app.packageName] = newValue // Update UI immediately for visual feedback
                                pendingAppInfoForLimitChange = app
                                pendingNewLimitValue = newValue
                            }
                        },
                        label = { Text("Usage Limit (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 72.dp, end = 16.dp, bottom = 8.dp)
                    )
                }
            }
        }

        Button(
            onClick = onNextClicked,
            enabled = selectedApps.any { it.value },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Next")
        }
    }

    // Passcode Verification Dialog for Usage Limit Change
    if (showPasscodeDialog) {
        PasscodeVerificationDialog(
            onPasscodeVerified = { verified ->
                showPasscodeDialog = false
                if (verified) {
                    showConfirmationDialog = true
                } else {
                    // Passcode incorrect, keep current limit value visible in UI
                    // Revert UI to previous state if needed (optional)
                }
            },
            onDismiss = { showPasscodeDialog = false }
        )
    }

    // Confirmation Dialog for Usage Limit Change
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Confirm Limit Change") },
            text = { Text("Are you sure you want to change the usage limit for ${appToChangeLimitFor?.appName} to ${newLimitValue} minutes?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmationDialog = false
                    appToChangeLimitFor?.let { app ->
                        val limit = newLimitValue.toIntOrNull()
                        AppPreferences.saveUsageLimit(context, app.packageName, limit ?: -1)
                        // usageLimits[app.packageName] is already updated by the original onValueChange
                        // or will be updated by the debounce effect before dialog
                    }
                }) {
                    Text("Yes, Change")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Passcode Verification Dialog for App Deactivation
    if (showDeactivationPasscodeDialog) {
        PasscodeVerificationDialog(
            onPasscodeVerified = { verified ->
                showDeactivationPasscodeDialog = false
                if (verified) {
                    appToDeactivate?.let { app ->
                        selectedApps[app.packageName] = false // Deactivate
                        AppPreferences.saveSelectedApps(context, selectedApps.filterValues { it }.keys)
                    }
                } else {
                    // Passcode incorrect, UI will automatically revert as state wasn't changed
                }
                appToDeactivate = null
            },
            onDismiss = {
                showDeactivationPasscodeDialog = false
                appToDeactivate = null
            }
        )
    }
}


@Composable
fun PasscodeVerificationDialog(onPasscodeVerified: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var passcodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") } // Clear errorMessage on new attempts or dismiss

    AlertDialog(
        onDismissRequest = {
            passcodeInput = "" // Clear input on dismiss
            errorMessage = "" // Clear error on dismiss
            onDismiss()
        },
        title = { Text("Verify Passcode") },
        text = {
            Column {
                OutlinedTextField(
                    value = passcodeInput,
                    onValueChange = {
                        passcodeInput = it
                        errorMessage = "" // Clear error message when user types again
                    },
                    label = { Text("Enter Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val savedPasscode = AppPreferences.loadPasscode(context)
                if (savedPasscode != null && passcodeInput == savedPasscode) {
                    passcodeInput = "" // Clear input on success
                    errorMessage = "" // Clear error on success
                    onPasscodeVerified(true)
                } else {
                    errorMessage = "Incorrect passcode. Please try again."
                    passcodeInput = "" // Clear input on incorrect attempt
                    onPasscodeVerified(false) // This will trigger dismissal from AppSelectionScreen
                }
            }) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                passcodeInput = "" // Clear input on cancel
                errorMessage = "" // Clear error on cancel
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NazrTheme {
        Greeting("Android")
    }
}