package com.sonicqr.qrcodescanner.storage

import android.provider.BaseColumns

object PayloadReaderContract {
    // Table contents are grouped together in an anonymous object.
    object PayloadEntry : BaseColumns {
        const val TABLE_NAME = "message"
        const val COLUMN_NAME_SEQ = "seq"
        const val COLUMN_NAME_MESSAGE = "message"
    }
}