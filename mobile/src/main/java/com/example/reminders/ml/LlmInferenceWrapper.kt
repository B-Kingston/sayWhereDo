package com.example.reminders.ml

import java.io.File

/**
 * Thin wrapper around MediaPipe's LLM Inference API.
 *
 * Provides a testable interface for on-device LLM inference. The real
 * implementation delegates to
 * [com.google.mediapipe.tasks.genai.jvm.LlmInference]. Tests use a
 * mock implementation.
 */
interface LlmInferenceWrapper {

    /**
     * Generates a text response for the given [prompt].
     *
     * @param prompt The full prompt string including system instruction
     *   and user message.
     * @return The generated text response.
     */
    fun generateResponse(prompt: String): String

    /** Releases the inference session and frees native resources. */
    fun close()
}
