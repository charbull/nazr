package com.wellbeing.nazr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PasscodeSettingsScreen(onPasscodeSet: () -> Unit) {
    val context = LocalContext.current
    var currentPasscode by remember { mutableStateOf("") }
    var newPasscode by remember { mutableStateOf("") }
    var confirmNewPasscode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var passcodeExists by remember { mutableStateOf(AppPreferences.loadPasscode(context) != null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = "Passcode Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (passcodeExists) {
                Text(text = "Change Passcode", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = currentPasscode,
                    onValueChange = { currentPasscode = it },
                    label = { Text("Current Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            } else {
                Text(text = "Set New Passcode", style = MaterialTheme.typography.headlineSmall)
            }

            OutlinedTextField(
                value = newPasscode,
                onValueChange = { newPasscode = it },
                label = { Text("New Passcode") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            OutlinedTextField(
                value = confirmNewPasscode,
                onValueChange = { confirmNewPasscode = it },
                label = { Text("Confirm New Passcode") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            Button(
                onClick = {
                    if (newPasscode.isEmpty() || confirmNewPasscode.isEmpty()) {
                        message = "New passcode and confirmation cannot be empty."
                    } else if (newPasscode != confirmNewPasscode) {
                        message = "New passcode and confirmation do not match."
                    } else if (passcodeExists && currentPasscode != AppPreferences.loadPasscode(context)) {
                        message = "Current passcode is incorrect."
                    } else {
                        AppPreferences.savePasscode(context, newPasscode)
                        passcodeExists = true
                        message = "Passcode saved successfully!"
                        currentPasscode = ""
                        newPasscode = ""
                        confirmNewPasscode = ""
                        onPasscodeSet()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text(text = if (passcodeExists) "Change Passcode" else "Set Passcode")
            }

            if (message.isNotEmpty()) {
                Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
