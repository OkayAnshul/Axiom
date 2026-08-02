package com.cosmiclaboratory.axiom.domain.model

enum class EntryKind {
    FREE_FORM,
    PROMPTED,
    VOICE;

    companion object {
        fun fromStorage(raw: String?): EntryKind = runCatching { valueOf(raw ?: FREE_FORM.name) }.getOrDefault(FREE_FORM)
    }
}
