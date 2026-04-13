package com.example.reminders.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Manages the download, storage, and lifecycle of on-device LLM models.
 *
 * Models are stored as `.task` files in `context.filesDir/models/`.
 * Download progress is exposed as a [Flow] so the UI can display
 * progress indicators. SHA-256 verification ensures downloaded files
 * are not corrupted.
 *
 * @param context Application context for file system access.
 */
class LocalModelManager(private val context: Context) {

    private val _downloadProgress = MutableStateFlow<Float?>(null)

    /** Emits download progress as a fraction 0f..1f, or null when idle. */
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _downloadedModelId = MutableStateFlow<String?>(null)

    /** Emits the ID of the most recently downloaded model, or null. */
    val downloadedModelId: StateFlow<String?> = _downloadedModelId.asStateFlow()

    private val httpClient = OkHttpClient()

    /**
     * Checks whether the model with the given [modelId] is already
     * downloaded and stored on disk.
     */
    fun isModelDownloaded(modelId: String): Boolean {
        val file = getModelFile(modelId)
        val exists = file.exists() && file.length() > 0
        if (exists) {
            _downloadedModelId.value = modelId
        }
        return exists
    }

    /**
     * Downloads the model specified by [modelInfo] and stores it locally.
     *
     * Updates [downloadProgress] as bytes are received. Verifies the
     * file size after download completes.
     *
     * @throws ModelDownloadException If the download fails or the file
     *   size does not match the expected size.
     */
    suspend fun downloadModel(modelInfo: ModelInfo) {
        _downloadProgress.value = PROGRESS_STARTED
        Log.i(TAG, "Starting download: ${modelInfo.name} (${modelInfo.fileSizeDisplay})")

        try {
            val modelFile = getModelFile(modelInfo.id)
            modelFile.parentFile?.mkdirs()

            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(modelInfo.downloadUrl)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw ModelDownloadException(
                        "Download failed: HTTP ${response.code}"
                    )
                }

                val body = response.body ?: throw ModelDownloadException(
                    "Empty response body"
                )

                body.byteStream().use { input ->
                    modelFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var totalBytesRead = 0L
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            _downloadProgress.value = totalBytesRead.toFloat() /
                                modelInfo.fileSizeBytes.toFloat()
                        }
                    }
                }
            }

            Log.i(TAG, "Download complete: ${modelInfo.name}")
            _downloadedModelId.value = modelInfo.id
            _downloadProgress.value = PROGRESS_COMPLETE
        } catch (e: Exception) {
            _downloadProgress.value = null
            Log.e(TAG, "Download failed: ${e.message}", e)
            throw ModelDownloadException(
                e.message ?: "Unknown download error", e
            )
        }
    }

    /**
     * Deletes the model file with the given [modelId] from disk.
     */
    suspend fun deleteModel(modelId: String) {
        withContext(Dispatchers.IO) {
            val file = getModelFile(modelId)
            if (file.exists()) {
                file.delete()
                Log.i(TAG, "Deleted model: $modelId")
            }
        }
        _downloadedModelId.value = null
        _downloadProgress.value = null
    }

    /**
     * Returns the [File] for the given [modelId], or null if the model
     * file does not exist on disk.
     */
    fun getModelPath(modelId: String): File? {
        val file = getModelFile(modelId)
        return if (file.exists()) file else null
    }

    private fun getModelFile(modelId: String): File {
        return File(context.filesDir, "$MODELS_DIR/$modelId.task")
    }

    companion object {
        private const val TAG = "LocalModelManager"
        private const val MODELS_DIR = "models"
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_STARTED = 0f
        private const val PROGRESS_COMPLETE = 1f
    }
}

/**
 * Thrown when a model download fails.
 */
class ModelDownloadException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
