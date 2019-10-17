package com.example.tempernova

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
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
import android.webkit.WebViewFragment
import androidx.navigation.fragment.NavHostFragment
import com.example.tempernova.helpers.RepeatListener
import com.example.tempernova.helpers.Bluetooth
import com.example.tempernova.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {
    var temperature: Int = 68
    var currTemp: Int = 69
    var transitiondrawable: Drawable? = null
    var mPrefs: SharedPreferences? = null
    lateinit var bluetoothAdapter: BluetoothAdapter
    var bluetoothClass: Bluetooth = Bluetooth()
    var bluetoothStatus: Bluetooth.BluetoothStates = Bluetooth.BluetoothStates.UNAVAILABLE
//    private lateinit var cardListAdapter: TravelListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_maps, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        mPrefs = this.getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)
        temperature = readIntegerSharedPrefs(resources.getInteger(R.integer.default_celcius_temperature), getString(R.string.temperature_preference_key))
        bluetoothStatus = bluetoothClass.checkBluetooth(this.applicationContext)
    }

    override fun onPause() {
        super.onPause()
        saveIntPref(temperature, getString(R.string.temperature_preference_key))
    }

    override fun onStop() {
        super.onStop()
        saveIntPref(temperature, getString(R.string.temperature_preference_key))
    }

    fun readIntegerSharedPrefs(default: Int, key: String): Int {
        return mPrefs?.getInt(key, default) ?: 0
    }

    fun readFloatSharedPrefs(default: Float, key: String): Float {
        return mPrefs?.getFloat(key, default) ?: 0f
    }

    fun readStringSharedPrefs(default: String, key: String): String {
        return mPrefs?.getString(key, default) ?: "emptyString"
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
            tempDisplayButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.colorPrimaryDark))
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
        with(mPrefs!!.edit()) {
            putInt(pref, value)
            commit()
        }
    }

    fun saveFloatPref(value: Float, pref: String) {
        with(mPrefs!!.edit()) {
            putFloat(pref, value)
            commit()
        }
    }

    fun saveStringPref(value: String, pref: String) {
        with(mPrefs!!.edit()) {
            putString(pref, value)
            commit()
        }
    }

    fun checkAndUpdateBluetoothStatus() {
        bluetoothStatus = bluetoothClass.checkBluetooth(this.applicationContext)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val navHostFragment = supportFragmentManager.fragments.first() as? NavHostFragment
        if(navHostFragment != null) {
            val childFragments = navHostFragment.childFragmentManager.fragments
            childFragments.forEach { fragment ->
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }
}
