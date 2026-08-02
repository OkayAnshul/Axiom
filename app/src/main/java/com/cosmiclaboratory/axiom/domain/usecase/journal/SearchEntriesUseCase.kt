package com.cosmiclaboratory.axiom.domain.usecase.journal

import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import javax.inject.Inject

class SearchEntriesUseCase @Inject constructor(
    private val journalRepo: JournalRepository
) {
    suspend operator fun invoke(query: String): List<Entry> = journalRepo.search(query)
}
