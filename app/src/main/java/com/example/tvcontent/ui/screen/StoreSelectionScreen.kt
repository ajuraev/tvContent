package com.example.tvcontent.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text


import com.example.tvcontent.viewModel.ContentViewModel

@SuppressLint("ComposableNaming")
@Composable
fun StoreSelection(
    onStoreSelected: (Int) -> Unit, // Changed from String to Int
    onPlayClicked: () -> Unit,
    onLogout: () -> Unit, // New callback for logout
    contentViewModel: ContentViewModel
) {
    val context = LocalContext.current

    // Collect states
    val selectedStore by contentViewModel.selectedStoreFlow.collectAsState()
    val stores by contentViewModel.storesFlow.collectAsState()

    // Load initial data
    LaunchedEffect(Unit) {
        contentViewModel.loadSelectedStore(context)
        contentViewModel.fetchStores()
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select the store you're at:",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = androidx.compose.ui.graphics.Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (stores.isEmpty()) {
            Text(
                text = "Loading stores...",
                color = androidx.compose.ui.graphics.Color.White
            )
        } else {
            val selectedStoreName = stores.find { it.id.toString() == selectedStore }?.name ?: "Select a store"

            Button(onClick = { expanded = true }) {
                Text(selectedStoreName)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(0.dp, 0.dp)
            ) {
                stores.forEach { store ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = store.name,
                                color = Color.Black // Set text color
                            )
                        },
                        onClick = {
                            expanded = false
                            onStoreSelected(store.id)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedStore != null) {
            Button(
                onClick = onPlayClicked,
                enabled = selectedStore != null
            ) {
                Text("Play")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Button(
            onClick = onLogout, // Trigger the logout callback
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Logout")
        }
    }
}