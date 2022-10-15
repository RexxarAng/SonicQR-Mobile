package com.sonicqr.qrcodescanner.ui.home

import android.content.ContentValues
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.sonicqr.qrcodescanner.R
import com.sonicqr.qrcodescanner.storage.PayloadReaderContract
import com.sonicqr.qrcodescanner.storage.PayloadReaderDbHelper
import com.sonicqr.qrcodescanner.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var dbHelper: PayloadReaderDbHelper

    private var insertCount: Int = 0
    private var valuesList: Array<String> = arrayOf<String>()

    private var mapValues: SparseArray<String> = SparseArray<String>()

    private var barcodeView: CompoundBarcodeView? = null

    private val toneGen1 = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    private var waitNextQRCounter = 0;
    private val WAIT_NEXT_QR_COUNTER = 0;
    private val NextQRToneMs = 20

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
            val (lineCount, nameTask) = matchResult.destructured
            binding.txtScanned.text = "Going for $lineCount lines for file $nameTask"

            // Clear records
            valuesList = Array(lineCount.toInt()) {""} // Init empty array
            insertCount = 0
            return
        }

        if (valuesList.isEmpty()) {
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
                if (valuesList[currentLineNumber] == "") {
                    savePayloadToDatabase(currentLineNumber, curData)
                    insertCount ++;

                    toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, NextQRToneMs)
                    this.waitNextQRCounter = 0;
                }
                else if (this.waitNextQRCounter >= this.WAIT_NEXT_QR_COUNTER) {
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, NextQRToneMs)
                    this.waitNextQRCounter = 0;
                }
                else {
                    this.waitNextQRCounter++;
                }

                valuesList[currentLineNumber] = curData
            }
        }
        /*
        var mapValuesText = "";
        mapValues.forEach { key, value -> mapValuesText += "$key - $value\n" };
         */

        //txtScanned.text = "Total records: " + mapValues.size().toString();
        binding.txtScanned.text = "Total records: $insertCount";
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