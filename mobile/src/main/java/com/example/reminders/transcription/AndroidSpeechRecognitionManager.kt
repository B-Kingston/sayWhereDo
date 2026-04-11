package com.example.reminders.transcription

import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of [SpeechRecognitionManager] backed by the
 * platform [SpeechRecognizer].
 *
 * **Thread safety:** [SpeechRecognizer] must be created and used on the
 * main thread. All calls are dispatched via [mainHandler] to guarantee this.
 *
 * @param context Activity-level context used to create the recogniser and
 *   request audio focus. Must not be an application context.
 */
class AndroidSpeechRecognitionManager(
    context: Context
) : SpeechRecognitionManager {

    private val appContext = context.applicationContext

    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    override val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                _recognitionState.value = RecognitionState.Error(
                    ERROR_CODE_UNAVAILABLE,
                    "Speech recognition is not available on this device"
                )
                return@post
            }

            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            if (recognizer == null) {
                _recognitionState.value = RecognitionState.Error(
                    ERROR_CODE_UNAVAILABLE,
                    "Speech recognition is not available on this device"
                )
                return@post
            }

            speechRecognizer = recognizer.apply {
                setRecognitionListener(RecognitionListenerImpl())
            }
        }
    }

    override fun startListening() {
        mainHandler.post {
            val recognizer = speechRecognizer ?: run {
                _recognitionState.value = RecognitionState.Error(
                    ERROR_CODE_UNAVAILABLE,
                    "Speech recognition is not available on this device"
                )
                return@post
            }

            requestAudioFocus()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Hint only — OEMs may ignore and fall back to cloud recognition
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            recognizer.startListening(intent)
            _recognitionState.value = RecognitionState.Listening
        }
    }

    override fun stopListening() {
        mainHandler.post {
            speechRecognizer?.stopListening()
        }
    }

    override fun destroy() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
            abandonAudioFocus()
        }
    }

    /**
     * Requests transient audio focus so the recognition engine can capture
     * microphone input without competing with media playback.
     */
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setOnAudioFocusChangeListener { /* no-op for transient focus */ }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { /* no-op */ },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    /** Releases the transient audio focus acquired in [requestAudioFocus]. */
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus { }
        }
    }

    /**
     * Bridges [SpeechRecognizer] callbacks into [RecognitionState] emissions.
     */
    private inner class RecognitionListenerImpl : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            // The engine confirmed it is ready — state is already Listening.
        }

        override fun onBeginningOfSpeech() {
            // User has started speaking — no state change needed.
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level change — no state change needed.
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Raw audio buffer — not used.
        }

        override fun onEndOfSpeech() {
            // User stopped speaking; engine is now finalising results.
            val current = _recognitionState.value
            if (current is RecognitionState.Listening || current is RecognitionState.PartialResult) {
                _recognitionState.value = RecognitionState.Processing
            }
        }

        override fun onError(error: Int) {
            abandonAudioFocus()

            // ERROR_NO_MATCH is non-fatal — the user simply didn't speak.
            _recognitionState.value = RecognitionState.Error(error, errorMessageForCode(error))
            Log.w(TAG, "Recognition error $error: ${errorMessageForCode(error)}")
        }

        override fun onResults(results: Bundle?) {
            abandonAudioFocus()

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty()

            if (text.isNotBlank()) {
                _recognitionState.value = RecognitionState.FinalResult(text)
            } else {
                // Empty result — treat the same as ERROR_NO_MATCH.
                _recognitionState.value = RecognitionState.Error(
                    SpeechRecognizer.ERROR_NO_MATCH,
                    errorMessageForCode(SpeechRecognizer.ERROR_NO_MATCH)
                )
            }
        }

        /**
         * Partial results **replace** the previous text entirely (they are
         * cumulative from the engine's perspective, not incremental).
         */
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                _recognitionState.value = RecognitionState.PartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future use by the framework.
        }
    }

    companion object {

        private const val TAG = "SpeechRecognition"
        private const val ERROR_CODE_UNAVAILABLE = -1

        /**
         * Maps a platform [SpeechRecognizer] error code to a human-readable
         * message suitable for display to the user.
         */
        fun errorMessageForCode(code: Int): String = when (code) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Please try again."
            SpeechRecognizer.ERROR_NETWORK -> "Network error. Check your connection."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_SERVER -> "Speech server error."
            SpeechRecognizer.ERROR_CLIENT -> "Client error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected. Please try again."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech was detected. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recogniser is busy. Please wait."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests. Please wait a moment."
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected. Please try again."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language is not supported."
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language is currently unavailable."
            ERROR_CODE_UNAVAILABLE -> "Speech recognition is not available on this device."
            else -> "Unknown speech recognition error ($code)."
        }
    }
}
