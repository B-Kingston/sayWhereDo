package com.example.reminders.ml

/**
 * Describes the hardware capabilities of the device relevant to
 * on-device LLM inference.
 *
 * Used by [DeviceCapabilityChecker] to determine whether the device
 * can run local AI models and at what performance level.
 */
data class DeviceCapabilities(
    val totalRamMb: Long,
    val availableStorageMb: Long,
    val cpuCores: Int,
    val hasNpu: Boolean,
    val supportedAbis: List<String>,
    val androidVersion: Int,
    val capabilityLevel: CapabilityLevel
)

/**
 * Categorises the device's ability to run on-device LLM inference.
 *
 * [RECOMMENDED] — Device meets or exceeds all recommended thresholds.
 * [MINIMUM] — Device meets minimum thresholds but may be slow.
 * [NOT_SUPPORTED] — Device does not meet minimum requirements.
 */
enum class CapabilityLevel {
    RECOMMENDED,
    MINIMUM,
    NOT_SUPPORTED
}
