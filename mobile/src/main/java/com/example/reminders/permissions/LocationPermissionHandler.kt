package com.example.reminders.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Holds the current location permission state and a function to
 * trigger the system permission dialog.
 */
data class LocationPermissionState(
    val isGranted: Boolean,
    val request: () -> Unit
)

/**
 * Composable helper that encapsulates the [ACCESS_FINE_LOCATION]
 * permission flow using the Activity Result API.
 *
 * Background location is intentionally omitted here — it will be
 * introduced in Phase 5 (geofencing).
 *
 * @return A [LocationPermissionState] that can be read and acted upon
 *         by the calling screen.
 */
@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current

    val isGranted = remember {
        context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Result is reflected on next recomposition via isGranted check */ }

    return LocationPermissionState(
        isGranted = isGranted,
        request = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    )
}
