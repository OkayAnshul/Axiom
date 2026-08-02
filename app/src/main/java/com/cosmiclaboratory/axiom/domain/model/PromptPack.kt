package com.cosmiclaboratory.axiom.domain.model

data class PromptPack(
    val id: Long,
    val name: String,
    val theme: String,
    val isBuiltIn: Boolean
)
