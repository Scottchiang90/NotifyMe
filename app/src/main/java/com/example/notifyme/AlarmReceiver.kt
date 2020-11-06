package com.example.notifyme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        checkLocations(context)
    }

    fun notify(context: Context, message: String) {
        val notificationUtils = NotificationUtils(context)
        val notification = notificationUtils.getNotificationBuilder(message).build()
        notificationUtils.getManager().notify(150, notification)
    }

    fun checkLocations(context: Context) {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)

        // add time to url
        val timed_url = context.getString(R.string.url) + "?time=" + System.currentTimeMillis().toString()

        // Request a string response from the provided URL.
        val stringRequest = JsonObjectRequest(
            Request.Method.GET, timed_url, null,
                { response ->try {
                    val jsonArray = response.getJSONArray("raids")
                    // get shared preference
                    val sharedPref = context.getSharedPreferences(context.getString(R.string.shared_pref), Context.MODE_PRIVATE)
                    //fetch the list
                    val locations = sharedPref.getString(context.getString(R.string.location_names), "")
                    notify(context, getMatchingRaids(jsonArray, locations, context))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                },
                { notify(context, "Error Checking")  })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}