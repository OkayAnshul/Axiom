package com.cosmiclaboratory.axiom.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations.
 *
 * Replaces string routes plus `createRoute()` builders plus
 * `savedStateHandle.get<Long>("noteId")`. That triple was a standing source of
 * bugs — an arg name typo compiled fine and failed at runtime, and the old
 * MainActivity popped to route "notes_list", which had not existed since the
 * rename to "library", so the share deep link silently never trimmed its stack.
 */

/** Wrapper subgraph for the four tabs. Bottom bar shows only inside this. */
@Serializable data object Main

@Serializable data object Today
@Serializable data object Journal
@Serializable data object Patterns
@Serializable data object Ask

@Serializable data object Onboarding

/**
 * One composer for every kind of writing.
 * - [entryId] null => new entry
 * - [questionId] non-null => guided prompt, rendered with its question header
 * - [voice] => open the voice sheet immediately (QS tile / widget shortcut)
 */
@Serializable
data class Composer(
    val entryId: Long? = null,
    val questionId: Long? = null,
    val initialText: String? = null,
    val voice: Boolean = false
)

@Serializable data class Reader(val entryId: Long)
@Serializable data class Search(val initialQuery: String = "")
@Serializable data object Calendar

@Serializable data object Settings
@Serializable data object SettingsAi
@Serializable data object SettingsAppearance

/** Deep-link URIs. Declared once so widgets, the tile and notifications agree. */
object AxiomDeepLinks {
    const val SCHEME = "axiom"
    const val COMPOSER = "$SCHEME://composer"
    const val COMPOSER_VOICE = "$SCHEME://composer?voice=true"
    const val TODAY = "$SCHEME://today"
    const val JOURNAL = "$SCHEME://journal"
    const val PATTERNS = "$SCHEME://patterns"
    const val SETTINGS_AI = "$SCHEME://settings/ai"
    fun reader(id: Long) = "$SCHEME://reader/$id"
}
