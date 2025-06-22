package com.example.manga_apk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.manga_apk.data.ReadingTheme
import com.example.manga_apk.viewmodel.ReadingViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel = viewModel()
) {
    val preferences by viewModel.readingPreferences
    val content by viewModel.readingContent
    val isSettingsVisible by viewModel.isSettingsVisible
    val isAutoScrolling by viewModel.isAutoScrolling
    
    val listState = rememberLazyListState()
    
    // Auto-scroll effect
    LaunchedEffect(isAutoScrolling, preferences.scrollSpeed) {
        if (isAutoScrolling) {
            while (isAutoScrolling) {
                delay((1000 / preferences.scrollSpeed).toLong())
                if (listState.canScrollForward) {
                    listState.animateScrollToItem(
                        index = minOf(listState.firstVisibleItemIndex + 1, listState.layoutInfo.totalItemsCount - 1)
                    )
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(preferences.theme.backgroundColor)
            .alpha(preferences.brightness)
    ) {
        // Main reading content
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .clickable { viewModel.toggleSettingsVisibility() },
            contentPadding = PaddingValues(vertical = 32.dp)
        ) {
            item {
                // Title
                Text(
                    text = content.title,
                    fontSize = (preferences.fontSize + 4).sp,
                    color = preferences.theme.textColor,
                    fontFamily = preferences.fontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }
            
            item {
                // Content
                Text(
                    text = content.content,
                    fontSize = preferences.fontSize.sp,
                    color = preferences.theme.textColor,
                    fontFamily = preferences.fontFamily,
                    lineHeight = preferences.lineHeight.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Progress indicator
        LinearProgressIndicator(
            progress = content.progressPercentage,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter),
            color = preferences.theme.textColor.copy(alpha = 0.7f),
            trackColor = preferences.theme.textColor.copy(alpha = 0.2f)
        )
        
        // Settings panel
        AnimatedVisibility(
            visible = isSettingsVisible,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            ReadingSettingsPanel(
                preferences = preferences,
                isAutoScrolling = isAutoScrolling,
                onFontSizeChange = viewModel::updateFontSize,
                onLineHeightChange = viewModel::updateLineHeight,
                onThemeChange = viewModel::updateTheme,
                onFontFamilyChange = viewModel::updateFontFamily,
                onBrightnessChange = viewModel::updateBrightness,
                onNightModeToggle = viewModel::toggleNightMode,
                onAutoScrollToggle = viewModel::toggleAutoScroll,
                onScrollSpeedChange = viewModel::updateScrollSpeed,
                onClose = viewModel::toggleSettingsVisibility
            )
        }
        
        // Auto-scroll indicator
        if (isAutoScrolling) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = preferences.theme.textColor.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Auto-scrolling",
                        tint = preferences.theme.backgroundColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Auto-scrolling",
                        color = preferences.theme.backgroundColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}