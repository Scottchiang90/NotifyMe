package com.example.notifyme

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.DateFormat
import java.util.*

class RequestHandler(val context: Context, private var isNotify: Boolean): Response.Listener<JSONObject>, Response.ErrorListener {
    lateinit var notificationUtils: NotificationUtils
    lateinit var textView: TextView
    lateinit var progressBar: ProgressBar
    private val queue: RequestQueue = Volley.newRequestQueue(context) // Instantiate the RequestQueue
    var retryCount = 0

    constructor(context: Context):this(context, true) {
        this.notificationUtils = NotificationUtils(context)
    }

    constructor(context: Context, textView: TextView, progressBar: ProgressBar):this(context, false) {
        this.textView = textView
        this.progressBar = progressBar
    }


    override fun onResponse(response: JSONObject) {
        var message = context.getString(R.string.error_message)
        try {
            val jsonArray = response.getJSONArray("raids")
            message = getMatchingRaids(jsonArray)

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        passMessage(message)
        // reset retryCount
        retryCount = 0
    }

    override fun onErrorResponse(error: VolleyError) {
        error.printStackTrace()
        if (this.retryCount < 5){ // retry for 5 times
            this.searchOnLocations()
        } else{ // send error to user
            passMessage(context.getString(R.string.error_message))
        }
    }
    
    private fun passMessage(message: String){
        if (isNotify) {
            // Only notify if there are raids
            if (message!=context.getString(R.string.text_view_default)){
                val notification = notificationUtils.getNotificationBuilder(message).build()
                notificationUtils.getManager().notify(150, notification)
            }
        } else{
            textView.text = message
            textView.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }
    
    fun searchOnLocations() {
        // add time to url
        val timedUrl = context.getString(R.string.url) + "?time=" + System.currentTimeMillis().toString()

        // Request a string response from the provided URL.
        val stringRequest = JsonObjectRequest(
                Request.Method.GET, timedUrl, null,
                this, this)

        // Add the request to the RequestQueue.
        queue.add(stringRequest)

        // increment requestCount
        retryCount += 1
    }

    private fun getLocationList(): MutableList<String> {
        // get shared preference
        val sharedPref = context.getSharedPreferences(context.getString(R.string.shared_pref), Context.MODE_PRIVATE)
        //fetch the list
        val locations = mutableListOf<String>()
        for (x in 1..20) {
            val location = sharedPref.getString("location$x", "")
            if (location?.isNotBlank()!!) {
                locations.add(location.toLowerCase(Locale.ROOT))
            }
        }
        return locations
    }

    private fun getMatchingRaids(jsonArray: JSONArray): String {
        // split locations string
        val locationList = getLocationList()

        //generate results
        val results = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            val raid = jsonArray.getJSONObject(i)
            val gymName = raid.getString("gym_name")
            if (gymName.toLowerCase(Locale.ROOT) in locationList) {
                val level = raid.getInt("level")
                //val raid_spawn = timeToString(raid.getString("raid_spawn"))
                val raidStart = raid.getString("raid_start")
                //val raid_end = timeToString(raid.getString("raid_end"))
                val result = parseMatchingRaidResult(gymName, level, raidStart)
                if (result.isNotEmpty() && result !in results) // remove repeats
                    results.add(result)
            }
        }
        return if (results.isEmpty()) context.getString(R.string.text_view_default) else results.joinToString(separator = "\n\n")
    }

    private fun parseMatchingRaidResult(gymName: String, level: Int, raidStart: String): String{
        var result = ""
        if (level > 4) {
            val raidStartDate = Date(raidStart.toLong() * 1000)
            result = if (isNotify && Date().before(raidStartDate)){ // only notify upcoming raids
                "$gymName[$level]: ${minsToStart(raidStartDate)} mins"
            } else { // display all raids
                "$gymName[$level]: ${timeToString(raidStartDate)}"
            }
        }
        return result
    }

    private fun minsToStart(raidStartDate: Date): String {
        val diff: Long = raidStartDate.time - Date().time
        val seconds = diff / 1000
        return (seconds / 60).toInt().toString()
    }

    private fun timeToString(raidStartDate: Date): String {
        val df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.ENGLISH)
        val tz = TimeZone.getTimeZone("Asia/Singapore")
        df.calendar.timeZone = tz
        return df.format(raidStartDate)
    }
}

