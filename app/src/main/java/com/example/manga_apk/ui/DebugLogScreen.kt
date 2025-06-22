package com.example.manga_apk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.manga_apk.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Debug log screen that displays real-time logs within the app
 * This helps troubleshoot issues without needing external tools like adb
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    onNavigateBack: () -> Unit
) {
    val logs by DebugLogCollector.logs.collectAsState()
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Debug Logs",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { DebugLogCollector.clearLogs() }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Logs")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        
        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Debug Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Keep this screen open\n" +
                    "2. Go back and try the 'Quick AI Analysis' button\n" +
                    "3. Return here to see what happened\n" +
                    "4. Look for ERROR messages or where the process stops",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Log display
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { logEntry ->
                LogEntryCard(logEntry)
            }
            
            // Add some bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LogEntryCard(logEntry: LogEntry) {
    val backgroundColor = when (logEntry.level) {
        "ERROR" -> MaterialTheme.colorScheme.errorContainer
        "WARN" -> Color(0xFFFFF3CD) // Light yellow
        "INFO" -> MaterialTheme.colorScheme.primaryContainer
        "DEBUG" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    
    val textColor = when (logEntry.level) {
        "ERROR" -> MaterialTheme.colorScheme.onErrorContainer
        "WARN" -> Color(0xFF856404) // Dark yellow
        "INFO" -> MaterialTheme.colorScheme.onPrimaryContainer
        "DEBUG" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = logEntry.level,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = logEntry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = logEntry.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Data class for log entries
 */
data class LogEntry(
    val timestamp: String,
    val level: String,
    val message: String
)

/**
 * Singleton object to collect and manage debug logs
 */
object DebugLogCollector {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    private val maxLogs = 500 // Keep only the last 500 logs
    
    fun addLog(level: String, message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        val newLog = LogEntry(timestamp, level, message)
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(newLog)
        
        // Keep only the last maxLogs entries
        if (currentLogs.size > maxLogs) {
            currentLogs.removeAt(0)
        }
        
        _logs.value = currentLogs
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
}