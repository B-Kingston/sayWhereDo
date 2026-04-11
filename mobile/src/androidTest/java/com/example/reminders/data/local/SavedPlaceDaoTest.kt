package com.example.reminders.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.reminders.data.model.SavedPlace
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedPlaceDaoTest {

    private lateinit var db: RemindersDatabase
    private lateinit var dao: SavedPlaceDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.savedPlaceDao()
    }

    @After
    fun closeDb() = db.close()

    private fun testPlace(
        id: String = "place-1",
        label: String = "Home",
        address: String = "123 Main St"
    ) = SavedPlace(
        id = id,
        label = label,
        address = address,
        latitude = 40.7128,
        longitude = -74.006
    )

    @Test
    fun insert and query by id() = runTest {
        dao.insert(testPlace())
        assertThat(dao.getById("place-1")).isEqualTo(testPlace())
    }

    @Test
    fun insert replaces on conflict() = runTest {
        dao.insert(testPlace(label = "Home"))
        dao.insert(testPlace(label = "Cabin"))
        assertThat(dao.getById("place-1")!!.label).isEqualTo("Cabin")
    }

    @Test
    fun deleteById removes place() = runTest {
        dao.insert(testPlace())
        dao.deleteById("place-1")
        assertThat(dao.getById("place-1")).isNull()
    }

    @Test
    fun findByLabel case insensitive() = runTest {
        dao.insert(testPlace(label = "Home"))
        assertThat(dao.findByLabel("home")).isNotNull()
        assertThat(dao.findByLabel("HOME")).isNotNull()
        assertThat(dao.findByLabel("Home")).isNotNull()
    }

    @Test
    fun findByLabel returns null when not found() = runTest {
        assertThat(dao.findByLabel("Work")).isNull()
    }

    @Test
    fun count returns correct count() = runTest {
        dao.insert(testPlace(id = "1"))
        dao.insert(testPlace(id = "2"))
        dao.insert(testPlace(id = "3"))
        assertThat(dao.count()).isEqualTo(3)
    }
}
