package com.cosmiclaboratory.axiom.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.components.TaskList
import com.cosmiclaboratory.axiom.ui.components.VoiceInputFab
import com.cosmiclaboratory.axiom.ui.components.MarkdownPreview
import com.cosmiclaboratory.axiom.ui.components.MarkdownToolbox
import com.cosmiclaboratory.axiom.ui.components.SplitPaneEditor
import com.cosmiclaboratory.axiom.domain.model.TaskParser
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplate
import com.cosmiclaboratory.axiom.ui.theme.NoteContentStyle
import com.cosmiclaboratory.axiom.ui.viewmodels.NoteDetailViewModel
import com.cosmiclaboratory.axiom.utils.ExportManager
import com.cosmiclaboratory.axiom.utils.ShareIntentHandler
import androidx.compose.ui.platform.LocalContext
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    sharedContent: ShareIntentHandler.SharedContent? = null,
    onSharedContentConsumed: () -> Unit = {},
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentFocusRequester = remember { FocusRequester() }
    var showMenu by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(true) }
    var showToolbox by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportManager = remember { ExportManager(context) }
    
    // Handle shared content
    LaunchedEffect(sharedContent) {
        if (sharedContent != null && uiState.isNewNote) {
            viewModel.updateTitle(sharedContent.title ?: "Shared Note")
            viewModel.updateContent(sharedContent.text)
            onSharedContentConsumed()
        }
    }
    
    // Auto-focus content field for new notes
    LaunchedEffect(uiState.isNewNote) {
        if (uiState.isNewNote && uiState.title.isEmpty() && sharedContent == null) {
            contentFocusRequester.requestFocus()
        }
    }
    
    // Template insertion handler
    val handleTemplateSelected = { template: MarkdownTemplate ->
        val currentContent = uiState.content
        val newContent = if (currentContent.isBlank()) {
            template.template
        } else {
            "$currentContent\n\n${template.template}"
        }
        viewModel.updateContent(newContent)
        showToolbox = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isNewNote) "New Note" else "Edit Note"
                        )
                        
                        if (uiState.isSaving) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Preview toggle button
                    IconButton(onClick = { showPreview = !showPreview }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Toggle markdown preview",
                            tint = if (showPreview) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Toolbox toggle button
                    IconButton(onClick = { showToolbox = !showToolbox }) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = "Toggle markdown toolbox",
                            tint = if (showToolbox) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    if (!uiState.isNewNote) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                // Export as Markdown
                                DropdownMenuItem(
                                    text = { Text("Export as Markdown") },
                                    onClick = {
                                        uiState.note?.let { note ->
                                            val intent = exportManager.exportNote(note, ExportManager.ExportFormat.MARKDOWN)
                                            intent?.let {
                                                context.startActivity(Intent.createChooser(it, "Export Note"))
                                            }
                                        }
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null
                                        )
                                    }
                                )
                                
                                // Export as Text
                                DropdownMenuItem(
                                    text = { Text("Export as Text") },
                                    onClick = {
                                        uiState.note?.let { note ->
                                            val intent = exportManager.exportNote(note, ExportManager.ExportFormat.TEXT)
                                            intent?.let {
                                                context.startActivity(Intent.createChooser(it, "Export Note"))
                                            }
                                        }
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null
                                        )
                                    }
                                )
                                
                                HorizontalDivider()
                                
                                // Delete option
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        viewModel.deleteNote()
                                        showMenu = false
                                        onNavigateBack()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            VoiceInputFab(
                onVoiceResult = { voiceText ->
                    // Append voice text to current content
                    val currentContent = uiState.content
                    val newContent = if (currentContent.isBlank()) {
                        voiceText
                    } else {
                        "$currentContent\n\n$voiceText"
                    }
                    viewModel.updateContent(newContent)
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            else -> {
                // Enhanced editor with split pane functionality
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Task list - only show if tasks are detected
                    val tasks = remember(uiState.content) { TaskParser.extractTasks(uiState.content) }
                    if (tasks.isNotEmpty()) {
                        TaskList(
                            content = uiState.content,
                            onContentChanged = viewModel::updateContent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Split pane editor
                    SplitPaneEditor(
                        title = uiState.title,
                        content = uiState.content,
                        onTitleChange = viewModel::updateTitle,
                        onContentChange = viewModel::updateContent,
                        isPreviewVisible = showPreview,
                        onPreviewToggle = { showPreview = !showPreview },
                        showToolbox = showToolbox,
                        onToolboxToggle = { showToolbox = !showToolbox },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Markdown Toolbox (bottom overlay)
        if (showToolbox) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.BottomCenter
            ) {
                MarkdownToolbox(
                    isExpanded = true,
                    onExpandedChange = { expanded -> if (!expanded) showToolbox = false },
                    onTemplateSelected = handleTemplateSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
        
        // Show error message if any
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // TODO: Show snackbar
                viewModel.clearErrorMessage()
            }
        }
    }
}