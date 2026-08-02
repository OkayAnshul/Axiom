package com.cosmiclaboratory.axiom.data.database.seed

import kotlinx.serialization.Serializable

@Serializable
internal data class CuratedPromptsAsset(
    val version: Int,
    val prompts: List<CuratedPromptDto>
)

@Serializable
internal data class CuratedPromptDto(
    val theme: String,
    val text: String
)
