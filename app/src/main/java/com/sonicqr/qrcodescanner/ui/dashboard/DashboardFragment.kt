package com.sonicqr.qrcodescanner.ui.dashboard

import android.os.Bundle
import android.provider.BaseColumns
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.sonicqr.qrcodescanner.R
import com.sonicqr.qrcodescanner.storage.PayloadReaderContract
import com.sonicqr.qrcodescanner.storage.PayloadReaderDbHelper
import java.net.URLEncoder
import java.util.*


class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var dbHelper: PayloadReaderDbHelper
    private lateinit var queue: RequestQueue

    private lateinit var payloadListView: ListView

    private lateinit var txtNumOfPayload: TextView
    private lateinit var txtSendResult: TextView
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private lateinit var urlEditText: EditText

    private var items: SparseArray<String> = SparseArray<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        this.payloadListView = root.findViewById<ListView>(R.id.payloadList)

        // Display
        val lst: ArrayList<String> = ArrayList<String>()
        this.arrayAdapter = ArrayAdapter(root.context,
            android.R.layout.simple_list_item_1, lst)
        this.payloadListView.adapter = arrayAdapter

        // Initialize dbHelper
        this.dbHelper = PayloadReaderDbHelper(root.context)

        // Initialize queue for network
        this.queue = Volley.newRequestQueue(this.payloadListView.context)

        // UI
        this.txtNumOfPayload = root.findViewById<TextView>(R.id.txtNumOfPayload)
        this.txtSendResult = root.findViewById<TextView>(R.id.txtSendResult)
        this.urlEditText = root.findViewById<EditText>(R.id.urlEditText)

        val btnTestNetwork = root.findViewById<Button>(R.id.btnTestNetwork)
        btnTestNetwork.setOnClickListener {
            this.testConnection()
        }

        val btnClearAll = root.findViewById<Button>(R.id.btnClearAll)
        btnClearAll.setOnClickListener {
            this.clearAllData()
            this.loadData()
        }

        val btnSendToComputer = root.findViewById<Button>(R.id.btnSendToComputer)
        btnSendToComputer.setOnClickListener {
            this.loadData()
            this.sendAllData()
        }

        this.loadData()

        return root
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    private fun loadData() {
        val db = dbHelper.readableDatabase

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(
            BaseColumns._ID,
            PayloadReaderContract.PayloadEntry.COLUMN_NAME_SEQ,
            PayloadReaderContract.PayloadEntry.COLUMN_NAME_MESSAGE
        )

        // Filter results WHERE "title" = 'My Title'
        /*
        val selection = "${PayloadReaderContract.PayloadEntry.COLUMN_NAME_SEQ} = ?"
        val selectionArgs = arrayOf("My Title")
         */

        // How you want the results sorted in the resulting Cursor
        val sortOrder = "${PayloadReaderContract.PayloadEntry.COLUMN_NAME_SEQ} ASC"

        val cursor = db.query(
            PayloadReaderContract.PayloadEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null, //selection,              // The columns for the WHERE clause
            null, //selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )

        // Clear
        items = SparseArray<String>()
        arrayAdapter.clear()

        // Load
        with(cursor) {
            while (moveToNext()) {
                val itemSeq = getInt(getColumnIndexOrThrow(
                    PayloadReaderContract.PayloadEntry.COLUMN_NAME_SEQ))
                var itemMessage = getString(getColumnIndexOrThrow(
                    PayloadReaderContract.PayloadEntry.COLUMN_NAME_MESSAGE
                ))
                items.put(itemSeq, itemMessage)
            }
        }

        arrayAdapter.addAll(sparseArray2List(items)!!)


        txtNumOfPayload.text = items.size().toString();
        // Convert to array
        //val itemArray = sparseArray2List(items)!!.toTypedArray<String>()
    }

    private fun clearAllData() {
        val db = dbHelper.writableDatabase

        // Define 'where' part of query.
        val selection = "${PayloadReaderContract.PayloadEntry.COLUMN_NAME_SEQ} >= 0"
        // Issue SQL statement.
        val deletedRows = db.delete(PayloadReaderContract.PayloadEntry.TABLE_NAME, selection, null)
    }

    private fun sparseArray2List(sparseArray: SparseArray<String>): List<String>? {
        val list: MutableList<String> = LinkedList()
        for (i in 0 until sparseArray.size()) {
            list.add(sparseArray.keyAt(i).toString() + "|" + sparseArray[sparseArray.keyAt(i)])
        }
        return list
    }

    private fun sendAllData() {
        this.txtSendResult.text = "Connecting to " + this.urlEditText.text;

        val url = this.urlEditText.text.toString()

        for (i in 0 until this.items.size()) {
            val key = this.items.keyAt(i).toString()
            val message = this.items[this.items.keyAt(i)]

            this.sendGetRequest(url, key, message);
        }
    }

    private fun testConnection() {
        this.txtSendResult.text = "Connecting to " + this.urlEditText.text;

        val url = this.urlEditText.text.toString()

        try {
            this.sendGetRequest(url, "-1", "Test Ok");
        } catch (e:Exception) {
            this.txtSendResult.text = "Error: "+e.printStackTrace()+"\n"+this.txtSendResult.text
            this.txtSendResult.text = "Error: "+e.toString()+"\n"+this.txtSendResult.text
        }
    }

    private fun sendGetRequest(url: String, seq:String, message:String) {
        val reqParam = "?data=" + URLEncoder.encode("$seq|$message", "UTF-8")

        // Instantiate the RequestQueue
        val combinedURL = "$url$reqParam"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, combinedURL,
            Response.Listener<String> { response ->
                // Display the first 500 characters of the response string.
                this.txtSendResult.text = "$seq : $response\n${this.txtSendResult.text}"
            },
            Response.ErrorListener {
                this.txtSendResult.text = "$seq : Error sending\n${this.txtSendResult.text}"
            })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    /*
    private fun copy() {

        val clipboard: ClipboardManager? =
            getSystemService<Any>(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
    */

    /*
    private fun sendPostRequest(url:String, data:String): String {

        val reqParam = URLEncoder.encode("data", "UTF-8") +
                "=" + URLEncoder.encode(data, "UTF-8")
        val mURL = URL(url)

        with(mURL.openConnection() as HttpURLConnection) {
            // optional default is GET
            requestMethod = "POST"

            val wr = OutputStreamWriter(getOutputStream());
            wr.write(reqParam);
            wr.flush();

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                println("Response : $response")

                return response.toString()
            }
        }
    }*/

}