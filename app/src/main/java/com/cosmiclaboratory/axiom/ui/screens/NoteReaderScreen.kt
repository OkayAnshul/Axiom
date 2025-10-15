package com.cosmiclaboratory.axiom.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.components.EnhancedMarkdownPreview
import com.cosmiclaboratory.axiom.ui.viewmodels.NoteReaderViewModel
import com.cosmiclaboratory.axiom.utils.ExportManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReaderScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    onEditNote: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportManager = remember { ExportManager(context) }
    val listState = rememberLazyListState()
    
    // UI State
    var isUIVisible by remember { mutableStateOf(true) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(16f) }
    
    // Auto-hide UI timer
    LaunchedEffect(isUIVisible) {
        if (isUIVisible) {
            delay(3000) // Hide UI after 3 seconds of inactivity
            isUIVisible = false
        }
    }
    
    // Load note
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }
    
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDarkMode) Color.Black else MaterialTheme.colorScheme.surface
            )
            .pointerInput(Unit) {
                detectTapGestures {
                    isUIVisible = !isUIVisible
                }
            }
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            uiState.note != null -> {
                // Main content
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(
                        top = 80.dp, 
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Note title
                    item {
                        Text(
                            text = uiState.note!!.title.ifEmpty { "Untitled Note" },
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = (fontSize + 8).sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (fontSize + 12).sp
                            ),
                            color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                    
                    // Note metadata
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Updated ${uiState.note!!.updatedAt}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = (fontSize - 2).sp
                                ),
                                color = if (isDarkMode) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Divider
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = if (isDarkMode) Color.Gray.copy(alpha = 0.3f) 
                                   else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                    
                    // Note content
                    item {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.bodyLarge.copy(
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.6f).sp,
                                color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            EnhancedMarkdownPreview(
                                content = uiState.note!!.content,
                                modifier = Modifier.fillMaxWidth(),
                                enableAnimations = true,
                                readerMode = true,
                                useScrollableContainer = false
                            )
                        }
                    }
                }
                
                // Top overlay with gradient
                AnimatedVisibility(
                    visible = isUIVisible,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { -it }
                    ),
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                        animationSpec = tween(300),
                        targetOffsetY = { -it }
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        if (isDarkMode) Color.Black.copy(alpha = 0.9f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        TopAppBar(
                            title = { },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = onEditNote) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                IconButton(onClick = { isSettingsVisible = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }
                
            }
            
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Note not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        // Reading settings modal
        if (isSettingsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isSettingsVisible = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { /* Prevent closing when clicking on card */ },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color.Black else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Reading Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Dark mode toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dark Mode",
                                    color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { isDarkMode = it }
                            )
                        }
                        
                        // Font size slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.FormatSize,
                                        contentDescription = null,
                                        tint = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Font Size",
                                        color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${fontSize.roundToInt()}sp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDarkMode) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                valueRange = 12f..24f,
                                steps = 11,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // Close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isSettingsVisible = false }) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }
}