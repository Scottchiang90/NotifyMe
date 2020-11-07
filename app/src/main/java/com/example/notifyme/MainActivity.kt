package com.example.notifyme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import java.text.DateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var alarmMgr: AlarmManager? = null
    private lateinit var alarmIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val tv = findViewById<TextView>(R.id.textView)
        tv.movementMethod = ScrollingMovementMethod()
        tv.setTextIsSelectable(true)
        initNotificationSwitch()
        searchOnLocations(tv)
    }

    fun initNotificationSwitch(){
        // get state from shared preference
        val sharedPref = this.getSharedPreferences(getString(R.string.shared_pref), Context.MODE_PRIVATE) ?: return
        val notificationState = sharedPref.getBoolean(getString(R.string.notification_state), false)
        val switch = findViewById<Switch>(R.id.switch_notification)
        switch.isChecked = notificationState
        switch.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                // The switch is On
                startNotifications(view)
            } else {
                // The switch is Off
                stopNotifications (view)
            }
            // save to shared preference
            with (sharedPref.edit()) {
                putBoolean(getString(R.string.notification_state), isChecked)
                apply()
            }
        }
    }

    fun startNotifications (view: View) {
        alarmMgr = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(this, AlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(this, 0, intent, 0)
        }

        // Set the alarm to start at approximately 8:00 a.m.
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 8)
        }

        //With setInexactRepeating(), you have to use one of the AlarmManager interval
        // constants--in this case, AlarmManager.INTERVAL_DAY.
        alarmMgr?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_HOUR,
            alarmIntent
        )
    }

    fun stopNotifications (view: View) {
        alarmMgr = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(this, AlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(this, 0, intent, 0)
        }
        alarmMgr?.cancel(alarmIntent)
    }

    fun goToSettings (view: View) {
        val intent = Intent(this, LocationList::class.java)
        startActivity(intent)
    }

    fun openMap (view: View) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_to_gym))))
    }

    /** Called when the user taps the Search button */
    fun searchOnLocations(view: View) {
        val textView = findViewById<TextView>(R.id.textView)
        textView.visibility = View.INVISIBLE

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)

        // add time to url
        val timed_url = getString(R.string.url) + "?time=" + System.currentTimeMillis().toString()

        // Request a string response from the provided URL.
        val stringRequest = JsonObjectRequest(
            Request.Method.GET, timed_url, null,
                { response ->try {
                    val jsonArray = response.getJSONArray("raids")
                    textView.text = getMatchingRaids(jsonArray, this)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    textView.text = "Unable to fetch from server."
                }
                    textView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                },
                { textView.text = "Unable to fetch from server." })

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}

fun getLocationList(context: Context): MutableList<String> {
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

fun getMatchingRaids(jsonArray: JSONArray, context: Context): String {
    // split locations string
    val locationList = getLocationList(context)

    //generate results
    val results = mutableListOf<String>()
    for (i in 0 until jsonArray.length()) {
        val raid = jsonArray.getJSONObject(i)
        val gymName = raid.getString("gym_name")
        if (gymName.toLowerCase(Locale.ROOT) in locationList) {
            val level = raid.getInt("level")
            if (level > 4) {
                //val raid_spawn = timeToString(raid.getString("raid_spawn"))
                val raid_start = timeToString(raid.getString("raid_start"))
                //val raid_end = timeToString(raid.getString("raid_end"))
                val result = "$gymName[$level]: $raid_start"
                if (result !in results) // remove repeats
                    results.add(result)
            }
        }
    }
    return if (results.isEmpty()) context.getString(R.string.text_view_default) else results.joinToString(separator = "\n\n")
}

fun timeToString(stringTime: String): String {
    val df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.ENGLISH)
    val tz = TimeZone.getTimeZone("Asia/Singapore")
    df.calendar.timeZone = tz
    return df.format(Date(stringTime.toLong() * 1000))
}