package com.example.reminders.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.reminders.data.model.LocationReminderState
import com.example.reminders.data.model.LocationTrigger
import com.example.reminders.data.model.Reminder
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var db: RemindersDatabase
    private lateinit var dao: ReminderDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.reminderDao()
    }

    @After
    fun closeDb() = db.close()

    private fun testReminder(
        id: String = "test-1",
        title: String = "Buy milk",
        locationTrigger: LocationTrigger? = null,
        locationState: LocationReminderState? = null,
        isCompleted: Boolean = false
    ) = Reminder(
        id = id,
        title = title,
        sourceTranscript = "remind me to $title",
        locationTrigger = locationTrigger,
        locationState = locationState,
        isCompleted = isCompleted
    )

    @Test
    fun insert and query by id() = runTest {
        val reminder = testReminder()
        dao.insert(reminder)
        assertThat(dao.getById("test-1")).isEqualTo(reminder)
    }

    @Test
    fun insert replaces on conflict() = runTest {
        dao.insert(testReminder(title = "Buy milk"))
        dao.insert(testReminder(title = "Buy eggs"))
        assertThat(dao.getById("test-1")!!.title).isEqualTo("Buy eggs")
    }

    @Test
    fun deleteById removes reminder() = runTest {
        dao.insert(testReminder())
        dao.deleteById("test-1")
        assertThat(dao.getById("test-1")).isNull()
    }

    @Test
    fun update changes reminder() = runTest {
        dao.insert(testReminder())
        dao.update(testReminder(title = "Buy eggs"))
        assertThat(dao.getById("test-1")!!.title).isEqualTo("Buy eggs")
    }

    @Test
    fun getActive returns only incomplete() = runTest {
        dao.insert(testReminder(id = "1", isCompleted = false))
        dao.insert(testReminder(id = "2", isCompleted = true))
        val active = dao.getActive()
        assertThat(active).hasSize(1)
    }

    @Test
    fun getCompleted returns only completed() = runTest {
        dao.insert(testReminder(id = "1", isCompleted = false))
        dao.insert(testReminder(id = "2", isCompleted = true))
        val completed = dao.getCompleted()
        assertThat(completed).hasSize(1)
    }

    @Test
    fun getActiveGeofenceCount counts active and triggered() = runTest {
        dao.insert(testReminder(id = "1", locationState = LocationReminderState.ACTIVE,
            locationTrigger = LocationTrigger(placeLabel = "Store")))
        dao.insert(testReminder(id = "2", locationState = LocationReminderState.TRIGGERED,
            locationTrigger = LocationTrigger(placeLabel = "Gym")))
        dao.insert(testReminder(id = "3", locationState = LocationReminderState.PENDING_GEOCODING))
        dao.insert(testReminder(id = "4", locationState = null))
        assertThat(dao.getActiveGeofenceCount()).isEqualTo(2)
    }

    @Test
    fun locationTrigger round trips through database() = runTest {
        val trigger = LocationTrigger(
            placeLabel = "Home",
            rawAddress = "123 Main St",
            latitude = 40.7128,
            longitude = -74.006,
            radiusMetres = 200,
            triggerOnEnter = true,
            triggerOnExit = false,
            geofenceId = "geo-123"
        )
        dao.insert(testReminder(locationTrigger = trigger, locationState = LocationReminderState.ACTIVE))
        val restored = dao.getById("test-1")!!
        assertThat(restored.locationTrigger).isEqualTo(trigger)
    }
}
