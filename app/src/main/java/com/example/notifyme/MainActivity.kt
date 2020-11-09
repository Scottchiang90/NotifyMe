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

    private fun initNotificationSwitch(){
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

        RequestHandler(context = this, textView=textView, progressBar=progressBar).searchOnLocations()
    }
}
