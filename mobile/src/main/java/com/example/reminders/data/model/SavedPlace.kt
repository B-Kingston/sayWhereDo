package com.example.reminders.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places")
data class SavedPlace(
    @PrimaryKey
    val id: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val defaultRadiusMetres: Int = 150
)
