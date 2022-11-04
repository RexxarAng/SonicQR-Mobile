package com.sonicqr.qrcodescanner.core

abstract class HeaderFrame {
    var numberOfDataFrames: Int = 0
    var fileName: String = "default.txt"
    var fileType: String = "text/plain"
    var sizeInBytes: Int = 0
    var encoding: String = "Base45"
    var checkSum: String = "XXX"
    var audioCoolDown: Int = 100
}

abstract class DataFrame {
    var seqNumber: Int = 0
    var dataString: String = ""
}
