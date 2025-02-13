package com.example.tvcontent.ui.navigation

import LoginScreen
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.tvcontent.ui.screen.DeviceActivationScreen
import com.example.tvcontent.ui.screen.FullScreenPlayer
import com.example.tvcontent.ui.screen.HomeScreen
import com.example.tvcontent.viewModel.ContentViewModel
import com.example.tvcontent.viewModel.DeviceActivationViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Define navigation routes in a sealed class for type-safety
 */
sealed class Screen(val route: String) {
//    object Login : Screen("login")
    object StoreSelection : Screen("store_selection")
    object FullScreenContent : Screen("full_screen_content")
    object DeviceActivation : Screen("device_activation")

}

@Composable
fun AppNavigation(
    navController: NavHostController,
    supabaseClient: SupabaseClient,
    contentViewModel: ContentViewModel,
    activationViewModel: DeviceActivationViewModel
//    sessionManager: SessionManager
) {

    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    // Listen for changes to deviceIdFlow
    val deviceId by contentViewModel.deviceIdFlow.collectAsState()

    // If deviceId is present => go to Home (StoreSelection).
    // Otherwise => go to DeviceActivation.
    val startDestination = if (!deviceId.isNullOrEmpty()) {
        Screen.StoreSelection.route
    } else {
        Screen.DeviceActivation.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addDeviceActivationScreen(navController, supabaseClient, contentViewModel)
//        addLoginScreen(navController, supabaseClient, coroutineScope, contentViewModel)
        addHomeScreen(navController, contentViewModel, supabaseClient, coroutineScope)
        addFullScreenContentScreen(navController, supabaseClient, contentViewModel)
    }
}

private fun NavGraphBuilder.addHomeScreen(
    navController: NavHostController,
    contentViewModel: ContentViewModel,
    supabaseClient: SupabaseClient,
    coroutineScope: CoroutineScope
) {

    composable(Screen.StoreSelection.route) {
        val context = LocalContext.current
        HomeScreen(
            onPlayClicked = {
                navController.navigate(Screen.FullScreenContent.route)
            },
            onLogout = {
                coroutineScope.launch {
                    try {
                        // Clear the device locally and in the ViewModel
                        contentViewModel.onLogout(context)

                        // Navigate to the login or activation screen
                        navController.navigate(Screen.DeviceActivation.route) {
                            popUpTo(0) { inclusive = true }
                        }

                        // Clear the session from Supabase (if you’re also using user-based auth)
                        //supabaseClient.realtime.disconnect()





                    } catch (e: Exception) {
                        // If session refresh fails, force logout and clear anyway
                        contentViewModel.onLogout(context)

                        // Force back to login
                        navController.navigate(Screen.DeviceActivation.route) {
                            popUpTo(0) { inclusive = true }
                        }

                        supabaseClient.realtime.disconnect()

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

//private fun NavGraphBuilder.addLoginScreen(
//    navController: NavHostController,
//    supabaseClient: SupabaseClient,
//    coroutineScope: CoroutineScope,
//    contentViewModel: ContentViewModel,
//) {
//
//    composable(Screen.Login.route) {
//        LoginScreen(
//            onLoginSuccess = {
//                navController.navigate(Screen.StoreSelection.route)
//            },
//            supabaseClient = supabaseClient,
//            coroutineScope = coroutineScope,
//            contentViewModel = contentViewModel
//        )
//    }
//}

private fun NavGraphBuilder.addDeviceActivationScreen(
    navController: NavHostController,
    supabaseClient: SupabaseClient,
    contentViewModel: ContentViewModel
) {
    composable(Screen.DeviceActivation.route) {
        val activationViewModel: DeviceActivationViewModel = viewModel(
            factory = DeviceActivationViewModel.Factory(supabaseClient)
        )

        DeviceActivationScreen(
            activationViewModel = activationViewModel,
            contentViewModel,
            // Provide a callback to navigate when activated
            onActivated = {
//                contentViewModel.resubscribeToRealtime()
                Log.d("Appnav","activated")
                // For example, once the device is activated, we go to the StoreSelection
                navController.navigate(Screen.StoreSelection.route) {
                    popUpTo(Screen.DeviceActivation.route) { inclusive = true }
                }
            }
        )
    }
}
