package com.example.tvcontent.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.example.tvcontent.viewModel.ContentViewModel

@Composable
fun StoreSelection(
    onStoreSelected: (String) -> Unit,
    onPlayClicked: () -> Unit,
    contentViewModel: ContentViewModel
) {
    // If you want to fetch content on entering this screen:
    // LaunchedEffect(Unit) {
    //    contentViewModel.fetchExistingContent()
    // }

    var expanded by remember { mutableStateOf(false) }
    var selectedStore by remember { mutableStateOf("Select a store") }
    val storeList = listOf("Store 1", "Store 2", "Store 3")

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

        // Simple Button that triggers the Dropdown
        Button(onClick = { expanded = true }) {
            Text(selectedStore)
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            storeList.forEach { store ->
                DropdownMenuItem(
                    text = { Text(store) },
                    onClick = {
                        selectedStore = store
                        expanded = false
                        onStoreSelected(store)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // New PLAY button that takes the user to the full-screen content
        Button(onClick = onPlayClicked) {
            Text("Play")
        }
    }
}
