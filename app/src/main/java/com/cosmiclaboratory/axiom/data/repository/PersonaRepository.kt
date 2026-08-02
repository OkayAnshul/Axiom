package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.PersonaDao
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.domain.model.Persona
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonaRepository @Inject constructor(
    private val dao: PersonaDao
) {
    fun observeAll(): Flow<List<Persona>> = dao.observeAll().map { rows -> rows.map { it.toDomainModel() } }

    suspend fun getByKey(key: PersonaKey): Persona? =
        dao.getByKey(key.storageValue)?.toDomainModel()
}
