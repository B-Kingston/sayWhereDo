package com.example.reminders.data.repository

import com.example.reminders.data.model.SavedPlace
import kotlinx.coroutines.flow.Flow

interface SavedPlaceRepository {
    fun getAll(): Flow<List<SavedPlace>>
    suspend fun getById(id: String): SavedPlace?
    suspend fun findByLabel(label: String): SavedPlace?
    suspend fun insert(place: SavedPlace)
    suspend fun delete(place: SavedPlace)
    suspend fun deleteById(id: String)
    suspend fun count(): Int
}
