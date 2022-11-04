package com.sonicqr.qrcodescanner.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import nl.minvws.encoding.Base45
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

class SonicQrDecoder : Decoder {
    override fun decode(context: Context, headerFrame: HeaderFrame, dataFrames: Array<DataFrame?>): File? {
        var encodedData = ""
        dataFrames.forEach { dataFrame -> encodedData += dataFrame?.dataString }

        val decodedUrlBytes =
            if ("Base45" == headerFrame.encoding) decodeBase45Data(encodedData)
            else decodeBase64Data(encodedData)

        val path = context.getExternalFilesDir(null)
        val myDir = File(path, "sonicqr")
        myDir.mkdir()

        if (hash(decodedUrlBytes) != headerFrame.checkSum) return null;
        Log.d("SonicQrDecoder", "Hash checksum verified")
        val file = File(myDir, headerFrame.fileName)
        file.writeBytes(decodedUrlBytes)
        return file
    }

    private fun hash(decodedUrlBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(decodedUrlBytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun decodeBase45Data(encodedData: String): ByteArray {
        return Base45.getDecoder().decode(encodedData)
    }

    private fun decodeBase64Data(encodedData: String): ByteArray {
        return Base64.getDecoder()
            .decode(encodedData
                .substring(encodedData.indexOf(',') + 1)
                .toByteArray(StandardCharsets.UTF_8))
    }

    override fun openFile(context: Context, file: File) {
        Log.d("SonicQrDecoder", "Trying to open File")

        val url: Uri =
            FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        if (url.toString().contains(".doc") ||
            url.toString().contains(".docx")) {
            // Word document
            intent.setDataAndType(url, "application/msword")
        } else if (url.toString().contains(".pdf")) {
            // PDF file
            intent.setDataAndType(url, "application/pdf")
        } else if (
            url.toString().contains(".ppt") ||
            url.toString().contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(url, "application/vnd.ms-powerpoint")
        } else if (
            url.toString().contains(".xls") ||
            url.toString().contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(url, "application/vnd.ms-excel")
        } else if (
            url.toString().contains(".zip") ||
            url.toString().contains(".rar")) {
            // WAV audio file
            intent.setDataAndType(url, "application/x-wav")
        } else if (url.toString().contains(".rtf")) {
            // RTF file
            intent.setDataAndType(url, "application/rtf")
        } else if (
            url.toString().contains(".wav") ||
            url.toString().contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(url, "audio/x-wav")
        } else if (url.toString().contains(".gif")) {
            // GIF file
            intent.setDataAndType(url, "image/gif")
        } else if (
            url.toString().contains(".jpg") ||
            url.toString().contains(".jpeg") ||
            url.toString().contains(".png")
        ) {
            // JPG file
            intent.setDataAndType(url, "image/jpeg")
        } else if (url.toString().contains(".txt")) {
            // Text file
            intent.setDataAndType(url, "text/plain")
        } else if (
            url.toString().contains(".3gp") ||
            url.toString().contains(".mpg") ||
            url.toString().contains(".mpeg") ||
            url.toString().contains(".mpe") ||
            url.toString().contains(".mp4") ||
            url.toString().contains(".avi")
        ) {
            // Video files
            intent.setDataAndType(url, "video/*")
        } else {
            //if you want you can also define the intent type for any other file
            //additionally use else clause below, to manage other unknown extensions
            //in this case, Android will show all applications installed on the device
            //so you can choose which application to use
            intent.setDataAndType(url, "*/*")
        }
        try {
            if (file.exists()) context.startActivity(
                Intent.createChooser(
                    intent,
                    "Open"
                )
            )
            else Toast.makeText(context, "File is corrupted", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Toast.makeText(
                context,
                "No Application is found to open this file.".trimIndent(),
                Toast.LENGTH_LONG
            ).show()
        }
    }

}

interface Decoder {
    fun decode(context: Context, headerFrame: HeaderFrame, dataFrames: Array<DataFrame?>): File?
    fun openFile(context: Context, file: File)
}