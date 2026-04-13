package com.example.reminders.formatting

/**
 * A pre-configured AI provider preset with all connection details.
 *
 * Each preset encapsulates the base URL, default model, authentication
 * requirements, and privacy metadata for a specific AI service provider.
 * Users can select from these presets in settings or define a custom endpoint.
 */
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val requiresApiKey: Boolean,
    val privacyNote: String,
    val dataRetention: String,
    val hasFreeTier: Boolean
)

/**
 * Catalog of pre-configured AI provider presets.
 *
 * Each constant represents a known AI service that supports the
 * OpenAI-compatible chat completions API (`/v1/chat/completions`).
 */
object AiProviderPresets {

    val GEMINI = AiProvider(
        id = "gemini",
        name = "Gemini",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-2.5-flash-lite",
        requiresApiKey = true,
        privacyNote = "Google's AI service. Data may be used to improve products.",
        dataRetention = "30 days, configurable in Google AI Studio",
        hasFreeTier = true
    )

    val OPENAI = AiProvider(
        id = "openai",
        name = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        requiresApiKey = true,
        privacyNote = "Data may be used for safety and model improvement. Opt out available.",
        dataRetention = "30 days, opt-out available",
        hasFreeTier = false
    )

    val GROQ = AiProvider(
        id = "groq",
        name = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.1-8b-instant",
        requiresApiKey = true,
        privacyNote = "Data not used for training. Fast inference.",
        dataRetention = "Not retained",
        hasFreeTier = true
    )

    val TOGETHER = AiProvider(
        id = "together",
        name = "Together AI",
        baseUrl = "https://api.together.xyz/v1",
        defaultModel = "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo",
        requiresApiKey = true,
        privacyNote = "Open-source model options available.",
        dataRetention = "30 days by default",
        hasFreeTier = true
    )

    val OLLAMA_LOCAL = AiProvider(
        id = "ollama-local",
        name = "Ollama (local)",
        baseUrl = "http://localhost:11434/v1",
        defaultModel = "gemma2:2b",
        requiresApiKey = false,
        privacyNote = "Runs on your own hardware. Full data control.",
        dataRetention = "Your server, your rules",
        hasFreeTier = true
    )

    val CUSTOM = AiProvider(
        id = "custom",
        name = "Custom endpoint",
        baseUrl = "",
        defaultModel = "",
        requiresApiKey = true,
        privacyNote = "Review the privacy policy of your chosen provider.",
        dataRetention = "Varies by provider",
        hasFreeTier = false
    )

    /** All available provider presets. */
    val ALL: List<AiProvider> = listOf(GEMINI, OPENAI, GROQ, TOGETHER, OLLAMA_LOCAL, CUSTOM)

    /**
     * Returns the provider preset with the given [id], or null if not found.
     */
    fun getById(id: String): AiProvider? = ALL.find { it.id == id }
}
