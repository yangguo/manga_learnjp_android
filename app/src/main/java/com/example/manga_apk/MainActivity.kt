package com.example.manga_apk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.manga_apk.ui.AISettingsScreen
import com.example.manga_apk.ui.InteractiveReadingScreen
import com.example.manga_apk.ui.MangaAnalysisScreen
import com.example.manga_apk.ui.ReadingScreen
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
    
    NavHost(
        navController = navController,
        startDestination = "manga_analysis"
    ) {
        composable("manga_analysis") {
            val viewModel: MangaAnalysisViewModel = viewModel(
                factory = MangaAnalysisViewModelFactory(context)
            )
            MangaAnalysisScreen(
                viewModel = viewModel,
                onNavigateToReading = {
                    navController.navigate("reading_mode")
                },
                onNavigateToInteractiveReading = {
                    navController.navigate("interactive_reading")
                },
                onNavigateToSettings = {
                    navController.navigate("ai_settings")
                }
            )
        }
        
        composable("reading_mode") {
            val viewModel: ReadingViewModel = viewModel()
            ReadingScreen(
                viewModel = viewModel
            )
        }
        
        composable("interactive_reading") {
            val mangaViewModel: MangaAnalysisViewModel = viewModel()
            InteractiveReadingScreen(
                panels = mangaViewModel.uiState.value.panels,
                selectedImage = mangaViewModel.uiState.value.selectedImage,
                onAnalyzeWord = { word ->
                    mangaViewModel.analyzeWord(word)
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
            val mangaViewModel: MangaAnalysisViewModel = viewModel(
                factory = MangaAnalysisViewModelFactory(context)
            )
            AISettingsScreen(
                aiConfig = mangaViewModel.uiState.value.aiConfig,
                onConfigUpdate = { config ->
                    mangaViewModel.updateAIConfig(config)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}