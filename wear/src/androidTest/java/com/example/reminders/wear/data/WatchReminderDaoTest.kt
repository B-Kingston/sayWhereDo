package com.example.reminders.wear.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WatchReminderDaoTest {

    private lateinit var db: WatchRemindersDatabase
    private lateinit var dao: WatchReminderDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, WatchRemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.watchReminderDao()
    }

    @After
    fun closeDb() = db.close()

    private fun testReminder(
        id: String = "test-1",
        title: String = "Buy milk",
        isCompleted: Boolean = false,
        triggerTime: Instant? = null
    ) = WatchReminder(
        id = id,
        title = title,
        sourceTranscript = "remind me to $title",
        isCompleted = isCompleted,
        triggerTime = triggerTime
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
    fun getUpcoming returns reminders in time range() = runTest {
        val from = Instant.parse("2025-01-15T00:00:00Z").toEpochMilli()
        val to = Instant.parse("2025-01-15T23:59:59Z").toEpochMilli()
        dao.insert(testReminder(id = "1", triggerTime = Instant.parse("2025-01-15T10:00:00Z")))
        dao.insert(testReminder(id = "2", triggerTime = Instant.parse("2025-01-16T10:00:00Z")))
        dao.insert(testReminder(id = "3", triggerTime = null))
        val upcoming = dao.getUpcoming(from, to)
        assertThat(upcoming).hasSize(1)
        assertThat(upcoming.first().id).isEqualTo("1")
    }
}
