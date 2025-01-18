package com.example.tvcontent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.tvcontent.ui.navigation.AppNavigation
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tvcontent.ui.theme.TvContentTheme
import com.example.tvcontent.viewModel.ContentViewModel

class MainActivity : ComponentActivity() {
    // Create the Supabase client (replace with your own URL and Key)
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://crwlqmdqrftnxzkjqtwn.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNyd2xxbWRxcmZ0bnh6a2pxdHduIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzcxNDM1MDEsImV4cCI6MjA1MjcxOTUwMX0.6Cy76lRj6Lf50KHVRpqx_QTTUtnlfrrYM6WiPsu0q50"
    ) {
        install(Postgrest)
        install(Realtime)
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Provide a single instance of the ContentViewModel
            val contentViewModel: ContentViewModel = viewModel(
                factory = ContentViewModel.Factory(supabase)
            )

            val navController = rememberNavController()

            TvContentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    // Setup your navigation graph
                    AppNavigation(
                        navController = navController,
                        supabaseClient = supabase,
                        contentViewModel = contentViewModel
                    )
                }
            }
        }
    }
}
