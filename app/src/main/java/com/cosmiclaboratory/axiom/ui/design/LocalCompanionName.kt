package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import com.cosmiclaboratory.axiom.domain.model.CompanionIdentity

/**
 * What the user calls their companion, available anywhere without plumbing.
 *
 * A CompositionLocal rather than a parameter because this is identity, not
 * screen state: the bottom bar needs it on two screens whose view models have
 * nothing else in common, and threading it through both would put an app-wide
 * fact into two unrelated UI states.
 *
 * `staticCompositionLocalOf` invalidates the whole tree when it changes, which
 * is exactly right here and exactly wrong for the palette — a name changes when
 * someone types a new one, not sixty times a second.
 */
val LocalCompanionName = staticCompositionLocalOf { CompanionIdentity.DEFAULT_NAME }
