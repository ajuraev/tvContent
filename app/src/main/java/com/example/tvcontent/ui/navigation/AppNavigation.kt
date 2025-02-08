package com.example.tvcontent.ui.navigation

import LoginScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tvcontent.ui.screen.FullScreenPlayer
import com.example.tvcontent.ui.screen.StoreSelection
import com.example.tvcontent.viewModel.ContentViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Define navigation routes in a sealed class for type-safety
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object StoreSelection : Screen("store_selection")
    object FullScreenContent : Screen("full_screen_content")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    supabaseClient: SupabaseClient,
    contentViewModel: ContentViewModel,
//    sessionManager: SessionManager
) {

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    val startDestination = if (supabaseClient.auth.currentSessionOrNull() != null) {
        Screen.StoreSelection.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addLoginScreen(navController, supabaseClient, coroutineScope, contentViewModel)
        addStoreSelectionScreen(navController, contentViewModel, supabaseClient, coroutineScope)
        addFullScreenContentScreen(navController, supabaseClient, contentViewModel)
    }
}

private fun NavGraphBuilder.addStoreSelectionScreen(
    navController: NavHostController,
    contentViewModel: ContentViewModel,
    supabaseClient: SupabaseClient,
    coroutineScope: CoroutineScope
) {

    composable(Screen.StoreSelection.route) {
        val context = LocalContext.current
        StoreSelection(
            onStoreSelected = { selectedStore ->
                contentViewModel.saveSelectedStore(context, selectedStore)
            },
            onPlayClicked = {
                navController.navigate(Screen.FullScreenContent.route)
            },
            onLogout = {
                coroutineScope.launch {
                    try {
                        // Attempt to refresh the session before logging out
                        supabaseClient.auth.refreshCurrentSession()
                        supabaseClient.auth.signOut()
                        supabaseClient.realtime.disconnect()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.StoreSelection.route) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        // If session refresh fails, force logout and clear navigation
                        supabaseClient.auth.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
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
        FullScreenPlayer(
            contentViewModel = contentViewModel,
            onBack = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.addLoginScreen(
    navController: NavHostController,
    supabaseClient: SupabaseClient,
    coroutineScope: CoroutineScope,
    contentViewModel: ContentViewModel,
) {

    composable(Screen.Login.route) {
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Screen.StoreSelection.route)
            },
            supabaseClient = supabaseClient,
            coroutineScope = coroutineScope,
            contentViewModel = contentViewModel
        )
    }
}