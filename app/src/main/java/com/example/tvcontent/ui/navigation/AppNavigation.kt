package com.example.tvcontent.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tvcontent.ui.screen.FullScreenContent
import com.example.tvcontent.ui.screen.StoreSelection
import com.example.tvcontent.viewModel.ContentViewModel
import io.github.jan.supabase.SupabaseClient

/**
 * Define navigation routes in a sealed class for type-safety
 */
sealed class Screen(val route: String) {
    object StoreSelection : Screen("store_selection")
    object FullScreenContent : Screen("full_screen_content")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    supabaseClient: SupabaseClient,
    contentViewModel: ContentViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StoreSelection.route
    ) {
        addStoreSelectionScreen(navController, contentViewModel)
        addFullScreenContentScreen(navController, supabaseClient, contentViewModel)
    }
}

private fun NavGraphBuilder.addStoreSelectionScreen(
    navController: NavHostController,
    contentViewModel: ContentViewModel
) {
    composable(Screen.StoreSelection.route) {
        StoreSelection(
            onStoreSelected = { selectedStore ->
                // Handle store selection logic here
            },
            onPlayClicked = {
                // Navigate to FullScreenContent screen
                navController.navigate(Screen.FullScreenContent.route)
            },
            contentViewModel = contentViewModel
        )
    }
}

private fun NavGraphBuilder.addFullScreenContentScreen(
    navController: NavHostController,
    supabaseClient: SupabaseClient,
    contentViewModel: ContentViewModel
) {
    composable(Screen.FullScreenContent.route) {
        FullScreenContent(
            supabaseClient = supabaseClient,
            contentViewModel = contentViewModel,
            onBack = { navController.popBackStack() }
        )
    }
}
