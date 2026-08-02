package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class AxiomShapes(
    val xs: CornerBasedShape = RoundedCornerShape(6.dp),    // chips, tags
    val sm: CornerBasedShape = RoundedCornerShape(10.dp),   // buttons, fields
    val md: CornerBasedShape = RoundedCornerShape(16.dp),   // cards, list rows
    val lg: CornerBasedShape = RoundedCornerShape(24.dp),   // sheets, dialogs
    val xl: CornerBasedShape = RoundedCornerShape(28.dp),   // FAB, hero
    val full: Shape = CircleShape
)

fun AxiomShapes.toMaterialShapes(): Shapes = Shapes(
    extraSmall = xs,
    small = sm,
    medium = md,
    large = lg,
    extraLarge = xl
)
