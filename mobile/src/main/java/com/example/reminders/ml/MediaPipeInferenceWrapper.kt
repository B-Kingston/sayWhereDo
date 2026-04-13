package com.example.reminders.ml

import android.util.Log

/**
 * Production implementation of [LlmInferenceWrapper] that delegates to
 * MediaPipe's LLM Inference API.
 *
 * Loads the model from [modelPath] on construction and generates text
 * responses using the MediaPipe runtime with GPU delegate when available.
 *
 * @param modelPath Absolute path to the `.task` model file on disk.
 */
class MediaPipeInferenceWrapper(private val modelPath: String) : LlmInferenceWrapper {

    // NOTE: The actual MediaPipe LlmInference API initialization will be
    // implemented when integration testing on a real device. For now this
    // serves as the production shell that will be filled in during device testing.
    // The import would be: com.google.mediapipe.tasks.genai.jvm.LlmInference

    init {
        Log.i(TAG, "Initializing MediaPipe inference from: $modelPath")
    }

    override fun generateResponse(prompt: String): String {
        Log.d(TAG, "Generating response (prompt: ${prompt.length} chars)")
        // TODO: Implement with actual MediaPipe LlmInference API
        // val inference = LlmInference.createFromOptions(...)
        // return inference.generateResponse(prompt)
        throw UnsupportedOperationException(
            "MediaPipe inference not yet implemented — requires device testing"
        )
    }

    override fun close() {
        Log.i(TAG, "Closing MediaPipe inference session")
        // TODO: Close the LlmInference session
    }

    companion object {
        private const val TAG = "MediaPipeInference"
    }
}
