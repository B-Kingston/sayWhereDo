package com.example.reminders

import android.os.Bundle
import android.util.Log
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
import com.example.reminders.data.export.ReminderExporter
import com.example.reminders.di.RemindersApplication
import com.example.reminders.transcription.AndroidSpeechRecognitionManager
import com.example.reminders.ui.screen.GeocodingConfirmationScreen
import com.example.reminders.ui.screen.KeyboardInputScreen
import com.example.reminders.ui.screen.ReminderEditScreen
import com.example.reminders.ui.screen.ReminderListScreen
import com.example.reminders.ui.screen.ReminderListUiState
import com.example.reminders.ui.screen.SavedPlacesScreen
import com.example.reminders.ui.screen.SettingsScreen
import com.example.reminders.ui.screen.TranscriptionScreen
import com.example.reminders.ui.theme.RemindersTheme
import com.example.reminders.ui.viewmodel.KeyboardInputViewModel
import com.example.reminders.ui.viewmodel.ProSettingsViewModel
import com.example.reminders.ui.viewmodel.ProSettingsViewModelFactory
import com.example.reminders.ui.viewmodel.ReminderEditViewModel
import com.example.reminders.ui.viewmodel.ReminderEditViewModelFactory
import com.example.reminders.ui.viewmodel.SavedPlacesViewModel
import com.example.reminders.ui.viewmodel.SavedPlacesViewModelFactory
import com.example.reminders.ui.viewmodel.TranscriptionViewModel
import com.example.reminders.ui.viewmodel.TranscriptionViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
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
                            Log.d(TAG, "Navigated to $ROUTE_REMINDERS")
                            // TODO: Phase 2 — Replace hardcoded state with a proper ViewModel
                            val uiState = ReminderListUiState.Success(emptyList())

                            ReminderListScreen(
                                uiState = uiState,
                                onRecordReminder = { navController.navigate(ROUTE_TRANSCRIPTION) },
                                onKeyboardInput = { navController.navigate(ROUTE_KEYBOARD_INPUT) },
                                onSettings = { navController.navigate(ROUTE_SETTINGS) }
                            )
                        }

                        composable(ROUTE_TRANSCRIPTION) {
                            Log.d(TAG, "Navigated to $ROUTE_TRANSCRIPTION")
                            val speechManager = AndroidSpeechRecognitionManager(this@MainActivity)
                            val viewModel: TranscriptionViewModel = viewModel(
                                factory = TranscriptionViewModelFactory(speechManager)
                            )

                            TranscriptionScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_KEYBOARD_INPUT) {
                            Log.d(TAG, "Navigated to $ROUTE_KEYBOARD_INPUT")
                            val keyboardViewModel: KeyboardInputViewModel = viewModel(
                                factory = KeyboardInputViewModel.Factory(
                                    pipelineOrchestrator = container.pipelineOrchestrator
                                )
                            )

                            KeyboardInputScreen(
                                viewModel = keyboardViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            Log.d(TAG, "Navigated to $ROUTE_SETTINGS")
                            val apiKey by container.userPreferences.apiKey
                                .collectAsStateWithLifecycle(initialValue = null)

                            val proViewModel: ProSettingsViewModel = viewModel(
                                factory = ProSettingsViewModelFactory(
                                    billingManager = container.billingManager,
                                    reminderRepository = container.reminderRepository,
                                    savedPlaceRepository = container.savedPlaceRepository,
                                    reminderExporter = ReminderExporter()
                                )
                            )

                            SettingsScreen(
                                currentApiKey = apiKey,
                                proViewModel = proViewModel,
                                onSaveApiKey = { key ->
                                    scope.launch {
                                        container.userPreferences.setApiKey(key)
                                    }
                                },
                                onBack = { navController.popBackStack() },
                                onUpgrade = {
                                    container.billingManager.launchBillingFlow(this@MainActivity)
                                },
                                onExport = { json ->
                                    // Share or save the exported JSON
                                    shareExportedData(json)
                                },
                                onImport = {
                                    // TODO: Launch file picker for import
                                }
                            )
                        }

                        composable(ROUTE_SAVED_PLACES) {
                            Log.d(TAG, "Navigated to $ROUTE_SAVED_PLACES")
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

                        composable(
                            route = "$ROUTE_EDIT_REMINDER/{reminderId}"
                        ) { backStackEntry ->
                            val reminderId = backStackEntry.arguments?.getString("reminderId")
                                ?: run { navController.popBackStack(); return@composable }
                            Log.d(TAG, "Navigated to $ROUTE_EDIT_REMINDER with reminderId=$reminderId")

                            val editViewModel: ReminderEditViewModel = viewModel(
                                factory = ReminderEditViewModelFactory(
                                    reminderRepository = container.reminderRepository,
                                    alarmScheduler = container.alarmScheduler,
                                    billingManager = container.billingManager,
                                    reminderId = reminderId
                                )
                            )

                            ReminderEditScreen(
                                viewModel = editViewModel,
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

    /**
     * Launches a share intent with the exported JSON data.
     */
    private fun shareExportedData(json: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_TEXT, json)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Export reminders"))
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val ROUTE_REMINDERS = "reminders"
        private const val ROUTE_TRANSCRIPTION = "transcription"
        private const val ROUTE_KEYBOARD_INPUT = "keyboard-input"
        private const val ROUTE_SETTINGS = "settings"
        private const val ROUTE_SAVED_PLACES = "saved-places"
        private const val ROUTE_EDIT_REMINDER = "edit-reminder"
        const val ROUTE_GEOCODING_CONFIRMATION = "geocoding-confirmation"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
