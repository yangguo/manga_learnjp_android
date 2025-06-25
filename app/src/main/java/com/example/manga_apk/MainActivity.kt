package com.example.manga_apk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.manga_apk.ui.*
import com.example.manga_apk.ui.theme.Manga_apkTheme
import com.example.manga_apk.viewmodel.MangaAnalysisViewModel
import com.example.manga_apk.viewmodel.MangaAnalysisViewModelFactory
import com.example.manga_apk.viewmodel.ReadingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Manga_apkTheme {
                MangaApp()
            }
        }
    }
}

@Composable
fun MangaApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Create a single shared ViewModel instance
    val sharedViewModel: MangaAnalysisViewModel = viewModel(
        factory = MangaAnalysisViewModelFactory(context)
    )
    
    NavHost(
        navController = navController,
        startDestination = "manga_analysis"
    ) {
        composable("manga_analysis") {
            MangaAnalysisScreen(
                viewModel = sharedViewModel,
                onNavigateToInteractiveReading = {
                    navController.navigate("interactive_reading")
                },
                onNavigateToSettings = {
                    navController.navigate("ai_settings")
                },
                onNavigateToDebugLog = {
                    navController.navigate("debug_log")
                }
            )
        }
        
        composable("interactive_reading") {
            val uiState by sharedViewModel.uiState.collectAsState()
            InteractiveReadingScreen(
                selectedImage = uiState.selectedImage,
                onAnalyzeWord = { word ->
                    sharedViewModel.analyzeWord(word)
                },
                onShowSettings = {
                    navController.navigate("ai_settings")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("ai_settings") {
            val uiState by sharedViewModel.uiState.collectAsState()
            AISettingsScreen(
                aiConfig = uiState.aiConfig,
                onConfigUpdate = { config ->
                    sharedViewModel.updateAIConfig(config)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },

                onClearPreferences = {
                    sharedViewModel.clearAllPreferences()
                },
                settingsSaved = uiState.settingsSaved
            )
        }
        
        // Removed reading modes that don't use uploaded images:
        // - reading_mode (Simple Reading)
        // - study_mode (Study Mode)
        // - speed_reading (Speed Reading)
        // - immersive_mode (Immersive Mode)
        // - vocabulary_focus (Vocabulary Focus)
        // These modes are standalone and don't require manga image upload
        
        composable("debug_log") {
            DebugLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}