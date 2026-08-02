package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.cosmiclaboratory.axiom.domain.model.Entry

/**
 * An entry and its tags, fetched by Room in one pass.
 *
 * This exists to kill an N+1: the previous repository called a suspend
 * `tagsFor(id)` inside the Flow's `map` for every entry, on every emission, so a
 * 500-entry library issued 501 queries each time anything changed.
 */
data class EntryWithTags(
    @Embedded val entry: EntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)

fun EntryWithTags.toDomainModel(): Entry =
    entry.toDomainModel(tags.map { it.toDomainModel() })
