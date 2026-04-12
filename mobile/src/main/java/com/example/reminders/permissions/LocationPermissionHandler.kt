package com.example.reminders.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Holds the current location permission state and functions to
 * trigger the system permission dialogs.
 */
data class LocationPermissionState(
    val isFineLocationGranted: Boolean,
    val isBackgroundLocationGranted: Boolean,
    val isPostNotificationsGranted: Boolean,
    val requestFineLocation: () -> Unit,
    val requestBackgroundLocation: () -> Unit,
    val requestPostNotifications: () -> Unit,
    val isAllGrantedForGeofencing: Boolean
)

/**
 * Composable helper that encapsulates the full location permission flow
 * required for geofencing, including:
 *
 * 1. [ACCESS_FINE_LOCATION] — requested first via system dialog.
 * 2. [ACCESS_BACKGROUND_LOCATION] — must be requested in a separate session
 *    on Android 11+. The user is taken to Settings to grant "Allow all the time".
 * 3. [POST_NOTIFICATIONS] — required to show geofence trigger notifications.
 *
 * @return A [LocationPermissionState] that can be read and acted upon
 *         by the calling screen.
 */
@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current

    var fineLocationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var backgroundLocationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var postNotificationsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        fineLocationGranted = granted
    }

    // Background location must be requested separately on Android 11+
    // The system dialog sends the user to Settings
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundLocationGranted = granted
    }

    val postNotificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        postNotificationsGranted = granted
    }

    return LocationPermissionState(
        isFineLocationGranted = fineLocationGranted,
        isBackgroundLocationGranted = backgroundLocationGranted,
        isPostNotificationsGranted = postNotificationsGranted,
        requestFineLocation = { fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
        requestBackgroundLocation = {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        },
        requestPostNotifications = {
            postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        isAllGrantedForGeofencing = fineLocationGranted &&
            backgroundLocationGranted &&
            postNotificationsGranted
    )
}

/**
 * Opens the application-specific settings page where the user can grant
 * "Allow all the time" location access on Android 11+.
 */
fun openAppLocationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
