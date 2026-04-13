package com.example.reminders.ml

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeviceCapabilityCheckerTest {

    @Test
    fun `recommended device returns RECOMMENDED`() {
        val level = determineCapabilityLevel(
            totalRamMb = 8192,
            availableStorageMb = 4096,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.RECOMMENDED)
    }

    @Test
    fun `device with more than recommended RAM returns RECOMMENDED`() {
        val level = determineCapabilityLevel(
            totalRamMb = 16384,
            availableStorageMb = 32000,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.RECOMMENDED)
    }

    @Test
    fun `minimum device returns MINIMUM`() {
        val level = determineCapabilityLevel(
            totalRamMb = 6144,
            availableStorageMb = 2048,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.MINIMUM)
    }

    @Test
    fun `device between minimum and recommended returns MINIMUM`() {
        val level = determineCapabilityLevel(
            totalRamMb = 7000,
            availableStorageMb = 3000,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.MINIMUM)
    }

    @Test
    fun `device below minimum RAM returns NOT_SUPPORTED`() {
        val level = determineCapabilityLevel(
            totalRamMb = 4000,
            availableStorageMb = 4096,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.NOT_SUPPORTED)
    }

    @Test
    fun `device below minimum storage returns NOT_SUPPORTED`() {
        val level = determineCapabilityLevel(
            totalRamMb = 8192,
            availableStorageMb = 1000,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.NOT_SUPPORTED)
    }

    @Test
    fun `device without arm64 returns NOT_SUPPORTED regardless of RAM`() {
        val level = determineCapabilityLevel(
            totalRamMb = 16384,
            availableStorageMb = 32000,
            supportedAbis = listOf("armeabi-v7a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.NOT_SUPPORTED)
    }

    @Test
    fun `device with exactly recommended RAM and storage returns RECOMMENDED`() {
        val level = determineCapabilityLevel(
            totalRamMb = DeviceCapabilityChecker.RECOMMENDED_RAM_MB,
            availableStorageMb = DeviceCapabilityChecker.RECOMMENDED_STORAGE_MB,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.RECOMMENDED)
    }

    @Test
    fun `device with exactly minimum RAM and storage returns MINIMUM`() {
        val level = determineCapabilityLevel(
            totalRamMb = DeviceCapabilityChecker.MINIMUM_RAM_MB,
            availableStorageMb = DeviceCapabilityChecker.MINIMUM_STORAGE_MB,
            supportedAbis = listOf("arm64-v8a")
        )
        assertThat(level).isEqualTo(CapabilityLevel.MINIMUM)
    }
}
