package com.example.notifyme

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton


class LocationList : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {saveLocation()}
        displayLocations ()
    }

    private fun saveLocation() {
        // get shared preference
        val sharedPref = this.getSharedPreferences( getString(R.string.shared_pref), Context.MODE_PRIVATE ) ?: return
        // save
        with (sharedPref.edit()) {
            for (x in 1..20) {
                putString("location$x",
                findViewById<EditText>(resources.getIdentifier("location$x", "id", packageName))
                    .text.toString())

            }
            apply()
        }
        Toast.makeText(this, "Gym Watchlist Saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun displayLocations () {
        // get shared preference
        val sharedPref = this.getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        ) ?: return

        //fetch and set the list
        for (x in 1..20) {
            findViewById<EditText>(resources.getIdentifier("location$x", "id", packageName))
                .setText(sharedPref.getString("location$x", ""))
        }
    }
}
