package com.example.reminders.data.preferences

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageTrackerTest {

    private lateinit var tracker: UsageTracker

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tracker = UsageTracker(context)
    }

    @Test
    fun initial formattingCount is zero() = runTest {
        tracker.reset()
        tracker.formattingCount.collect { count ->
            assertThat(count).isEqualTo(0)
            return@collect
        }
    }

    @Test
    fun incrementFormattingCount increases count() = runTest {
        tracker.reset()
        tracker.incrementFormattingCount()
        tracker.formattingCount.collect { count ->
            assertThat(count).isEqualTo(1)
            return@collect
        }
    }

    @Test
    fun isFormattingAllowed returns true for Pro users() = runTest {
        tracker.reset()
        assertThat(tracker.isFormattingAllowed(isPro = true, hasApiKey = false)).isTrue()
    }

    @Test
    fun isFormattingAllowed returns true when BYO API key set() = runTest {
        tracker.reset()
        assertThat(tracker.isFormattingAllowed(isPro = false, hasApiKey = true)).isTrue()
    }

    @Test
    fun isFormattingAllowed blocks free user after one use() = runTest {
        tracker.reset()
        tracker.incrementFormattingCount()
        assertThat(tracker.isFormattingAllowed(isPro = false, hasApiKey = false)).isFalse()
    }

    @Test
    fun isFormattingAllowed allows free user with zero count() = runTest {
        tracker.reset()
        assertThat(tracker.isFormattingAllowed(isPro = false, hasApiKey = false)).isTrue()
    }
}
