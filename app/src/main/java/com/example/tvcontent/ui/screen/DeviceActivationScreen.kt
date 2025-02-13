package com.example.tvcontent.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button

import androidx.tv.material3.Text
import com.example.tvcontent.viewModel.ContentViewModel
import com.example.tvcontent.viewModel.DeviceActivationViewModel


@Composable
fun DeviceActivationScreen(
    activationViewModel: DeviceActivationViewModel,
    contentViewModel: ContentViewModel,
    onActivated: () -> Unit
) {
    val context = LocalContext.current
    val activationCode = activationViewModel.activationCode
    val isActivated = activationViewModel.isActivated

    // Once device is activated, store device ID and navigate away
    if (isActivated) {
        LaunchedEffect(Unit) {
            // Save the device ID in your local secure prefs for future references
            contentViewModel.saveDevice(
                context = context,
                deviceId = activationViewModel.deviceId.toString(),
                deviceName = "MyDevice"
            )
            onActivated()
        }
    }

    // The UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (activationCode == null) {
                // Show a button to generate code
                Button(
                    onClick = { activationViewModel.generatePairingCode() }
                ) {
                    Text(
                        text = "Generate Pairing Code",
                        fontSize = 20.sp
                    )
                }
            } else {
                // Show the generated code
                Text(
                    text = "Your pairing code:",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activationCode,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Use the code on your dashboard to activate this device.",
                    fontSize = 16.sp
                )
            }
        }
    }
}
