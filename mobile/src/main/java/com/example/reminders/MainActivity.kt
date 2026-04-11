package com.example.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.reminders.di.RemindersApplication
import com.example.reminders.transcription.AndroidSpeechRecognitionManager
import com.example.reminders.ui.screen.GeocodingConfirmationScreen
import com.example.reminders.ui.screen.ReminderListScreen
import com.example.reminders.ui.screen.ReminderListUiState
import com.example.reminders.ui.screen.SavedPlacesScreen
import com.example.reminders.ui.screen.SettingsScreen
import com.example.reminders.ui.screen.TranscriptionScreen
import com.example.reminders.ui.theme.RemindersTheme
import com.example.reminders.ui.viewmodel.SavedPlacesViewModel
import com.example.reminders.ui.viewmodel.SavedPlacesViewModelFactory
import com.example.reminders.ui.viewmodel.TranscriptionViewModel
import com.example.reminders.ui.viewmodel.TranscriptionViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as RemindersApplication).container

        setContent {
            RemindersTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_REMINDERS
                    ) {
                        composable(ROUTE_REMINDERS) {
                            // TODO: Phase 2 — Replace hardcoded state with a proper ViewModel
                            val uiState = ReminderListUiState.Success(emptyList())

                            ReminderListScreen(
                                uiState = uiState,
                                onRecordReminder = { navController.navigate(ROUTE_TRANSCRIPTION) },
                                onSettings = { navController.navigate(ROUTE_SETTINGS) }
                            )
                        }

                        composable(ROUTE_TRANSCRIPTION) {
                            val speechManager = AndroidSpeechRecognitionManager(this@MainActivity)
                            val viewModel: TranscriptionViewModel = viewModel(
                                factory = TranscriptionViewModelFactory(speechManager)
                            )

                            TranscriptionScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            val apiKey by container.userPreferences.apiKey
                                .collectAsStateWithLifecycle(initialValue = null)

                            SettingsScreen(
                                currentApiKey = apiKey,
                                onSaveApiKey = { key ->
                                    scope.launch {
                                        container.userPreferences.setApiKey(key)
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_SAVED_PLACES) {
                            val savedPlacesViewModel: SavedPlacesViewModel = viewModel(
                                factory = SavedPlacesViewModelFactory(
                                    savedPlaceRepository = container.savedPlaceRepository,
                                    geocodingService = container.geocodingService,
                                    billingManager = container.billingManager
                                )
                            )

                            SavedPlacesScreen(
                                viewModel = savedPlacesViewModel,
                                onBack = { navController.popBackStack() },
                                onUpgradeToPro = {
                                    container.billingManager.launchBillingFlow(this@MainActivity)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val ROUTE_REMINDERS = "reminders"
        private const val ROUTE_TRANSCRIPTION = "transcription"
        private const val ROUTE_SETTINGS = "settings"
        private const val ROUTE_SAVED_PLACES = "saved-places"
        const val ROUTE_GEOCODING_CONFIRMATION = "geocoding-confirmation"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
