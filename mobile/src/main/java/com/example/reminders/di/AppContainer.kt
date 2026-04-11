package com.example.reminders.di

import android.content.Context
import android.location.Geocoder
import androidx.room.Room
import com.example.reminders.billing.BillingManager
import com.example.reminders.data.local.RemindersDatabase
import com.example.reminders.data.preferences.UsageTracker
import com.example.reminders.data.preferences.UserPreferences
import com.example.reminders.data.repository.ReminderRepository
import com.example.reminders.data.repository.ReminderRepositoryImpl
import com.example.reminders.data.repository.SavedPlaceRepository
import com.example.reminders.data.repository.SavedPlaceRepositoryImpl
import com.example.reminders.formatting.GeminiFormattingProvider
import com.example.reminders.formatting.RawFallbackProvider
import com.example.reminders.geocoding.AndroidGeocodingService
import com.example.reminders.geocoding.SavedPlaceMatcher
import com.example.reminders.network.GeminiApiClient
import com.example.reminders.pipeline.PipelineOrchestrator
import kotlinx.coroutines.flow.first

class AppContainer(context: Context) {

    private val database: RemindersDatabase = Room.databaseBuilder(
        context,
        RemindersDatabase::class.java,
        "reminders-db"
    ).build()

    val reminderRepository: ReminderRepository = ReminderRepositoryImpl(
        database.reminderDao()
    )

    val savedPlaceRepository: SavedPlaceRepository = SavedPlaceRepositoryImpl(
        database.savedPlaceDao()
    )

    val geocodingService = AndroidGeocodingService(Geocoder(context))

    val savedPlaceMatcher = SavedPlaceMatcher(savedPlaceRepository)

    val userPreferences = UserPreferences(context)
    val usageTracker = UsageTracker(context)
    val billingManager = BillingManager(context)

    val geminiApiClient = GeminiApiClient()

    val geminiFormattingProvider = GeminiFormattingProvider(
        apiClient = geminiApiClient,
        apiKeyProvider = { userPreferences.apiKey.first() ?: "" }
    )

    val rawFallbackProvider = RawFallbackProvider()

    val pipelineOrchestrator = PipelineOrchestrator(
        formattingProvider = geminiFormattingProvider,
        rawFallbackProvider = rawFallbackProvider,
        reminderRepository = reminderRepository,
        usageTracker = usageTracker,
        billingManager = billingManager,
        userPreferences = userPreferences
    )
}
