package com.sonicqr.qrcodescanner.ui.home

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.multidex.BuildConfig
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.sonicqr.qrcodescanner.R
import com.sonicqr.qrcodescanner.databinding.FragmentHomeBinding
import com.sonicqr.qrcodescanner.storage.PayloadReaderContract
import com.sonicqr.qrcodescanner.storage.PayloadReaderDbHelper
import com.sonicqr.qrcodescanner.util.PayloadDecoder
import java.io.File
import java.time.LocalDateTime
import java.util.*


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var dbHelper: PayloadReaderDbHelper

    private var insertCount: Int = 0
    private var fileName: String = "default.png"
    private var valuesList: Array<String> = arrayOf<String>()

    private var mapValues: SparseArray<String> = SparseArray<String>()

    private var barcodeView: CompoundBarcodeView? = null

    private val toneGen1 = ToneGenerator(AudioManager.STREAM_ALARM, 75)
    private lateinit var lastBeepDateTime: LocalDateTime;
    private val MAX_BEEP_INTERVAL_MS = 70; //70
    private val MAX_BEEP_REPEAT_INTERVAL_MS = 70; //200
    private val BEEP_DURATION_MS = 20; // to delay QR

    private val callback: BarcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (result.text != null) {
                handleDecode(result)
            }
        }
        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        // Initialize dbHelper
        this.dbHelper = PayloadReaderDbHelper(root.context)

        // BarcodeView
        this.barcodeView = root.findViewById(R.id.barcode_scanner);
        (this.barcodeView as CompoundBarcodeView).decodeContinuous(callback);

        this.lastBeepDateTime = LocalDateTime.now();

        return root
    }

    override fun onResume() {
        barcodeView!!.resume()
        super.onResume()
    }

    override fun onPause() {
        barcodeView!!.pause()
        super.onPause()
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    fun handleDecode(rawResult: BarcodeResult) {
        val result = rawResult.text

        // Check header
        val headerRegex = """@(\d+)\|(\S+)""".toRegex();
        val matchResult = headerRegex.find(result)
        if (matchResult != null && matchResult.groups.count() > 0) {
            val (lineCount, headerFileName) = matchResult.destructured
            binding.txtScanned.text = "Going for $lineCount lines for file $headerFileName"

            // Clear records
            this.fileName = headerFileName
            this.valuesList = Array(lineCount.toInt()) {""} // Init empty array
            this.insertCount = 0
            return
        }

        if (this.valuesList.isEmpty()) {
            return
        };

        val columns = result.split("|")

        if (columns.count() >= 2) {
            // Check for line number
            val currentLineNumber:Int? = columns[0].toIntOrNull()
            if (currentLineNumber != null) {
                // process
                val curData = result.subSequence(columns[0].count() + 1, result.length).toString();

                //mapValues.put(curLine.toInt(), curData);

                val now = LocalDateTime.now();
//                var beepTone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                var beepTone = ToneGenerator.TONE_CDMA_ONE_MIN_BEEP;
                if (this.checkPreviousFrameIsEmpty(currentLineNumber)) {
//                    beepTone = ToneGenerator.TONE_CDMA_HIGH_L;
                    beepTone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                }

                val firstDetectionOfFrame = valuesList[currentLineNumber] == "";
                if (firstDetectionOfFrame) {
                    savePayloadToDatabase(currentLineNumber, curData)
                    insertCount ++;

                    if (now.isAfter(
                            this.lastBeepDateTime.plusNanos(MAX_BEEP_INTERVAL_MS * 1000000L))) {
                        toneGen1.startTone(beepTone, BEEP_DURATION_MS)
                        this.lastBeepDateTime = LocalDateTime.now()
                    }

                }
                else if (now.isAfter(
                        this.lastBeepDateTime.plusNanos(MAX_BEEP_REPEAT_INTERVAL_MS * 1000000L))
                ) {
                    toneGen1.startTone(beepTone, BEEP_DURATION_MS)
                    this.lastBeepDateTime = LocalDateTime.now();
                }

                valuesList[currentLineNumber] = curData

                if (this.checkAllDataReceived()) {
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 100)
                    val file = PayloadDecoder.decodeDataPacketsIntoFile(
                        requireContext(), this.fileName, valuesList)
                    PayloadDecoder.openFile(requireContext(), file)
                }
            }
        }
        /*
        var mapValuesText = "";
        mapValues.forEach { key, value -> mapValuesText += "$key - $value\n" };
         */

        //txtScanned.text = "Total records: " + mapValues.size().toString();
        binding.txtScanned.text = "Total records: $insertCount";
    }

    private fun checkAllDataReceived(): Boolean {
        for (i in valuesList.indices) {
            if ( valuesList[i] == "" ) {
                return false;
            }
        }
        return true;
    }

    private fun checkPreviousFrameIsEmpty(seq: Int): Boolean {
        for (i in 0 until seq) {
            if ( valuesList[i] == "" ) {
                return true;
            }
        }
        return false;
    }

    private fun savePayloadToDatabase(seq: Int, message: String) {
        val db = dbHelper.writableDatabase // Gets the data repository in write mode

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(PayloadReaderContract.PayloadEntry.COLUMN_NAME_SEQ, seq)
            put(PayloadReaderContract.PayloadEntry.COLUMN_NAME_MESSAGE, message)
        }

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db?.insert(PayloadReaderContract.PayloadEntry.TABLE_NAME, null, values)

    }

}