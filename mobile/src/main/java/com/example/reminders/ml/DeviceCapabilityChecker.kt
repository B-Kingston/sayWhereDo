package com.example.reminders.ml

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs

/**
 * Checks the device's hardware capabilities to determine whether
 * on-device LLM inference is feasible.
 *
 * Examines total RAM, available storage, CPU cores, supported ABIs,
 * and Android version. Assigns a [CapabilityLevel] based on defined
 * thresholds:
 * - **Recommended:** ≥8 GB RAM, ≥4 GB storage, arm64-v8a, Android 12+
 * - **Minimum:** ≥6 GB RAM, ≥2 GB storage, arm64-v8a
 * - **Not supported:** Below minimum thresholds or no arm64-v8a
 *
 * @param context Application context for accessing system services.
 */
class DeviceCapabilityChecker(private val context: Context) {

    /**
     * Performs the capability check and returns a [DeviceCapabilities]
     * snapshot of the current device state.
     */
    fun check(): DeviceCapabilities {
        val totalRamMb = getTotalRamMb()
        val availableStorageMb = getAvailableStorageMb()
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val androidVersion = Build.VERSION.SDK_INT
        val hasNpu = supportedAbis.contains(REQUIRED_ABI)

        val capabilityLevel = determineCapabilityLevel(
            totalRamMb = totalRamMb,
            availableStorageMb = availableStorageMb,
            supportedAbis = supportedAbis
        )

        return DeviceCapabilities(
            totalRamMb = totalRamMb,
            availableStorageMb = availableStorageMb,
            cpuCores = cpuCores,
            hasNpu = hasNpu,
            supportedAbis = supportedAbis,
            androidVersion = androidVersion,
            capabilityLevel = capabilityLevel
        )
    }

    private fun getTotalRamMb(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / BYTES_PER_MB
    }

    private fun getAvailableStorageMb(): Long {
        val statFs = StatFs(context.filesDir.absolutePath)
        return statFs.availableBlocksLong * statFs.blockSizeLong / BYTES_PER_MB
    }

    companion object {
        const val RECOMMENDED_RAM_MB = 8192L
        const val MINIMUM_RAM_MB = 6144L
        const val RECOMMENDED_STORAGE_MB = 4096L
        const val MINIMUM_STORAGE_MB = 2048L
        const val REQUIRED_ABI = "arm64-v8a"
        private const val BYTES_PER_MB = 1024L * 1024L
    }
}

/**
 * Determines the [CapabilityLevel] for a device based on its hardware specs.
 *
 * Extracted as a top-level function for testability without Android
 * framework dependencies.
 *
 * @param totalRamMb Total device RAM in megabytes.
 * @param availableStorageMb Available internal storage in megabytes.
 * @param supportedAbis List of supported CPU ABIs (e.g. "arm64-v8a").
 * @return The appropriate [CapabilityLevel] for the device.
 */
internal fun determineCapabilityLevel(
    totalRamMb: Long,
    availableStorageMb: Long,
    supportedAbis: List<String>
): CapabilityLevel {
    val hasArm64 = supportedAbis.contains(DeviceCapabilityChecker.REQUIRED_ABI)

    if (!hasArm64) {
        return CapabilityLevel.NOT_SUPPORTED
    }

    val meetsRecommended = totalRamMb >= DeviceCapabilityChecker.RECOMMENDED_RAM_MB &&
        availableStorageMb >= DeviceCapabilityChecker.RECOMMENDED_STORAGE_MB

    if (meetsRecommended) {
        return CapabilityLevel.RECOMMENDED
    }

    val meetsMinimum = totalRamMb >= DeviceCapabilityChecker.MINIMUM_RAM_MB &&
        availableStorageMb >= DeviceCapabilityChecker.MINIMUM_STORAGE_MB

    return if (meetsMinimum) {
        CapabilityLevel.MINIMUM
    } else {
        CapabilityLevel.NOT_SUPPORTED
    }
}
