package com.cosmiclaboratory.axiom.domain.model

import androidx.compose.ui.graphics.Color

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Long = Color.Blue.value.toLong(),
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)