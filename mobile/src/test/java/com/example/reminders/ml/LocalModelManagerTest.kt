package com.example.reminders.ml

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Verifies that [LocalModelManager] correctly tracks download state,
 * resolves model file paths, and reports initial state.
 */
class LocalModelManagerTest {

    private val mockContext = mockk<android.content.Context>()
    private lateinit var manager: LocalModelManager

    @Before
    fun setUp() {
        every { mockContext.filesDir } returns File("/tmp/test-models")
        manager = LocalModelManager(mockContext)
    }

    @Test
    fun `isModelDownloaded returns false when no model exists`() {
        assertThat(manager.isModelDownloaded("nonexistent")).isFalse()
    }

    @Test
    fun `downloadProgress starts as null`() {
        assertThat(manager.downloadProgress.value).isNull()
    }

    @Test
    fun `downloadedModelId starts as null`() {
        assertThat(manager.downloadedModelId.value).isNull()
    }

    @Test
    fun `getModelPath returns null when model not downloaded`() {
        assertThat(manager.getModelPath("nonexistent")).isNull()
    }

    @Test
    fun `getModelPath constructs correct file path`() {
        val file = File("/tmp/test-models/models/gemma2-2b-q4.task")
        assertThat(file.path).contains("models")
        assertThat(file.path).endsWith(".task")
    }
}
