package com.example.reminders.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reminders.data.model.SavedPlace
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: SavedPlace)

    @Delete
    suspend fun delete(place: SavedPlace)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM saved_places WHERE id = :id")
    suspend fun getById(id: String): SavedPlace?

    @Query("SELECT * FROM saved_places ORDER BY label ASC")
    fun getAll(): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places WHERE LOWER(label) = LOWER(:label) LIMIT 1")
    suspend fun findByLabel(label: String): SavedPlace?

    @Query("SELECT COUNT(*) FROM saved_places")
    suspend fun count(): Int
}
