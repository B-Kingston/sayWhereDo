package com.example.reminders.data.repository

import com.example.reminders.data.local.SavedPlaceDao
import com.example.reminders.data.model.SavedPlace
import kotlinx.coroutines.flow.Flow

class SavedPlaceRepositoryImpl(
    private val savedPlaceDao: SavedPlaceDao
) : SavedPlaceRepository {

    override fun getAll(): Flow<List<SavedPlace>> = savedPlaceDao.getAll()

    override suspend fun getById(id: String): SavedPlace? = savedPlaceDao.getById(id)

    override suspend fun findByLabel(label: String): SavedPlace? = savedPlaceDao.findByLabel(label)

    override suspend fun insert(place: SavedPlace) = savedPlaceDao.insert(place)

    override suspend fun delete(place: SavedPlace) = savedPlaceDao.delete(place)

    override suspend fun deleteById(id: String) = savedPlaceDao.deleteById(id)

    override suspend fun count(): Int = savedPlaceDao.count()
}
