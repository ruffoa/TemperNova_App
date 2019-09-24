package com.example.tempernova

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {
    var temperature: Int = 68
    var mPrefsTempVar = getSharedPreferences("TemperNova", 0)
    var mPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        mPrefs = this.getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)
        temperature = readIntegerSharedPrefs(resources.getInteger(R.integer.default_celcius_temperature), getString(R.string.temperature_preference_key))
    }

    fun readIntegerSharedPrefs(default: Int, key: String): Int {
//        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        return mPrefs?.getInt(key, default) ?: 0
//        return sharedPref.getInt(key, default)
    }

    fun bindButtonFunctions(view: View) {
        val tempDownButton: Button = view.findViewById(R.id.tempDownButton)
        val tempUpButton: Button = view.findViewById(R.id.tempUpButton)

        tempUpButton.setOnClickListener {
            temperature++
            updateTemp(view)
        }

        /** Called when the user touches the "-" button */
        tempDownButton.setOnClickListener {
            temperature--
            updateTemp(view)
        }
    }

    fun updateTemp(view: View) {
        val tempDisplayButton: Button = view.findViewById(R.id.tempDisplayButton)
        tempDisplayButton.text = temperature.toString() + getString(R.string.temperature_celcius_unit_string)
    }

    fun saveIntPref(value: Int, pref: String) {

    }
}
