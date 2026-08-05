package com.cosmiclaboratory.axiom.domain.model

/**
 * What the companion is called.
 *
 * Naming a thing is how people start treating it as a presence rather than a
 * feature, so this is the one string in the app the user should be able to
 * overwrite. It reaches the bottom bar, the settings row that sets it, and the
 * system prompt — a companion that does not know its own name will contradict
 * the label the user is looking at.
 *
 * This is deliberately NOT the launcher label. Android fixes that at build time
 * in the manifest; the only way to vary it is a set of `<activity-alias>` entries
 * declared in advance and toggled with PackageManager, which can only offer names
 * chosen by us, and which drops and re-adds the icon — losing its place on the
 * home screen and breaking pinned shortcuts. An arbitrary name on the launcher
 * is not something the platform allows.
 */
object CompanionIdentity {
    const val DEFAULT_NAME = "Axiom"

    /** Long enough for a real name, short enough to sit in a bottom bar tab. */
    const val MAX_LENGTH = 20

    /** Falls back rather than rendering an empty tab or an anonymous prompt. */
    fun resolve(stored: String?): String =
        stored?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_LENGTH) ?: DEFAULT_NAME
}
