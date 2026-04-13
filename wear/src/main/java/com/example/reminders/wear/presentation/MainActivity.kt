package com.example.reminders.wear.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.reminders.wear.presentation.theme.RemindersTheme
import com.example.reminders.wear.ui.screen.ComplicationConfigScreen
import com.example.reminders.wear.ui.screen.KeyboardInputScreen
import com.example.reminders.wear.ui.screen.ReminderDetailScreen
import com.example.reminders.wear.ui.screen.StreamToPhoneScreen
import com.example.reminders.wear.ui.screen.VoiceRecordScreen
import com.example.reminders.wear.ui.screen.WatchReminderListScreen
import com.example.reminders.wear.ui.screen.WatchSettingsScreen
import com.example.reminders.wear.ui.viewmodel.ReminderDetailViewModel
import com.example.reminders.wear.ui.viewmodel.StreamToPhoneViewModel
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
                            onNavigateToSettings = {
                                navController.navigate(ROUTE_SETTINGS)
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
                            onNavigateToStreamToPhone = {
                                navController.navigate(ROUTE_STREAM_TO_PHONE)
                            },
                            onNavigateToCloudFormat = {
                                navController.navigate(ROUTE_CLOUD_FORMAT)
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

                    composable(ROUTE_STREAM_TO_PHONE) {
                        val streamViewModel: StreamToPhoneViewModel = viewModel()
                        StreamToPhoneScreen(
                            viewModel = streamViewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(ROUTE_CLOUD_FORMAT) {
                        Text(
                            text = "Cloud format — coming soon",
                            modifier = Modifier.fillMaxSize().wrapContentSize()
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

                    composable(ROUTE_SETTINGS) {
                        val isPhoneConnected by container.wearDataLayerClient.isPhoneConnected
                            .collectAsStateWithLifecycle(initialValue = false)

                        WatchSettingsScreen(
                            isPhoneConnected = isPhoneConnected,
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
        private const val ROUTE_STREAM_TO_PHONE = "stream-to-phone"
        private const val ROUTE_CLOUD_FORMAT = "cloud-format"
        private const val ROUTE_REMINDER_DETAIL = "reminder-detail"
        private const val ROUTE_SETTINGS = "settings"
        private const val ROUTE_COMPLICATION_CONFIG = "complication-config"
    }
}
