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
                },
                onNavigateToStudyMode = {
                    navController.navigate("study_mode")
                },
                onNavigateToSpeedReading = {
                    navController.navigate("speed_reading")
                },
                onNavigateToImmersiveMode = {
                    navController.navigate("immersive_mode")
                },
                onNavigateToVocabularyFocus = {
                    navController.navigate("vocabulary_focus")
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
            val mangaViewModel: MangaAnalysisViewModel = viewModel(
                factory = MangaAnalysisViewModelFactory(context)
            )
            val uiState by mangaViewModel.uiState.collectAsState()
            InteractiveReadingScreen(
                panels = uiState.panels,
                selectedImage = uiState.selectedImage,
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
            val uiState by mangaViewModel.uiState.collectAsState()
            AISettingsScreen(
                aiConfig = uiState.aiConfig,
                onConfigUpdate = { config ->
                    mangaViewModel.updateAIConfig(config)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("study_mode") {
            val viewModel: ReadingViewModel = viewModel()
            StudyModeScreen(
                viewModel = viewModel
            )
        }
        
        composable("speed_reading") {
            val viewModel: ReadingViewModel = viewModel()
            SpeedReadingScreen(
                viewModel = viewModel
            )
        }
        
        composable("immersive_mode") {
            val viewModel: ReadingViewModel = viewModel()
            ImmersiveModeScreen(
                viewModel = viewModel
            )
        }
        
        composable("vocabulary_focus") {
            val viewModel: ReadingViewModel = viewModel()
            VocabularyFocusScreen(
                viewModel = viewModel
            )
        }
    }
}