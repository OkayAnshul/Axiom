package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.data.ai.dto.ChatMessage
import com.cosmiclaboratory.axiom.domain.model.Persona
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonaPromptBuilder @Inject constructor() {

    internal fun buildMessages(
        persona: Persona,
        userPrompt: String
    ): List<ChatMessage> = listOf(
        ChatMessage(role = "system", content = persona.systemPromptFragment),
        ChatMessage(role = "user", content = userPrompt)
    )
}
