package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Full-text index over entries.
 *
 * `unicode61` matters: the default `simple` tokenizer treats every non-ASCII
 * byte as a non-word character, so Devanagari and accented Latin were silently
 * unsearchable — a real bug for an app whose voice layer ships Hindi and
 * Hinglish. It also folds diacritics, so "cafe" finds "café".
 *
 * [promptSnapshot] is indexed so a guided entry is findable by its question,
 * not just by the answer the user typed.
 */
@Fts4(
    contentEntity = EntryEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61
)
@Entity(tableName = "entry_fts")
data class EntryFts(
    val title: String,
    val content: String,
    val promptSnapshot: String?
)
