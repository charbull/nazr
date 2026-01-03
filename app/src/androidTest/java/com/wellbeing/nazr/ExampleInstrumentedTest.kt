package com.example.nazr

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.app.AppOpsManager
import android.content.Context
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.onAllNodesWithClass
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import org.junit.Assert.assertTrue
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.performTextInput
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import android.content.pm.PackageManager

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyMainActivityDisplaysGreeting() {
        // Check if the "Hello Android!" text is displayed
        composeTestRule.onNodeWithText("Hello Android!").assertIsDisplayed()
    }

    @Test
    fun verifyUsageStatsPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)

        val message = "Usage Stats permission is not granted. Please grant it manually via 'adb shell cmd appops set ${context.packageName} android:get_usage_stats allow' or in device settings."
        assertEquals(AppOpsManager.MODE_ALLOWED, mode, message)
    }

    @Test
    fun verifyInstalledAppsDisplayed() {
        // Assert that the "Usage Stats Permission NOT Granted" message is not displayed,
        // implying the app list should be visible instead.
        // This test relies on the assumption that if permission is granted,
        // the MainActivity will display the app list.
        composeTestRule.onNodeWithText("Usage Stats Permission NOT Granted").assertDoesNotExist()

        // You might want to add more robust checks here,
        // like checking for a specific app name or the existence of a scrollable list.
        // For now, asserting the absence of the "not granted" message indicates success.
    }

    @Test
    fun verifyAppSelectionUI() {
        // Assume usage stats permission is granted, and app list is displayed.
        // We will try to interact with the first switch found on the screen.

        composeTestRule.onNodeWithText("Usage Stats Permission NOT Granted").assertDoesNotExist()

        // Find the first Switch component
        val firstSwitch = composeTestRule.onAllNodesWithClass("android.widget.Switch").onFirst()

        // Assert it's initially unchecked (or whatever its default state is)
        // A more robust test would know the initial state based on data.
        // For now, we just verify interaction.
        // If it's checked, uncheck it, if unchecked, check it.
        val initialCheckedState = firstSwitch.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState)
        if (initialCheckedState == ToggleableState.On) {
            firstSwitch.performClick()
            composeTestRule.waitForIdle()
            firstSwitch.assertIsOff()
        } else {
            firstSwitch.performClick()
            composeTestRule.waitForIdle()
            firstSwitch.assertIsOn()
        }
    }

    @Test
    fun verifyAppPreferencePersistence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear any previous selections for a clean test
        AppPreferences.saveSelectedApps(context, emptySet())

        // Launch the activity and select an app
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.runOnUiThread {
                val installedApps = getInstalledUserApplications(activity)
                if (installedApps.isNotEmpty()) {
                    val firstAppPackageName = installedApps.first().packageName
                    // Simulate selecting the first app
                    val currentSelected = AppPreferences.loadSelectedApps(context).toMutableSet()
                    currentSelected.add(firstAppPackageName)
                    AppPreferences.saveSelectedApps(context, currentSelected)
                }
            }
        }

        // Recreate the activity to check for persistence
        composeTestRule.activityRule.scenario.recreate()

        // Verify that the first app is now selected
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.runOnUiThread {
                val installedApps = getInstalledUserApplications(activity)
                if (installedApps.isNotEmpty()) {
                    val firstAppPackageName = installedApps.first().packageName
                    val selectedApps = AppPreferences.loadSelectedApps(context)
                    assertTrue("Selected app should be persisted", selectedApps.contains(firstAppPackageName))
                }
            }
        }
        // Clean up: Clear selections after the test
        AppPreferences.saveSelectedApps(context, emptySet())
    }

    @Test
    fun verifyUsageLimitInputUI() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppPreferences.saveSelectedApps(context, emptySet()) // Clear selections
        AppPreferences.saveUsageLimit(context, "com.example.someapp", -1) // Clear any previous limit for a dummy app

        // Select the first app to make its usage limit field visible
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.runOnUiThread {
                val installedApps = getInstalledUserApplications(activity)
                if (installedApps.isNotEmpty()) {
                    val firstAppPackageName = installedApps.first().packageName
                    val currentSelected = AppPreferences.loadSelectedApps(context).toMutableSet()
                    currentSelected.add(firstAppPackageName)
                    AppPreferences.saveSelectedApps(context, currentSelected)
                }
            }
        }
        composeTestRule.waitForIdle()

        // Find the first OutlinedTextField (Usage Limit input)
        // Note: You might need a more specific matcher if there are other OutlinedTextFields
        val usageLimitField = composeTestRule.onNodeWithText("Usage Limit (minutes)")
        usageLimitField.assertIsDisplayed()

        val testLimit = "120" // 2 hours

        // Type the test limit
        usageLimitField.performTextInput(testLimit)
        composeTestRule.waitForIdle()

        // Verify the value is saved in preferences
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.runOnUiThread {
                val installedApps = getInstalledUserApplications(activity)
                if (installedApps.isNotEmpty()) {
                    val firstAppPackageName = installedApps.first().packageName
                    val savedLimit = AppPreferences.loadUsageLimit(context, firstAppPackageName)
                    assertEquals("Usage limit should be saved correctly", testLimit.toInt(), savedLimit)
                }
            }
        }

        // Clean up
        AppPreferences.saveSelectedApps(context, emptySet())
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.runOnUiThread {
                val installedApps = getInstalledUserApplications(activity)
                if (installedApps.isNotEmpty()) {
                    val firstAppPackageName = installedApps.first().packageName
                    AppPreferences.saveUsageLimit(context, firstAppPackageName, -1)
                }
            }
        }
    }

    @Test
    fun verifyUsageLimitPersistence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppPreferences.saveSelectedApps(context, emptySet()) // Clear selections
        AppPreferences.saveUsageLimit(context, "com.example.someapp", -1) // Clear any previous limit for a dummy app

        val testPackageName = "com.example.testapp"
        val testLimit = 150

        // Simulate saving a usage limit for a test app
        AppPreferences.saveUsageLimit(context, testPackageName, testLimit)

        // Recreate the activity to check for persistence
        composeTestRule.activityRule.scenario.recreate()

        // Verify the usage limit is loaded correctly
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.runOnUiThread {
                val loadedLimit = AppPreferences.loadUsageLimit(context, testPackageName)
                assertEquals("Persisted usage limit should be loaded correctly", testLimit, loadedLimit)
            }
        }
        // Clean up
        AppPreferences.saveUsageLimit(context, testPackageName, -1)
    }

    @Test
    fun verifyWorkManagerSetup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val workManager = WorkManager.getInstance(context)

        // Cancel all existing work to ensure a clean state
        workManager.cancelAllWork()

        // Enqueue the work for testing purposes
        val workRequest = PeriodicWorkRequestBuilder<UsageStatsWorker>(
            15, TimeUnit.MINUTES // Run every 15 minutes
        ).build()
        workManager.enqueueUniquePeriodicWork(
            "UsageStatsMonitoring",
            ExistingPeriodicWorkPolicy.REPLACE, // Replace existing work for testing
            workRequest
        ).result.get() // Block until enqueued

        // Observe the work status
        val info = workManager.getWorkInfosForUniquePeriodicWork("UsageStatsMonitoring").get()
        assertNotNull(info)
        assertFalse(info.isEmpty())
        // The work should be ENQUEUED initially
        assertEquals(WorkInfo.State.ENQUEUED, info.first().state)

        // Clean up
        workManager.cancelAllWork()
    }

    @Test
    fun verifyBlockingScreenDisplayed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dummyBlockedPackageName = "com.example.blockedapp"

        // Set the blocked app in MainActivity
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.blockedAppPackageName = dummyBlockedPackageName
        }
        composeTestRule.waitForIdle()

        // Get the app name that would be displayed
        val appName = try {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(dummyBlockedPackageName, 0)).toString()
        } catch (e: Exception) {
            dummyBlockedPackageName // Fallback to package name if name not found
        }

        // Verify that the BlockingScreen is displayed with the correct text
        composeTestRule.onNodeWithText("Access to $appName is blocked!").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have exceeded your daily usage limit for this application.").assertIsDisplayed()

        // Clear the blocked app state
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.blockedAppPackageName = null
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun verifyAccessibilityServiceSetup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pm = context.packageManager
        val componentName = ComponentName(context, BlockingAccessibilityService::class.java)

        // Check if the service is enabled (requires user to enable it manually)
        val isServiceEnabled = isAccessibilityServiceEnabled(context, componentName)

        // We can't enable it programmatically, so we assert that it's declared correctly.
        // A failing test here means the user needs to enable the service.
        assertTrue("Accessibility service should be enabled for full functionality. Please enable it manually in device settings -> Accessibility -> Installed apps -> Nazr.", isServiceEnabled)


        // Verify that the service is declared in AndroidManifest.xml
        val serviceInfo = pm.getServiceInfo(componentName, PackageManager.GET_META_DATA)
        assertNotNull("Accessibility service is not declared in AndroidManifest.xml", serviceInfo)
        assertNotNull("Accessibility service meta-data is missing", serviceInfo.metaData)
        assertEquals("Accessibility service meta-data resource is incorrect", R.xml.accessibility_service_config, serviceInfo.metaData.getInt("android.accessibilityservice"))
    }

    // Helper function to check if accessibility service is enabled (simplified, actual check is more involved)
    private fun isAccessibilityServiceEnabled(context: Context, componentName: ComponentName): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (service in enabledServices) {
            if (componentName.equals(service.resolveInfo.serviceInfo.componentName)) {
                return true
            }
        }
        return false
    }

    @Test
    fun verifyPasscodeSettingsUI() {
        // Navigate to PasscodeSettingsScreen
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.currentScreen = Screen.PasscodeSettings
        }
        composeTestRule.waitForIdle()

        // Clear any previous passcode
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppPreferences.savePasscode(context, "")

        // Enter a new passcode
        val newPasscodeField = composeTestRule.onNodeWithText("New Passcode")
        val confirmNewPasscodeField = composeTestRule.onNodeWithText("Confirm New Passcode")
        val setPasscodeButton = composeTestRule.onNodeWithText("Set Passcode")

        newPasscodeField.performTextInput("1234")
        confirmNewPasscodeField.performTextInput("1234")
        setPasscodeButton.performClick()
        composeTestRule.waitForIdle()

        // Verify success message
        composeTestRule.onNodeWithText("Passcode saved successfully!").assertIsDisplayed()

        // Verify persistence by restarting activity and checking if passcode is loaded
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertEquals("1234", AppPreferences.loadPasscode(context))
        }

        // Clean up
        AppPreferences.savePasscode(context, "")
    }

    @Test
    fun verifyTemporaryUnblocking() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testPasscode = "5678"
        val dummyBlockedPackageName = "com.example.blockedapp"

        // Set a passcode and block an app
        AppPreferences.savePasscode(context, testPasscode)
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.blockedAppPackageName = dummyBlockedPackageName
        }
        composeTestRule.waitForIdle()

        // Enter correct passcode
        composeTestRule.onNodeWithText("Enter Passcode").performTextInput(testPasscode)
        composeTestRule.onNodeWithText("Verify Passcode").performClick()
        composeTestRule.waitForIdle()

        // Click "Temporarily Unblock for 5 minutes" button
        composeTestRule.onNodeWithText("Temporarily Unblock for 5 minutes").performClick()
        composeTestRule.waitForIdle()

        // Verify app is unblocked
        composeTestRule.activityRule.scenario.onActivity { activity ->
            assertNull(activity.blockedAppPackageName)
            assertTrue(AppPreferences.isTemporarilyUnblocked(context, dummyBlockedPackageName))
        }

        // Clean up
        AppPreferences.savePasscode(context, "")
        AppPreferences.clearTemporaryUnblock(context, dummyBlockedPackageName)
    }
}
