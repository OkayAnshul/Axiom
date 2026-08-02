package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.TaskItem
import com.cosmiclaboratory.axiom.domain.model.TaskParser

@Composable
fun TaskList(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks = remember(content) { TaskParser.extractTasks(content) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tasks (${tasks.count { !it.isCompleted }}/${tasks.size})",
                style = MaterialTheme.typography.titleMedium
            )
            
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task"
                )
            }
        }
        
        if (tasks.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No tasks yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add tasks using markdown syntax: - [ ] Task name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Task list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tasks, key = { "${it.lineIndex}-${it.startIndex}" }) { task ->
                    TaskItemRow(
                        task = task,
                        onToggle = { taskItem ->
                            val newContent = TaskParser.toggleTaskCompletion(content, taskItem)
                            onContentChanged(newContent)
                        }
                    )
                }
            }
        }
    }
    
    // Add task dialog
    if (showAddDialog) {
        AddTaskDialog(
            onTaskAdded = { taskText ->
                val newContent = TaskParser.addNewTask(content, taskText)
                onContentChanged(newContent)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun TaskItemRow(
    task: TaskItem,
    onToggle: (TaskItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle(task) }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) 
                    MaterialTheme.colorScheme.onSurfaceVariant 
                else 
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    onTaskAdded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it },
                    label = { Text("Task description") },
                    placeholder = { Text("Enter task description...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (taskText.isNotBlank()) {
                        onTaskAdded(taskText.trim())
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}