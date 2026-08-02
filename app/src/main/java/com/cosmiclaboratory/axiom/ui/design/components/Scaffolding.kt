package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Standard screen frame: top bar with scroll behaviour, snackbar host, insets.
 *
 * [testTag] is required, not optional — instrumented tests need a stable handle
 * on every screen root, and making it a parameter means nobody forgets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxiomScaffold(
    title: String,
    screenTag: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier
            .testTag(screenTag)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AxiomTheme.colors.canvas,
        topBar = {
            TopAppBar(
                title = { Text(title, style = AxiomTheme.type.uiTitleLarge) },
                navigationIcon = { navigationIcon?.invoke() },
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AxiomTheme.colors.canvas,
                    scrolledContainerColor = AxiomTheme.colors.surface,
                    titleContentColor = AxiomTheme.colors.ink,
                    actionIconContentColor = AxiomTheme.colors.inkMuted
                )
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        content = content
    )
}

/** Modal sheet with consistent chrome. Predictive back comes free from M3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxiomBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AxiomTheme.colors.surfaceRaised,
        contentColor = AxiomTheme.colors.ink,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = AxiomTheme.space.screenH,
                    end = AxiomTheme.space.screenH,
                    bottom = AxiomTheme.space.xxl
                )
        ) {
            Text(title, style = AxiomTheme.type.uiTitle, color = AxiomTheme.colors.ink)
            Row(Modifier.padding(top = AxiomTheme.space.md)) { }
            content()
        }
    }
}

@Composable
fun AxiomConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AxiomTheme.colors.surfaceRaised,
        title = { Text(title, style = AxiomTheme.type.uiTitle, color = AxiomTheme.colors.ink) },
        text = { Text(body, style = AxiomTheme.type.uiBody, color = AxiomTheme.colors.inkMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    style = AxiomTheme.type.uiLabel,
                    color = if (destructive) AxiomTheme.colors.critical else AxiomTheme.colors.accent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = AxiomTheme.type.uiLabel, color = AxiomTheme.colors.inkMuted)
            }
        },
        shape = AxiomTheme.shapes.lg
    )
}

/**
 * Destructive actions are undoable rather than confirmed where possible.
 * Returns true when the user tapped Undo.
 */
suspend fun SnackbarHostState.showUndo(message: String): Boolean =
    showSnackbar(message = message, actionLabel = "Undo") == SnackbarResult.ActionPerformed
