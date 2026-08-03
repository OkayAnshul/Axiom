package com.cosmiclaboratory.axiom.domain.model

enum class EntryKind {
    FREE_FORM,
    PROMPTED,
    VOICE,

    /** Auto-digested from a companion conversation session — journaling that emerged from talking. */
    CONVERSATION;

    companion object {
        fun fromStorage(raw: String?): EntryKind = runCatching { valueOf(raw ?: FREE_FORM.name) }.getOrDefault(FREE_FORM)
    }
}
