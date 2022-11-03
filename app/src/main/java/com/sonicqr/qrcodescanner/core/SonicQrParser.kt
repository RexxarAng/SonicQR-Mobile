package com.sonicqr.qrcodescanner.core

import android.util.Log

class SonicQrParser : Parser {

    override fun parseHeaderFrame(dataString: String): SonicQrHeaderFrame? {
        val sonicQrHeaderPacketRegex = """@(\d+)\|(.+)""".toRegex()
        val matchResult = sonicQrHeaderPacketRegex.find(dataString)

        // Check data string is SonicQr header packet pattern else return null
        if (matchResult == null || matchResult.groups.isEmpty()) {
            Log.d("SonicQrParser", "Frame is not header frame")
            return null
        };

        // Retrieve header packet detail
        val sonicQrHeader = SonicQrHeaderFrame()
        val (numOfDataPacket, fileData) = matchResult.destructured
        val fileAttributes = fileData.split('|')
        sonicQrHeader.numberOfDataFrames = numOfDataPacket.toInt()
        sonicQrHeader.fileName = fileAttributes[0]
        sonicQrHeader.fileType = fileAttributes[1]
        sonicQrHeader.sizeInBytes = fileAttributes[2].toInt()
        sonicQrHeader.audioCoolDown =
            if (fileAttributes.size >= 5) fileAttributes[4].toInt()
            else 100
        return sonicQrHeader
    }

    override fun parseDataFrame(dataString: String): SonicQrDataFrame? {
        val columns = dataString.split(":")

        if (columns.count() < 2) {
            Log.d("SonicQrParser", "Frame is not data frame")
            return null
        }

        val sonicQrDataPacket = SonicQrDataFrame()
        sonicQrDataPacket.seqNumber = columns[0].toIntOrNull() ?: return null
        sonicQrDataPacket.dataString =
            dataString.subSequence(columns[0].count() + 1, dataString.length).toString();
        return sonicQrDataPacket
    }

}

interface Parser {
    fun parseHeaderFrame(dataString:String): HeaderFrame?
    fun parseDataFrame(dataString: String): DataFrame?
}