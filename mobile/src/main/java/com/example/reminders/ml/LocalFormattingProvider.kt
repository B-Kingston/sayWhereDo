package com.example.reminders.ml

import android.util.Log
import com.example.reminders.formatting.FormattingProvider
import com.example.reminders.formatting.FormattingPrompt
import com.example.reminders.formatting.FormattingResponseParser
import com.example.reminders.formatting.FormattingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * [FormattingProvider] that runs inference using an on-device LLM via
 * MediaPipe's LLM Inference API.
 *
 * Uses a [Mutex] to serialize concurrent calls since the underlying
 * inference session is not thread-safe. The model is loaded lazily on
 * first use and released on [close] or memory pressure.
 *
 * @param modelManager     Manages model download and storage.
 * @param modelInfo        Metadata for the model to use.
 * @param inferenceFactory Creates the inference wrapper from a model file.
 */
class LocalFormattingProvider(
    private val modelManager: LocalModelManager,
    private val modelInfo: ModelInfo,
    private val inferenceFactory: (File) -> LlmInferenceWrapper
) : FormattingProvider {

    @Volatile
    private var session: LlmInferenceWrapper? = null
    private val mutex = Mutex()

    /**
     * Formats the given [transcript] using the on-device LLM.
     *
     * Loads the model if not already loaded, generates a response, and
     * parses it via [FormattingResponseParser]. Concurrent calls are
     * serialized via [Mutex].
     */
    override suspend fun format(transcript: String): FormattingResult =
        mutex.withLock {
            val inference = ensureModelLoaded()
                ?: return FormattingResult.Failure("Model not downloaded")

            val prompt = FormattingPrompt.buildForLocalModel()
            val fullPrompt = buildPrompt(prompt, transcript)

            return@withLock try {
                Log.d(TAG, "Running local inference (transcript: ${transcript.take(60)})")
                val response = withContext(Dispatchers.Default) {
                    inference.generateResponse(fullPrompt)
                }
                Log.d(TAG, "Local inference complete (${response.length} chars)")
                FormattingResponseParser.parse(response, transcript)
            } catch (e: Exception) {
                Log.e(TAG, "Local inference failed: ${e.message}", e)
                FormattingResult.Failure("Local inference failed: ${e.message}")
            }
        }

    private suspend fun ensureModelLoaded(): LlmInferenceWrapper? {
        session?.let { return it }

        val modelFile = modelManager.getModelPath(modelInfo.id)
            ?: return null

        return try {
            val wrapper = inferenceFactory(modelFile)
            session = wrapper
            Log.i(TAG, "Model loaded: ${modelInfo.name}")
            wrapper
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
            null
        }
    }

    private fun buildPrompt(systemPrompt: String, userMessage: String): String {
        return "$systemPrompt\n\nUser: $userMessage\n\nAssistant:"
    }

    /**
     * Releases the inference session and frees native resources.
     *
     * Called on memory pressure via
     * [com.example.reminders.di.RemindersApplication.onTrimMemory].
     */
    fun close() {
        session?.close()
        session = null
        Log.i(TAG, "Local model session closed")
    }

    companion object {
        private const val TAG = "LocalFormatting"
    }
}
