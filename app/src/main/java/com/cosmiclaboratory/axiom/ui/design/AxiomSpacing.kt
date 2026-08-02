package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4dp grid. The named applications below matter more than the raw scale — they
 * are what keep two screens built weeks apart looking like the same app.
 */
@Immutable
data class AxiomSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val base: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    val huge: Dp = 64.dp,

    // Named applications
    val screenH: Dp = 20.dp,
    val screenV: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val listGap: Dp = 12.dp,
    val sectionGap: Dp = 28.dp,
    val composerGutter: Dp = 20.dp
)
