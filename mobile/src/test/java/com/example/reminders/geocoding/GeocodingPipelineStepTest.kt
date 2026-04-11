package com.example.reminders.geocoding

import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.ParsedReminder
import com.example.reminders.data.model.SavedPlace
import com.example.reminders.data.repository.SavedPlaceRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Tests for [GeocodingPipelineStep].
 *
 * Covers the full set of state transitions:
 * - Reminder with no location trigger → immediate Resolved
 * - Reminder with coordinates already present → immediate Resolved
 * - Saved place match → Matched then Resolved
 * - Single geocoding result → Resolved
 * - Multiple results → AwaitingConfirmation → confirmCandidate → Resolved
 * - No results → Failed
 * - Geocoder error → Failed
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GeocodingPipelineStepTest {

    private val mockSavedPlaceMatcher = mockk<SavedPlaceMatcher>()
    private val mockGeocodingService = mockk<GeocodingService>()
    private lateinit var pipelineStep: GeocodingPipelineStep

    @Before
    fun setUp() {
        pipelineStep = GeocodingPipelineStep(
            savedPlaceMatcher = mockSavedPlaceMatcher,
            geocodingService = mockGeocodingService
        )
    }

    @Test
    fun `reminder with no location trigger resolves immediately`() = runTest {
        val reminder = ParsedReminder(title = "Buy milk")

        pipelineStep.process(reminder)

        assertThat(pipelineStep.state.value).isInstanceOf(GeocodingStepState.Resolved::class.java)
        val resolved = pipelineStep.state.value as GeocodingStepState.Resolved
        assertThat(resolved.reminder).isEqualTo(reminder)
    }

    @Test
    fun `reminder with coordinates already present resolves immediately`() = runTest {
        val reminder = ParsedReminder(
            title = "Buy milk",
            locationTrigger = LocationTrigger(
                placeLabel = "Store",
                latitude = 51.5,
                longitude = -0.1
            )
        )

        pipelineStep.process(reminder)

        assertThat(pipelineStep.state.value).isInstanceOf(GeocodingStepState.Resolved::class.java)
    }

    @Test
    fun `saved place match transitions to Matched then Resolved`() = runTest {
        val savedPlace = SavedPlace(
            id = "sp-1",
            label = "Home",
            address = "123 Main St",
            latitude = 51.5074,
            longitude = -0.1278
        )
        coEvery { mockSavedPlaceMatcher.match("Home") } returns savedPlace

        val reminder = ParsedReminder(
            title = "Pick up keys",
            locationTrigger = LocationTrigger(placeLabel = "Home")
        )

        pipelineStep.process(reminder)

        // Final state should be Resolved with coordinates from saved place
        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.Resolved::class.java)
        val resolved = state as GeocodingStepState.Resolved
        assertThat(resolved.reminder.locationTrigger?.latitude).isEqualTo(51.5074)
        assertThat(resolved.reminder.locationTrigger?.longitude).isEqualTo(-0.1278)
        assertThat(resolved.reminder.locationTrigger?.rawAddress).isEqualTo("123 Main St")
    }

    @Test
    fun `single geocoding result resolves directly`() = runTest {
        coEvery { mockSavedPlaceMatcher.match("Office") } returns null
        coEvery { mockGeocodingService.geocode("Office") } returns GeocodingResult.Resolved(
            latitude = 40.7128,
            longitude = -74.0060,
            displayAddress = "456 Business Ave"
        )

        val reminder = ParsedReminder(
            title = "Meeting",
            locationTrigger = LocationTrigger(placeLabel = "Office")
        )

        pipelineStep.process(reminder)

        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.Resolved::class.java)
        val resolved = state as GeocodingStepState.Resolved
        assertThat(resolved.reminder.locationTrigger?.latitude).isEqualTo(40.7128)
        assertThat(resolved.reminder.locationTrigger?.longitude).isEqualTo(-74.0060)
        assertThat(resolved.reminder.locationTrigger?.rawAddress).isEqualTo("456 Business Ave")
    }

    @Test
    fun `ambiguous result transitions to AwaitingConfirmation`() = runTest {
        coEvery { mockSavedPlaceMatcher.match("Park") } returns null
        val candidates = listOf(
            GeocodingCandidate(51.5, -0.1, "Central Park, London"),
            GeocodingCandidate(40.7, -74.0, "Central Park, New York")
        )
        coEvery { mockGeocodingService.geocode("Park") } returns GeocodingResult.Ambiguous(candidates)

        val reminder = ParsedReminder(
            title = "Walk the dog",
            locationTrigger = LocationTrigger(placeLabel = "Park")
        )

        pipelineStep.process(reminder)

        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.AwaitingConfirmation::class.java)
        val awaiting = state as GeocodingStepState.AwaitingConfirmation
        assertThat(awaiting.candidates).hasSize(2)
        assertThat(awaiting.reminder.title).isEqualTo("Walk the dog")
    }

    @Test
    fun `confirmCandidate resolves with selected coordinates`() = runTest {
        coEvery { mockSavedPlaceMatcher.match("Park") } returns null
        val candidates = listOf(
            GeocodingCandidate(51.5, -0.1, "Central Park, London"),
            GeocodingCandidate(40.7, -74.0, "Central Park, New York")
        )
        coEvery { mockGeocodingService.geocode("Park") } returns GeocodingResult.Ambiguous(candidates)

        val reminder = ParsedReminder(
            title = "Walk the dog",
            locationTrigger = LocationTrigger(placeLabel = "Park")
        )

        pipelineStep.process(reminder)

        val selected = candidates[1]
        pipelineStep.confirmCandidate(selected)

        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.Resolved::class.java)
        val resolved = state as GeocodingStepState.Resolved
        assertThat(resolved.reminder.locationTrigger?.latitude).isEqualTo(40.7)
        assertThat(resolved.reminder.locationTrigger?.longitude).isEqualTo(-74.0)
        assertThat(resolved.reminder.locationTrigger?.rawAddress).isEqualTo("Central Park, New York")
    }

    @Test
    fun `not found result transitions to Failed`() = runTest {
        coEvery { mockSavedPlaceMatcher.match("Nowhere") } returns null
        coEvery { mockGeocodingService.geocode("Nowhere") } returns GeocodingResult.NotFound

        val reminder = ParsedReminder(
            title = "Go somewhere",
            locationTrigger = LocationTrigger(placeLabel = "Nowhere")
        )

        pipelineStep.process(reminder)

        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.Failed::class.java)
        val failed = state as GeocodingStepState.Failed
        assertThat(failed.message).contains("Nowhere")
    }

    @Test
    fun `geocoder error transitions to Failed`() = runTest {
        coEvery { mockSavedPlaceMatcher.match("Errorville") } returns null
        coEvery { mockGeocodingService.geocode("Errorville") } returns GeocodingResult.Error(
            "Service not available"
        )

        val reminder = ParsedReminder(
            title = "Test",
            locationTrigger = LocationTrigger(placeLabel = "Errorville")
        )

        pipelineStep.process(reminder)

        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.Failed::class.java)
        val failed = state as GeocodingStepState.Failed
        assertThat(failed.message).isEqualTo("Service not available")
    }

    @Test
    fun `empty place label transitions to Failed`() = runTest {
        val reminder = ParsedReminder(
            title = "Test",
            locationTrigger = LocationTrigger(placeLabel = "")
        )

        pipelineStep.process(reminder)

        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.Failed::class.java)
    }

    @Test
    fun `reset returns to Idle`() = runTest {
        val reminder = ParsedReminder(title = "Test")
        pipelineStep.process(reminder)

        pipelineStep.reset()

        assertThat(pipelineStep.state.value).isEqualTo(GeocodingStepState.Idle)
    }

    @Test
    fun `saved place match preserves other reminder fields`() = runTest {
        val savedPlace = SavedPlace(
            id = "sp-1",
            label = "Gym",
            address = "789 Fitness Blvd",
            latitude = 34.0522,
            longitude = -118.2437
        )
        coEvery { mockSavedPlaceMatcher.match("Gym") } returns savedPlace

        val triggerTime = Instant.parse("2025-06-01T09:00:00Z")
        val reminder = ParsedReminder(
            title = "Workout",
            body = "Leg day",
            triggerTime = triggerTime,
            recurrence = "weekly",
            locationTrigger = LocationTrigger(placeLabel = "Gym")
        )

        pipelineStep.process(reminder)

        val state = pipelineStep.state.value
        assertThat(state).isInstanceOf(GeocodingStepState.Resolved::class.java)
        val resolved = state as GeocodingStepState.Resolved
        assertThat(resolved.reminder.title).isEqualTo("Workout")
        assertThat(resolved.reminder.body).isEqualTo("Leg day")
        assertThat(resolved.reminder.triggerTime).isEqualTo(triggerTime)
        assertThat(resolved.reminder.recurrence).isEqualTo("weekly")
    }

    @Test
    fun `confirmCandidate throws when not in AwaitingConfirmation state`() {
        val candidate = GeocodingCandidate(0.0, 0.0, "Test")

        try {
            pipelineStep.confirmCandidate(candidate)
            assertThat(false).isTrue()
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("AwaitingConfirmation")
        }
    }
}
