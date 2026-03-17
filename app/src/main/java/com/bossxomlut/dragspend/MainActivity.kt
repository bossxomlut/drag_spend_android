package com.bossxomlut.dragspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.bossxomlut.dragspend.navigation.AppNavGraph
import com.bossxomlut.dragspend.ui.theme.DragSpendTheme
import io.github.jan.supabase.SupabaseClient
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val supabase: SupabaseClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DragSpendTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        supabase = supabase,
                    )
                }
            }
        }
    }
}
