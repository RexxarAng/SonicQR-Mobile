package com.sonicqr.qrcodescanner.core

import android.content.Context
import android.util.Log
import java.time.Duration
import java.time.LocalDateTime

class SonicQrProcessor(private var context: Context) {

    private val sonicQrParser: Parser = SonicQrParser()
    private val sonicQrAudioConfiguration: SonicQrAudioConfiguration = SonicQrAudioConfiguration()
    private val sonicQrAudioController: SonicQrAudioController = SonicQrAudioController(sonicQrAudioConfiguration)
    private val sonicQrDecoder: Decoder = SonicQrDecoder()

    private var headerFrame: HeaderFrame? = null
    private var dataFrames: Array<DataFrame?> = arrayOf<DataFrame?>()
    private var totalNumberOfDataFrameReceived: Int = 0 // for quick access
    private var lastSequentialFrameSeqNumber: Int = -1
    private var isOpen: Boolean = false

    // Analysis only
    private var receivedFirstFrameAt: LocalDateTime = LocalDateTime.now();

    fun handle(dataString: String) {
        if (processHeaderFrame(dataString)) return
        if (isOpen) return
        if (processDataFrame(dataString)) return
    }

    private fun restartState() {
        totalNumberOfDataFrameReceived = 0
        lastSequentialFrameSeqNumber = -1
        isOpen = false
    }

    private fun processHeaderFrame(dataString: String): Boolean {
        val processedHeaderFrame = sonicQrParser.parseHeaderFrame(dataString) ?: return false
        headerFrame = processedHeaderFrame
        // Get header configuration
        sonicQrAudioConfiguration.ackAudioCoolDownInMs = processedHeaderFrame.audioCoolDown
        // Initialize Data Frame
        dataFrames = Array(processedHeaderFrame.numberOfDataFrames) { null }
        restartState()
        sonicQrAudioController.handle(SonicQrEvent.ACK_FIRST)
        return true
    }
    
    private fun processDataFrame(dataString: String): Boolean {
        // Do not process without header frame
        if (headerFrame == null) return false

        val dataFrame = sonicQrParser.parseDataFrame(dataString) ?: return false

        // Check if sequence number is within acceptable range
        val seqNumber = dataFrame.seqNumber;
        if (seqNumber >= dataFrames.size) return false

        // Check states
        val isNewDataFrame = dataFrames[seqNumber] == null
        val hasAnyPreviousEmptyDataFrame = checkAnyPreviousFrameIsEmpty(seqNumber)
        // Store Data Frame
        if (isNewDataFrame) {
            totalNumberOfDataFrameReceived ++
        }
        dataFrames[seqNumber] = dataFrame
        if (!hasAnyPreviousEmptyDataFrame) lastSequentialFrameSeqNumber = seqNumber
        val hasAllDataFrames = checkAllDataFramesReceived()

        // Determine event type to be trigger
        val event = if (hasAllDataFrames) {
            SonicQrEvent.COMPLETED
        } else if (hasAnyPreviousEmptyDataFrame) {
            if (isNewDataFrame) SonicQrEvent.BACKTRACK_FIRST
            else SonicQrEvent.BACKTRACK_REPEATED
        } else {
            if (isNewDataFrame) SonicQrEvent.ACK_FIRST
            else SonicQrEvent.ACK_REPEATED
        }
        sonicQrAudioController.handle(event)

        // Decode Data when all frame is received
        if (hasAllDataFrames && !isOpen) {
            val file = sonicQrDecoder.decode(context, headerFrame!!, dataFrames)
            if (file != null) {
                sonicQrDecoder.openFile(context, file)
                isOpen = true
            }
            else {
                Log.w("SonicQrProcessor", "File is null, cannot open")
            }
        }

        // Analysis
        if (isNewDataFrame && seqNumber == 0) {
            receivedFirstFrameAt = LocalDateTime.now()
        }
        if (hasAllDataFrames) {
            val timeTakenToTransferFile =
                Duration.between(receivedFirstFrameAt, LocalDateTime.now())
            Log.d("SonicQrProcessor",
                "Time taken : " + timeTakenToTransferFile.toMillis() / 1000.0)
        }

        Log.d("SonicQrProcessor",
            "totalNumberOfDataFrameReceived : $totalNumberOfDataFrameReceived"
        )
        Log.d("SonicQrProcessor",
            "lastSequentialFrameSeqNumber : $lastSequentialFrameSeqNumber"
        )

        return true
    }

    private fun checkAnyPreviousFrameIsEmpty(seqNumber: Int): Boolean {
        return seqNumber > lastSequentialFrameSeqNumber + 1
    }

    private fun checkAllDataFramesReceived(): Boolean {
        if (headerFrame == null) return false
        return totalNumberOfDataFrameReceived >= dataFrames.size
                || lastSequentialFrameSeqNumber == headerFrame!!.numberOfDataFrames-1
    }
}
