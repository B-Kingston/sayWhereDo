package com.example.reminders.data.repository

import com.example.reminders.data.local.SavedPlaceDao
import com.example.reminders.data.model.SavedPlace
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SavedPlaceRepositoryImplTest {

    private val dao = mockk<SavedPlaceDao>(relaxed = true)
    private val repo = SavedPlaceRepositoryImpl(dao)

    private val testPlace = SavedPlace(
        id = "place-1",
        label = "Home",
        address = "123 Main St",
        latitude = 40.7128,
        longitude = -74.006
    )

    @Test
    fun `insert delegates to dao`() = runTest {
        repo.insert(testPlace)
        coVerify { dao.insert(testPlace) }
    }

    @Test
    fun `getById delegates to dao`() = runTest {
        coEvery { dao.getById("place-1") } returns testPlace
        assertThat(repo.getById("place-1")).isEqualTo(testPlace)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById("missing") } returns null
        assertThat(repo.getById("missing")).isNull()
    }

    @Test
    fun `findByLabel delegates to dao`() = runTest {
        coEvery { dao.findByLabel("Home") } returns testPlace
        assertThat(repo.findByLabel("Home")).isEqualTo(testPlace)
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repo.delete(testPlace)
        coVerify { dao.delete(testPlace) }
    }

    @Test
    fun `deleteById delegates to dao`() = runTest {
        repo.deleteById("place-1")
        coVerify { dao.deleteById("place-1") }
    }

    @Test
    fun `count delegates to dao`() = runTest {
        coEvery { dao.count() } returns 5
        assertThat(repo.count()).isEqualTo(5)
    }
}
