package com.example.reminders.wear.audio

import android.util.Log
import kotlinx.coroutines.flow.Flow

/**
 * Streams audio data from the watch to the companion phone via the
 * Wearable Data Layer Channel API.
 *
 * Opens a bidirectional channel, sends PCM audio chunks as they are
 * produced by [AudioRecorder], and closes the channel when the flow
 * completes.
 *
 * This is a V2 stub — actual streaming requires a connected phone.
 */
class WearableAudioStreamer {

    /**
     * Streams [audioChunks] to the companion phone.
     *
     * @param audioChunks Flow of PCM audio byte arrays from [AudioRecorder].
     * @return The transcription result text from the phone, or null on failure.
     */
    suspend fun streamAudio(audioChunks: Flow<ByteArray>): String? {
        Log.d(TAG, "streamAudio called — stub, not yet implemented")
        return null
    }

    companion object {
        private const val TAG = "WearableAudioStreamer"
    }
}
