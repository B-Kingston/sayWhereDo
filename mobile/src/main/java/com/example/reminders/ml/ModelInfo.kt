package com.example.reminders.ml

/**
 * Metadata for a downloadable on-device LLM model.
 *
 * Each model has a unique ID, download URL, file size, and minimum
 * RAM requirement. The [isRecommended] flag indicates whether the
 * model is the default recommendation for capable devices.
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
    val fileSizeDisplay: String,
    val minRamMb: Long,
    val isRecommended: Boolean
)

/**
 * Catalog of available on-device LLM models.
 *
 * Models must be in MediaPipe `.task` format (not GGUF). Google
 * provides pre-converted Gemma models from their storage bucket.
 */
object AvailableModels {

    val GEMMA_2_2B_Q4 = ModelInfo(
        id = "gemma2-2b-q4",
        name = "Gemma 2 2B (Q4)",
        downloadUrl = "https://storage.googleapis.com/mediapipe-models/gemma2/text_classification/gemma2-2b-it-q4/float32/1/gemma2-2b-it-q4.task",
        fileSizeBytes = 1_500_000_000L,
        fileSizeDisplay = "~1.4 GB",
        minRamMb = 6000L,
        isRecommended = true
    )

    /** All available models. */
    val ALL: List<ModelInfo> = listOf(GEMMA_2_2B_Q4)

    /**
     * Returns the model with the given [id], or null if not found.
     */
    fun getById(id: String): ModelInfo? = ALL.find { it.id == id }
}
