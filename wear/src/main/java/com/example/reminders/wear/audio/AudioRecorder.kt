package com.example.reminders.wear.audio

import android.media.AudioFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Wraps Android's [android.media.AudioRecord] to capture 16 kHz mono
 * 16-bit PCM audio.
 *
 * The recorded audio is emitted as [ByteArray] chunks via a [Flow],
 * suitable for streaming to the companion phone via the Wearable
 * Data Layer Channel API.
 *
 * This is a V2 stub — actual recording requires a physical device or
 * emulator with microphone support.
 */
class AudioRecorder {

    /**
     * Starts recording and emits PCM audio chunks.
     *
     * Each chunk is approximately [CHUNK_SIZE_BYTES] bytes of 16 kHz
     * mono 16-bit PCM data.
     *
     * @throws UnsupportedOperationException Always, until real recording
     *   is implemented on a device/emulator.
     */
    fun start(): Flow<ByteArray> = flow {
        throw UnsupportedOperationException(
            "Audio recording not yet implemented — requires device/emulator"
        )
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_SIZE_BYTES = 32768
    }
}
