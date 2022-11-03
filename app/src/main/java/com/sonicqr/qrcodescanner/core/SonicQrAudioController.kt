package com.sonicqr.qrcodescanner.core

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import java.time.LocalDateTime

class SonicQrAudioController(configuration: SonicQrAudioConfiguration) {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 75)
    private val configuration: SonicQrAudioConfiguration
    private var lastAudioPlayedAt: LocalDateTime = LocalDateTime.now()

    init {
        this.configuration = configuration
    }

    fun handle(event: SonicQrEvent) {
        playAudioNow(event)
    }

    private fun playAudioNow(event: SonicQrEvent) {
        if(!canPlayAudioNow(event)) {
            Log.d("SonicQrAudioController", "Cannot play event due to Cool Down")
            return
        }
        this.toneGenerator.startTone(event.audioTone, event.durationInMs)
        this.lastAudioPlayedAt = LocalDateTime.now()
    }

    private fun canPlayAudioNow(event: SonicQrEvent): Boolean {
        Log.d("SonicQrAudioController", "ackAudioCoolDownInMs : " + configuration.ackAudioCoolDownInMs.toString())

        // Check event has audio cool down
        if (!event.hasCoolDown) return true

        // Check audio cool down is over
        return LocalDateTime.now().isAfter(this.lastAudioPlayedAt
            .plusNanos(configuration.ackAudioCoolDownInMs * 1000000L))
    }
}

class SonicQrAudioConfiguration {
    var ackAudioCoolDownInMs = 100
}

enum class SonicQrEvent(var audioTone: Int, var durationInMs: Int, var hasCoolDown: Boolean) {
    ACK_FIRST(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 20, true),
    ACK_REPEATED(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, 20, true),
    BACKTRACK_FIRST(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 20, true),
    BACKTRACK_REPEATED(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 20, true),
    COMPLETED(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 20, false),
}