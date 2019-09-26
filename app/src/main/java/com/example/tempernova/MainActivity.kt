package com.example.tempernova

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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.example.tempernova.helpers.RepeatListener

class MainActivity : AppCompatActivity() {
    var temperature: Int = 68
    var currTemp: Int = 69
    var transitiondrawable: Drawable? = null
    private var isTempDownHeld = false;

    //    var mPrefsTempVar = getSharedPreferences("TemperNova", 0)
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
//        mPrefs = this.getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)
//        temperature = readIntegerSharedPrefs(resources.getInteger(R.integer.default_celcius_temperature), getString(R.string.temperature_preference_key))
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

        tempUpButton.setOnTouchListener(RepeatListener(400, 100, {
            temperature++
            updateTemp(view)
        }))

        /** Called when the user touches the "-" button */
        tempDownButton.setOnClickListener {
            temperature--
            updateTemp(view)
        }

        tempDownButton.setOnTouchListener(RepeatListener(400, 100, {
            temperature--
            updateTemp(view)
        }))
    }

    fun updateTemp(view: View) {
        val tempDisplayButton: Button = view.findViewById(R.id.tempDisplayButton)
        tempDisplayButton.text = temperature.toString() + getString(R.string.temperature_celcius_unit_string)
        if (temperature > currTemp) {
            val BackGroundColor = arrayOf(
                ColorDrawable(Color.parseColor("#ff0000")),
                ColorDrawable(Color.parseColor("#56ff00"))
            )

            transitiondrawable = TransitionDrawable(BackGroundColor)
            transitiondrawable = resources.getDrawable(R.drawable.button_bg_transition_default_to_warm, theme)
//            tempDisplayButton.background = transitiondrawable // broken, so disabled for now...
            tempDisplayButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorAccent))
            tempDisplayButton.isEnabled = true
        } else if (temperature === currTemp) {
            tempDisplayButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.material_on_surface_disabled))
            tempDisplayButton.isEnabled = false
        } else {
            tempDisplayButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorTempCooling))
            tempDisplayButton.isEnabled = true
        }
    }

    fun saveIntPref(value: Int, pref: String) {

    }
}
