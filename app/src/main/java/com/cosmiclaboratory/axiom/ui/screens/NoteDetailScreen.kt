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
import com.cosmiclaboratory.axiom.domain.model.TaskParser
import com.cosmiclaboratory.axiom.domain.model.MarkdownTemplate
import com.cosmiclaboratory.axiom.ui.theme.NoteContentStyle
import com.cosmiclaboratory.axiom.ui.viewmodels.NoteDetailViewModel
import com.cosmiclaboratory.axiom.utils.ExportManager
import androidx.compose.ui.platform.LocalContext
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentFocusRequester = remember { FocusRequester() }
    var showMenu by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showToolbox by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportManager = remember { ExportManager(context) }
    
    // Auto-focus content field for new notes
    LaunchedEffect(uiState.isNewNote) {
        if (uiState.isNewNote && uiState.title.isEmpty()) {
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
                if (showPreview) {
                    Row(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Editor side
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(16.dp)
                        ) {
                    // Title input
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text("Title") },
                        placeholder = { Text("Untitled Note") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { contentFocusRequester.requestFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Task list - only show if tasks are detected
                    val tasks = remember(uiState.content) { TaskParser.extractTasks(uiState.content) }
                    if (tasks.isNotEmpty()) {
                        TaskList(
                            content = uiState.content,
                            onContentChanged = viewModel::updateContent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Content input
                    OutlinedTextField(
                        value = uiState.content,
                        onValueChange = viewModel::updateContent,
                        label = { Text("Start writing...") },
                        placeholder = { 
                            Text(
                                text = "What's on your mind?",
                                style = NoteContentStyle,
                                textAlign = TextAlign.Start
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(contentFocusRequester),
                        minLines = 15,
                        textStyle = NoteContentStyle,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                        }
                        
                        // Vertical divider
                        VerticalDivider()
                        
                        // Preview side
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                            
                            MarkdownPreview(
                                content = uiState.content,
                                showDebug = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp, bottom = 16.dp)
                            )
                        }
                    }
                } else {
                    // Single column layout (no preview)
                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                    // Title input
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text("Title") },
                        placeholder = { Text("Untitled Note") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { contentFocusRequester.requestFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Task list - only show if tasks are detected
                    val tasks = remember(uiState.content) { TaskParser.extractTasks(uiState.content) }
                    if (tasks.isNotEmpty()) {
                        TaskList(
                            content = uiState.content,
                            onContentChanged = viewModel::updateContent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Content input
                    OutlinedTextField(
                        value = uiState.content,
                        onValueChange = viewModel::updateContent,
                        label = { Text("Start writing...") },
                        placeholder = { 
                            Text(
                                text = "What's on your mind?",
                                style = NoteContentStyle,
                                textAlign = TextAlign.Start
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(contentFocusRequester),
                        minLines = 15,
                        textStyle = NoteContentStyle,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    }
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