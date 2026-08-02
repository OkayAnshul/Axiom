package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide snackbar host, provided once by AxiomNavigation's Scaffold.
 *
 * Before this existed, three screens caught an error into `uiState.errorMessage` and
 * dropped it behind a `// TODO: Show snackbar`, so save/load/search failures were
 * completely silent. Screens now surface errors through [ShowSnackbarOnError].
 */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * Shows [message] whenever it becomes non-null, then invokes [onShown] so the caller
 * can clear it from its UI state. Keyed on the message so an identical error raised
 * twice in a row still re-shows.
 */
@Composable
fun ShowSnackbarOnError(
    message: String?,
    onShown: () -> Unit
) {
    val host = LocalSnackbarHostState.current
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            host.showSnackbar(message = message, duration = SnackbarDuration.Short)
            onShown()
        }
    }
}
