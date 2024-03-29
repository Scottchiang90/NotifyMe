package com.example.notifyme

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton


class LocationList : AppCompatActivity() {
    // get shared preference
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_list)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {saveLocation()}
        // set shared preference
        this.sharedPref = this.getSharedPreferences(getString(R.string.shared_pref), Context.MODE_PRIVATE)
        initChip()
        migrateLocations()
        displayLocations()
    }

    private fun migrateLocations(){
        if (sharedPref.getStringSet(getString(R.string.shared_pref_locations), mutableSetOf<String>()).isNullOrEmpty()) {
            // fetch location list
            val locations = mutableListOf<String>()
            for (x in 1..20) {
                val loc = sharedPref.getString("location$x", "")
                if (!loc.isNullOrBlank()) {
                    locations.add(loc)
                }
            }
            // save to Set<String>
            with(sharedPref.edit()) {
                putStringSet(getString(R.string.shared_pref_locations), locations.toMutableSet())
                apply()
            }
        }
    }

    private fun saveLocation() {
        // generate location list
        val locations = mutableListOf<String>()
        for (x in 1..20) {
            val location = findViewById<EditText>(resources.getIdentifier("location$x", "id", packageName))
                    .text.toString()
            if (location.isNotBlank()){
                locations.add(location)
            }
        }
        // save location list
        with (sharedPref.edit()) {
            putStringSet(getString(R.string.shared_pref_locations), locations.toMutableSet())
            apply()
        }
        Toast.makeText(this, "Gym Watchlist Saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun displayLocations () {
        // clear the list
        for (x in 1..20) {
            findViewById<EditText>(resources.getIdentifier("location$x", "id", packageName))
                    .setText("")
        }
        //fetch and set the list
        val locations = sharedPref.getStringSet(getString(R.string.shared_pref_locations), mutableSetOf<String>())?: mutableSetOf<String>()
        locations.forEachIndexed { index, location ->
            findViewById<EditText>(resources.getIdentifier("location${index+1}", "id", packageName))
                .setText(location)
        }
    }

    private fun initChip() {
        val statesNames = arrayOf(R.string.level_1_state, R.string.level_3_state, R.string.level_5_state, R.string.level_6_state)
        val chipNames = arrayOf(R.id.chip_lv_1, R.id.chip_lv_3, R.id.chip_lv_5, R.id.chip_lv_6)


        statesNames.forEachIndexed { index, state_name ->
            // get state from shared preference
            val state = sharedPref.getBoolean(getString(state_name), false)

            val chip = findViewById<Chip>(chipNames.get(index))
            chip.isChecked = state

            chip.setOnCheckedChangeListener { view, isChecked ->
                // save to shared preference
                with (sharedPref.edit()) {
                    putBoolean(getString(state_name), isChecked)
                    apply()
                }
            }
        }
    }
}
