package com.sonicqr.qrcodescanner.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sonicqr.qrcodescanner.BuildConfig
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class PayloadDecoder {

    companion object {
        fun decodeDataPacketsIntoFile(
            context: Context,
            filename: String,
            payloads: Array<String>): File {
            val encodedUrl = payloads.joinToString("")

            // data:image/png:base64,XXXX
//            val decodedUrlBytes = Base64.getUrlDecoder()
//                .decode(encodedUrl.split(',')[1])
            val decodedUrlBytes = Base64.getDecoder()
                .decode(encodedUrl
                    .substring(encodedUrl.indexOf(',') + 1)
                    .toByteArray(StandardCharsets.UTF_8))

            val path = context.getExternalFilesDir(null)
            val myDir = File(path, "sonicqr")
            myDir.mkdir()
            val file = File(myDir,filename)
            file.writeBytes(decodedUrlBytes)
            return file
        }

        fun openFile(context: Context, file: File) {
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
                ) else Toast.makeText(context, "File is corrupted", Toast.LENGTH_LONG).show()
            } catch (ex: Exception) {
                Toast.makeText(
                    context,
                    "No Application is found to open this file.".trimIndent(),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}