# Plan for Core Distraction-Free App Functionality

This plan outlines the steps to build the core functionality for the Android distraction-free app. The development will follow an iterative approach, focusing on key features as defined in the `spec.md`.

## Phase 1: Foundation and App Selection

### Objective
Establish the basic application structure, permissions handling, and enable users to select applications for restriction.

### Tasks
- [x] Task: Project Setup and Dependencies
    - [x] Write Tests: Verify Android project setup and basic dependency integration.
    - [x] Implement: Initialize a new Android project with Kotlin and Jetpack Compose. Configure necessary build.gradle files.
- [x] Task: Request and Handle Usage Stats Permission
    - [x] Write Tests: Verify permission request flow and user response handling.
    - [x] Implement: Implement logic to request `android.permission.PACKAGE_USAGE_STATS`. Handle cases where permission is granted or denied.
- [x] Task: List Installed Applications
    - [x] Write Tests: Verify correct retrieval and display of all installed applications.
    - [x] Implement: Develop a utility to fetch a list of all user-installed applications with their names and icons.
- [x] Task: App Selection UI
    - [x] Write Tests: Verify UI interactions for selecting/deselecting apps.
    - [ ] Implement: Create a Jetpack Compose screen to display the list of installed applications with checkboxes or toggles for selection.
- [x] Task: Persist Selected Apps
    - [x] Write Tests: Verify saving and loading of selected app preferences.
    - [ ] Implement: Use Android's `SharedPreferences` or a similar mechanism to persist the list of selected applications.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Foundation and App Selection' (Protocol in workflow.md)

## Phase 2: Usage Limits and Background Monitoring

### Objective
Implement the ability to set usage limits for selected apps and monitor app usage in the background.

### Tasks
- [x] Task: Usage Limit Input UI
    - [x] Write Tests: Verify UI elements for setting time limits per app.
    - [ ] Implement: Add UI elements to the app selection screen or a detail screen for each selected app, allowing users to input daily usage limits (e.g., in minutes).
- [x] Task: Persist Usage Limits
    - [x] Write Tests: Verify saving and loading of usage limits for each app.
    - [x] Implement: Store the configured usage limits for each restricted app persistently.
- [x] Task: Background Usage Monitoring with WorkManager
    - [x] Write Tests: Verify WorkManager setup and periodic task execution.
    - [ ] Implement: Configure WorkManager to periodically check the usage statistics of selected applications.
- [x] Task: Calculate Current App Usage
    - [x] Write Tests: Verify accurate calculation of app usage time since last reset (e.g., beginning of day).
    - [ ] Implement: Develop logic to query `UsageStatsManager` for the current day's usage of restricted applications.
- [x] Task: Detect Limit Exceedance
    - [x] Write Tests: Verify correct detection when an app's usage exceeds its defined limit.
    - [x] Implement: Integrate usage calculation with usage limits to determine when a restricted app's limit has been exceeded.
- [x] Task: Conductor - User Manual Verification 'Phase 2: Usage Limits and Background Monitoring' (Protocol in workflow.md)

## Phase 3: Blocking Mechanism and Passcode

### Objective
Implement the blocking screen, passcode setup, and the ability to unblock restricted apps.

### Tasks
- [x] Task: Implement Blocking Screen UI
    - [x] Write Tests: Verify display and basic interaction of the blocking screen.
    - [ ] Implement: Create a full-screen Jetpack Compose UI that appears when a restricted app is launched after exceeding its limit. This screen should clearly state the app is blocked.
- [x] Task: Intercept App Launch
    - [x] Write Tests: Verify successful interception of a blocked app's launch intent.
    - [ ] Implement: Develop a mechanism (e.g., an accessibility service or activity hijacking) to intercept attempts to open a blocked application.
- [x] Task: Passcode Configuration UI
    - [x] Write Tests: Verify UI for setting and changing the alphanumeric passcode.
    - [ ] Implement: Create a settings screen where users can set and update their alphanumeric passcode.
- [x] Task: Secure Passcode Storage
    - [x] Write Tests: Verify secure storage of the passcode (e.g., using Android KeyStore).
    - [x] Implement: Store the alphanumeric passcode securely, ensuring it's not easily accessible.
- [x] Task: Passcode Entry and Verification on Blocking Screen
    - [x] Write Tests: Verify passcode input and correct validation logic on the blocking screen.
    - [ ] Implement: Add an input field to the blocking screen for the alphanumeric passcode and implement verification logic.
- [x] Task: Temporary Unblocking Logic
    - [x] Write Tests: Verify temporary unblocking functionality after correct passcode entry.
    - [ ] Implement: Allow the user to temporarily unblock the app for a predefined duration (e.g., 5-15 minutes) after entering the correct passcode.
- [x] Task: Conductor - User Manual Verification 'Phase 3: Blocking Mechanism and Passcode' (Protocol in workflow.md)
