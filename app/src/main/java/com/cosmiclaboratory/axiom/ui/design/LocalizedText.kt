package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/**
 * The current locale, read observably.
 *
 * `Locale.getDefault()` inside a composable is a real bug, not a lint nit: it is
 * not a Compose state read, so changing the device or per-app language leaves
 * every already-composed date, month name and weekday stale until the process
 * restarts. Reading through LocalConfiguration makes the locale a recomposition
 * trigger, so text updates the moment it changes.
 *
 * This matters more than usual here — the app declares `localeConfig`, so the
 * per-app language picker is offered to users and expected to work.
 */
// The Locale.getDefault() below is the ONE legitimate use in a composable: it is
// the fallback for a configuration that reports no locales at all, which the
// observable path cannot supply. Every other call site goes through this function.
@Suppress("NonObservableLocale")
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale =
    ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

/**
 * Remembers a value derived from the locale, recomputing when it changes.
 * Use for DateTimeFormatter instances, which are expensive to build per frame.
 */
@Composable
fun <T> rememberLocalized(vararg keys: Any?, block: (Locale) -> T): T {
    val locale = currentLocale()
    return remember(locale, *keys) { block(locale) }
}
