package com.example.reminders.wear.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.reminders.wear.presentation.theme.RemindersTheme
import com.example.reminders.wear.ui.screen.ComplicationConfigScreen
import com.example.reminders.wear.ui.screen.GeofencingPreferenceScreen
import com.example.reminders.wear.ui.screen.KeyboardInputScreen
import com.example.reminders.wear.ui.screen.ReminderDetailScreen
import com.example.reminders.wear.ui.screen.VoiceRecordScreen
import com.example.reminders.wear.ui.screen.WatchReminderListScreen
import com.example.reminders.wear.ui.viewmodel.ReminderDetailViewModel
import com.example.reminders.wear.ui.viewmodel.VoiceRecordViewModel
import com.example.reminders.wear.ui.viewmodel.WatchReminderListViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate called")

        val container = (application as com.example.reminders.wear.di.WatchRemindersApplication).container
        container.wearDataLayerClient.startMonitoring()

        setContent {
            RemindersTheme {
                val navController = rememberSwipeDismissableNavController()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = ROUTE_REMINDER_LIST
                ) {
                    composable(ROUTE_REMINDER_LIST) {
                        val listViewModel: WatchReminderListViewModel = viewModel(
                            factory = WatchReminderListViewModel.Factory(
                                container.watchReminderRepository,
                                container.watchAlarmScheduler
                            )
                        )
                        val isPhoneConnected by container.wearDataLayerClient.isPhoneConnected
                            .collectAsStateWithLifecycle(initialValue = false)

                        WatchReminderListScreen(
                            viewModel = listViewModel,
                            isPhoneConnected = isPhoneConnected,
                            onNavigateToVoiceRecord = {
                                navController.navigate(ROUTE_VOICE_RECORD)
                            },
                            onNavigateToDetail = { reminderId ->
                                navController.navigate("$ROUTE_REMINDER_DETAIL/$reminderId")
                            },
                            onNavigateToGeofencingPrefs = {
                                navController.navigate(ROUTE_GEOFENCING_PREFS)
                            }
                        )
                    }

                    composable(ROUTE_VOICE_RECORD) {
                        val voiceViewModel: VoiceRecordViewModel = viewModel(
                            factory = VoiceRecordViewModel.Factory(
                                container.watchReminderRepository,
                                container.watchAlarmScheduler,
                                container.wearDataLayerClient
                            )
                        )
                        VoiceRecordScreen(
                            viewModel = voiceViewModel,
                            onNavigateToKeyboard = {
                                navController.navigate(ROUTE_KEYBOARD_INPUT)
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(ROUTE_KEYBOARD_INPUT) {
                        val voiceViewModel: VoiceRecordViewModel = viewModel(
                            factory = VoiceRecordViewModel.Factory(
                                container.watchReminderRepository,
                                container.watchAlarmScheduler,
                                container.wearDataLayerClient
                            )
                        )
                        KeyboardInputScreen(
                            viewModel = voiceViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("$ROUTE_REMINDER_DETAIL/{reminderId}") { backStackEntry ->
                        val reminderId = backStackEntry.arguments?.getString("reminderId")
                            ?: return@composable
                        val detailViewModel: ReminderDetailViewModel = viewModel(
                            factory = ReminderDetailViewModel.Factory(
                                container.watchReminderRepository,
                                container.watchAlarmScheduler,
                                reminderId
                            )
                        )
                        ReminderDetailScreen(
                            viewModel = detailViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(ROUTE_GEOFENCING_PREFS) {
                        GeofencingPreferenceScreen(
                            deviceManager = container.geofencingDeviceManager,
                            hasGps = container.gpsDetector.hasGps()
                        )
                    }

                    composable(ROUTE_COMPLICATION_CONFIG) {
                        ComplicationConfigScreen(
                            preferences = container.complicationPreferences
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WatchMainActivity"
        private const val ROUTE_REMINDER_LIST = "reminder-list"
        private const val ROUTE_VOICE_RECORD = "voice-record"
        private const val ROUTE_KEYBOARD_INPUT = "keyboard-input"
        private const val ROUTE_REMINDER_DETAIL = "reminder-detail"
        private const val ROUTE_GEOFENCING_PREFS = "geofencing-prefs"
        private const val ROUTE_COMPLICATION_CONFIG = "complication-config"
    }
}
