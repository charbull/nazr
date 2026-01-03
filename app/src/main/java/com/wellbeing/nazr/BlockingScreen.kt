package com.wellbeing.nazr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlin.random.Random // Added import

@Composable
fun BlockingScreen(appName: String, blockedAppPackageName: String, onUnlock: () -> Unit, onTemporaryUnlock: (String, Int) -> Unit) {
    val context = LocalContext.current
    var passcodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passcodeCorrect by remember { mutableStateOf(false) }

    // New state for delay
    var showLoadingDelay by remember { mutableStateOf(true) }

    // Hardcoded quotes
    val quotes = listOf(
        "The best way to predict the future is to create it. - Peter Drucker",
        "Your time is limited, don't waste it living someone else's life. - Steve Jobs",
        "The future belongs to those who believe in the beauty of their dreams. - Eleanor Roosevelt",
        "The only way to do great work is to love what you do. - Steve Jobs",
        "Believe you can and you're halfway there. - Theodore Roosevelt"
    )
    val randomQuote by remember { mutableStateOf(quotes[Random.nextInt(quotes.size)]) }

    LaunchedEffect(key1 = Unit) {
        delay(5000L) // 5-second delay
        showLoadingDelay = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.error
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time limit reached for $appName!", // Modified message
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onError,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display random quote always
            Text(
                text = randomQuote,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onError,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (showLoadingDelay) {
                // Loading animation/text placeholder
                Text(
                    text = "Loading...", // Placeholder for sand device
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                // Original passcode and unlock UI (only show when not in loading delay)
                OutlinedTextField(
                    value = passcodeInput,
                    onValueChange = { passcodeInput = it },
                    label = { Text("Enter Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val savedPasscode = AppPreferences.loadPasscode(context)
                        if (savedPasscode != null && passcodeInput == savedPasscode) {
                            passcodeCorrect = true
                            errorMessage = ""
                            onUnlock() // Use callback for permanent unlock for the session
                        } else {
                            passcodeCorrect = false
                            errorMessage = "Incorrect passcode. Please try again."
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                ) {
                    Text("Verify Passcode")
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (passcodeCorrect) {
                    Button(
                        onClick = {
                            onTemporaryUnlock(blockedAppPackageName, 5)
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                    ) {
                        Text("Temporarily Unblock for 5 minutes")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onTemporaryUnlock(blockedAppPackageName, 15)
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                    ) {
                        Text("Temporarily Unblock for 15 minutes")
                    }
                }
            }
        }
    }
}