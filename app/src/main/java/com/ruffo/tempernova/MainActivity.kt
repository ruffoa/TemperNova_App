package com.ruffo.tempernova

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ruffo.tempernova.components.BannerComponent
import com.ruffo.tempernova.components.SimpleDialogComponent
import com.ruffo.tempernova.helpers.Bluetooth
import com.ruffo.tempernova.helpers.LocationHelper
import com.ruffo.tempernova.helpers.RepeatListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.ruffo.tempernova.helpers.Temperature
import com.sergivonavi.materialbanner.Banner
import com.sergivonavi.materialbanner.BannerInterface
import kotlin.Error

class MainActivity: AppCompatActivity(), SimpleDialogComponent.SimpleDialogListener{
    var temperature: Int = 68
    var currTemp: Int? = null
    var isDisabled: Boolean = false

    var transitiondrawable: Drawable? = null
    var mPrefs: SharedPreferences? = null

    var bluetoothClass: Bluetooth = Bluetooth()
    var bluetoothStatus: Bluetooth.BluetoothStates = Bluetooth.BluetoothStates.UNAVAILABLE

    lateinit var locationHelper: LocationHelper

    var temperatureClass: Temperature = Temperature()

    private val bannerClass = BannerComponent()
    private lateinit var banner: Banner

    object CoreHelper {
        var contextGetter: (() -> Context)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoreHelper.contextGetter = {
            this
        }

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)

        setSupportActionBar(findViewById(R.id.appbar))  // set the appbar (orange thing with fragment name in it) to be the custom one we designed with buttons on it :)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
                R.id.navigation_home, R.id.navigation_maps, R.id.navigation_notifications))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        mPrefs = this.getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)
        temperature = readIntegerSharedPrefs(resources.getInteger(R.integer.default_celcius_temperature), getString(R.string.temperature_preference_key))
        temperatureClass.updateRefillsFromPrefs()
        bluetoothClass.createBluetoothManager(this.applicationContext)
        bluetoothStatus = bluetoothClass.checkBluetooth(this.applicationContext)

        if (!::locationHelper.isInitialized) {
            locationHelper = LocationHelper()
            locationHelper.setUpLocationUpdates(this)
            locationHelper.setUpPlacesApi(this)
            locationHelper.updateStateAndStartLocationUpdates(this)
        }
    }

    override fun onPause() {
        super.onPause()
        saveIntPref(temperature, getString(R.string.temperature_preference_key))

        if (::locationHelper.isInitialized)
            locationHelper.storeLocationList(this)
    }

    override fun onStop() {
        super.onStop()
        saveIntPref(temperature, getString(R.string.temperature_preference_key))
        temperatureClass.saveRefillsToPrefs()

        if (::locationHelper.isInitialized)
            locationHelper.storeLocationList(this)
    }

    override fun onResume() {
        super.onResume()

        if (::locationHelper.isInitialized)
            locationHelper.readLocationListFromPref(this)
    }

    fun updateRefillsCard(view: View) {
        val nRefills = temperatureClass.getTodaysRefills()

        if (nRefills === null)
            return

        Log.d("MAINACTIVITY", "NRefills: $nRefills")

        if (nRefills <= 0)
            return

        val refillsCard: MaterialCardView? = view.findViewById(R.id.homeRefillInfoCard)
        val refillsCardImage: ImageView? = view.findViewById(R.id.homeRefillInfoCardImage)
        val refillsCardTitle: TextView? = view.findViewById(R.id.homeRefillInfoCardTitle)
        val refillsCardText: TextView? = view.findViewById(R.id.homeRefillInfoCardText)

        if (refillsCard === null || refillsCardImage === null || refillsCardText === null || refillsCardTitle === null)
            return

        refillsCardImage.setImageDrawable(getDrawable(R.drawable.coffee_background))
        refillsCardTitle.text = getString(R.string.refill_card_title, nRefills)

        val averageRefills = temperatureClass.getAverageRefills()
        refillsCardText.text = getString(R.string.refill_card_text, averageRefills)

        refillsCard.setOnClickListener(onRefillsCardClickListener)
        refillsCard.visibility = View.VISIBLE
        Log.d("MAINACTIVITY", "SHOWING THE REFILLS CARD")

    }

    private val onRefillsCardClickListener = View.OnClickListener {
        Log.d("onRefillsCardClickListener", "Calling CARD ON CLICK!")

        val intent = Intent(this, ChartActivity::class.java).apply {}
        ActivityCompat.startActivity(this, intent, null)
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
        val tempDisplayButton: Button = view.findViewById(R.id.tempDisplayButton)

        tempDisplayButton.setOnClickListener {
            isDisabled = !isDisabled
            updateTemp(view)
        }

        tempUpButton.setOnClickListener {
            temperature++
            bluetoothClass.sendDesiredTemp()
            updateTemp(view)
        }

        tempUpButton.setOnTouchListener(RepeatListener(400, 100, {
            temperature++
            bluetoothClass.sendDesiredTemp()
            updateTemp(view)
        }))

        /** Called when the user touches the "-" button */
        tempDownButton.setOnClickListener {
            temperature--
            bluetoothClass.sendDesiredTemp()
            updateTemp(view)
        }

        tempDownButton.setOnTouchListener(RepeatListener(400, 100, {
            temperature--
            bluetoothClass.sendDesiredTemp()
            updateTemp(view)
        }))
    }

    fun changeDisabledState(disabled: Boolean) {
        isDisabled = disabled
    }

    fun updateTemp(view: View) {
//        val homeFrag: HomeFragment = supportFragmentManager.findFragmentByTag("HomeFragment") as HomeFragment

        try {
            val tempDisplayButton: Button? = view.findViewById(R.id.tempDisplayButton)
            val tempDownButton: Button? = view.findViewById(R.id.tempDownButton)
            val tempUpButton: Button? = view.findViewById(R.id.tempUpButton)

            if (tempDisplayButton === null || tempDownButton === null || tempUpButton === null) {
                return
            }

            if (bluetoothStatus == Bluetooth.BluetoothStates.CONNECTED && currTemp !== null) {
                tempDisplayButton.text = Html.fromHtml(
                    "<b><big>" + temperature.toString() + getString(R.string.temperature_celcius_unit_string) + "</big></b>" + "<br />" +
                            "<small>" + currTemp.toString() + getString(R.string.temperature_celcius_unit_string) + "</small>" + "<br />"
                )
            } else {
                Log.d("updateTemp", "BluetoothStatus is $bluetoothStatus")
                tempDisplayButton.text = Html.fromHtml("<b><big>" + temperature.toString() + getString(R.string.temperature_celcius_unit_string) + "</big></b>" + "<br />")
            }

            when {
                isDisabled -> {
                    tempDisplayButton.backgroundTintList =
                        ColorStateList.valueOf(getColor(R.color.material_on_surface_disabled))
                }
                currTemp !== null && temperature > currTemp!! -> {
                    val BackGroundColor = arrayOf(
                        ColorDrawable(Color.parseColor("#ff0000")),
                        ColorDrawable(Color.parseColor("#56ff00"))
                    )

                    transitiondrawable = TransitionDrawable(BackGroundColor)
                    transitiondrawable = resources.getDrawable(
                        R.drawable.button_bg_transition_default_to_warm,
                        theme
                    )
                    //            tempDisplayButton.background = transitiondrawable // broken, so disabled for now...
                    tempDisplayButton.backgroundTintList =
                        ColorStateList.valueOf(getColor(R.color.colorPrimaryDark))
                }
                currTemp !== null && temperature == currTemp -> {
                    tempDisplayButton.backgroundTintList =
                        ColorStateList.valueOf(getColor(R.color.colorTempDoneHex))
                }
                currTemp !== null && temperature <= currTemp!! -> {
                    tempDisplayButton.backgroundTintList =
                        ColorStateList.valueOf(getColor(R.color.colorTempCooling))
                }
                else -> {
                    tempDisplayButton.backgroundTintList =
                        ColorStateList.valueOf(getColor(R.color.material_on_surface_disabled))
                }
            }

            tempDisplayButton.isEnabled = !isDisabled
            tempDownButton.isEnabled = !isDisabled
            tempUpButton.isEnabled = !isDisabled
        } catch (e: Error) {
            Log.e("updateTemp (Main Activity)", "SOMETHING BROKE :( $e")
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

    // create an action bar button
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        menuInflater.inflate(R.menu.default_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            // User chose the "Settings" item, show the app settings UI...

            val intent = SettingsActivity.newIntent(this)
            ActivityCompat.startActivity(this, intent, null)

            true
        }

        R.id.action_about -> {
            // User chose the "About" action, show the about page...
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    fun dismissBanner() {
        if (::banner.isInitialized) {
            banner.dismiss()
        }
    }

    fun displayBluetoothPairedBanner(view: View, msg: String) {
        banner = bannerClass.createBanner(view, view.findViewById(R.id.home_root_linear_layout), msg, null, R.drawable.logo_round, BannerInterface.OnClickListener {
            it.dismiss()
        })

        banner.show()
    }

    fun displayBluetoothDisconnectedBanner(view: View, msg: String) {
        banner = bannerClass.createBanner(view, view.findViewById(R.id.home_root_linear_layout), msg, null, R.drawable.ic_bluetooth_disabled_black_24dp, BannerInterface.OnClickListener {
            it.dismiss()
        })

        banner.show()
    }


    override fun onDialogPositiveClick(dialog: DialogFragment) {

    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
