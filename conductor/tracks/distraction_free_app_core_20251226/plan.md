# Plan for Core Distraction-Free App Functionality

This plan outlines the steps to build the core functionality for the Android distraction-free app. The development will follow an iterative approach, focusing on key features as defined in the `spec.md`.

## Phase 1: Foundation and App Selection

### Objective
Establish the basic application structure, permissions handling, and enable users to select applications for restriction.

### Tasks
- [ ] Task: Project Setup and Dependencies
    - [ ] Write Tests: Verify Android project setup and basic dependency integration.
    - [ ] Implement: Initialize a new Android project with Kotlin and Jetpack Compose. Configure necessary build.gradle files.
- [ ] Task: Request and Handle Usage Stats Permission
    - [ ] Write Tests: Verify permission request flow and user response handling.
    - [ ] Implement: Implement logic to request `android.permission.PACKAGE_USAGE_STATS`. Handle cases where permission is granted or denied.
- [ ] Task: List Installed Applications
    - [ ] Write Tests: Verify correct retrieval and display of all installed applications.
    - [ ] Implement: Develop a utility to fetch a list of all user-installed applications with their names and icons.
- [ ] Task: App Selection UI
    - [ ] Write Tests: Verify UI interactions for selecting/deselecting apps.
    - [ ] Implement: Create a Jetpack Compose screen to display the list of installed applications with checkboxes or toggles for selection.
- [ ] Task: Persist Selected Apps
    - [ ] Write Tests: Verify saving and loading of selected app preferences.
    - [ ] Implement: Use Android's `SharedPreferences` or a similar mechanism to persist the list of selected applications.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Foundation and App Selection' (Protocol in workflow.md)

## Phase 2: Usage Limits and Background Monitoring

### Objective
Implement the ability to set usage limits for selected apps and monitor app usage in the background.

### Tasks
- [ ] Task: Usage Limit Input UI
    - [ ] Write Tests: Verify UI elements for setting time limits per app.
    - [ ] Implement: Add UI elements to the app selection screen or a detail screen for each selected app, allowing users to input daily usage limits (e.g., in minutes).
- [ ] Task: Persist Usage Limits
    - [ ] Write Tests: Verify saving and loading of usage limits for each app.
    - [ ] Implement: Store the configured usage limits for each restricted app persistently.
- [ ] Task: Background Usage Monitoring with WorkManager
    - [ ] Write Tests: Verify WorkManager setup and periodic task execution.
    - [ ] Implement: Configure WorkManager to periodically check the usage statistics of selected applications.
- [ ] Task: Calculate Current App Usage
    - [ ] Write Tests: Verify accurate calculation of app usage time since last reset (e.g., beginning of day).
    - [ ] Implement: Develop logic to query `UsageStatsManager` for the current day's usage of restricted applications.
- [ ] Task: Detect Limit Exceedance
    - [ ] Write Tests: Verify correct detection when an app's usage exceeds its defined limit.
    - [ ] Implement: Integrate usage calculation with usage limits to determine when a restricted app's limit has been exceeded.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Usage Limits and Background Monitoring' (Protocol in workflow.md)

## Phase 3: Blocking Mechanism and Passcode

### Objective
Implement the blocking screen, passcode setup, and the ability to unblock restricted apps.

### Tasks
- [ ] Task: Implement Blocking Screen UI
    - [ ] Write Tests: Verify display and basic interaction of the blocking screen.
    - [ ] Implement: Create a full-screen Jetpack Compose UI that appears when a restricted app is launched after exceeding its limit. This screen should clearly state the app is blocked.
- [ ] Task: Intercept App Launch
    - [ ] Write Tests: Verify successful interception of a blocked app's launch intent.
    - [ ] Implement: Develop a mechanism (e.g., an accessibility service or activity hijacking) to intercept attempts to open a blocked application.
- [ ] Task: Passcode Configuration UI
    - [ ] Write Tests: Verify UI for setting and changing the alphanumeric passcode.
    - [ ] Implement: Create a settings screen where users can set and update their alphanumeric passcode.
- [ ] Task: Secure Passcode Storage
    - [ ] Write Tests: Verify secure storage of the passcode (e.g., using Android KeyStore).
    - [ ] Implement: Store the alphanumeric passcode securely, ensuring it's not easily accessible.
- [ ] Task: Passcode Entry and Verification on Blocking Screen
    - [ ] Write Tests: Verify passcode input and correct validation logic on the blocking screen.
    - [ ] Implement: Add an input field to the blocking screen for the alphanumeric passcode and implement verification logic.
- [ ] Task: Temporary Unblocking Logic
    - [ ] Write Tests: Verify temporary unblocking functionality after correct passcode entry.
    - [ ] Implement: Allow the user to temporarily unblock the app for a predefined duration (e.g., 5-15 minutes) after entering the correct passcode.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Blocking Mechanism and Passcode' (Protocol in workflow.md)
