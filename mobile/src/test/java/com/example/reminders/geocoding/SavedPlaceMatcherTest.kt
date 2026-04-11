package com.example.reminders.geocoding

import com.example.reminders.data.model.SavedPlace
import com.example.reminders.data.repository.SavedPlaceRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests for [SavedPlaceMatcher].
 *
 * Verifies case-insensitive, whitespace-trimmed matching against the
 * saved-places repository, as well as the no-match and null scenarios.
 */
class SavedPlaceMatcherTest {

    private val mockRepository = mockk<SavedPlaceRepository>()
    private lateinit var matcher: SavedPlaceMatcher

    private val testPlace = SavedPlace(
        id = "place-1",
        label = "Home",
        address = "123 Main St",
        latitude = 51.5074,
        longitude = -0.1278
    )

    @Before
    fun setUp() {
        matcher = SavedPlaceMatcher(mockRepository)
    }

    @Test
    fun `exact label match returns saved place`() = runTest {
        coEvery { mockRepository.findByLabel("Home") } returns testPlace

        val result = matcher.match("Home")

        assertThat(result).isEqualTo(testPlace)
    }

    @Test
    fun `case-insensitive match returns saved place`() = runTest {
        // The DAO performs LOWER(label) = LOWER(:label), so the matcher
        // just passes the trimmed label through. The repository/DAO handle
        // case-insensitive comparison.
        coEvery { mockRepository.findByLabel("home") } returns testPlace

        val result = matcher.match("home")

        assertThat(result).isEqualTo(testPlace)
    }

    @Test
    fun `uppercase label match returns saved place`() = runTest {
        coEvery { mockRepository.findByLabel("HOME") } returns testPlace

        val result = matcher.match("HOME")

        assertThat(result).isEqualTo(testPlace)
    }

    @Test
    fun `whitespace-trimmed label match returns saved place`() = runTest {
        coEvery { mockRepository.findByLabel("Home") } returns testPlace

        val result = matcher.match("  Home  ")

        assertThat(result).isEqualTo(testPlace)
    }

    @Test
    fun `mixed case and whitespace returns saved place`() = runTest {
        coEvery { mockRepository.findByLabel("home") } returns testPlace

        val result = matcher.match("  home  ")

        assertThat(result).isEqualTo(testPlace)
    }

    @Test
    fun `no match returns null`() = runTest {
        coEvery { mockRepository.findByLabel("Work") } returns null

        val result = matcher.match("Work")

        assertThat(result).isNull()
    }

    @Test
    fun `empty label returns null`() = runTest {
        coEvery { mockRepository.findByLabel("") } returns null

        val result = matcher.match("")

        assertThat(result).isNull()
    }

    @Test
    fun `whitespace-only label is trimmed and returns null`() = runTest {
        coEvery { mockRepository.findByLabel("") } returns null

        val result = matcher.match("   ")

        assertThat(result).isNull()
    }
}
