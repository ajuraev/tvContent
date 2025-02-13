package com.example.tvcontent.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.example.tvcontent.util.loadDeviceLocally
import com.example.tvcontent.viewModel.ContentViewModel

@SuppressLint("ComposableNaming")
@Composable
fun HomeScreen(
    onPlayClicked: () -> Unit,
    onLogout: () -> Unit,
    contentViewModel: ContentViewModel
) {
    val context = LocalContext.current
    val deviceId by contentViewModel.deviceIdFlow.collectAsState()
    val deviceName by contentViewModel.deviceNameFlow.collectAsState()  // Observe deviceNameFlow

    // Load initial data
    LaunchedEffect(Unit) {
        contentViewModel.loadDevice(context)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (deviceId != null) {
            // Display device name and ID from the ViewModel's flows
            Text(text = "Device: $deviceName")
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onPlayClicked,
            // enabled = selectedDevice != null // You'll need to decide how to handle enabling
        ) {
            Text("Play")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Logout")
        }
    }
}