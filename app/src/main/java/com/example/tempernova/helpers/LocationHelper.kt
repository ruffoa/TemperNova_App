package com.example.tempernova.helpers

import android.app.Activity
import android.content.ContentValues
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.tempernova.MainActivity
import com.example.tempernova.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import java.text.SimpleDateFormat
import java.util.*

class LocationHelper {
    class LocationData(latLng: LatLng) {
        var latitude: Double = latLng.latitude
        var longitude: Double = latLng.longitude
        var time: Date = Date()
    }

    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    // 2
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    private val REQUEST_LOCATION_PERMISSIONS = 1
    public val REQUEST_CHECK_SETTINGS = 2

    private var locationList: MutableList<LocationData> = mutableListOf()

    public fun setFusedLocationProviderCLient(activity: Activity) {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)
    }

    public fun getFusedLocationProviderClient(): FusedLocationProviderClient {
        return this.mFusedLocationProviderClient
    }

    fun getLastLocation(activity: Activity) {
        mFusedLocationProviderClient.lastLocation.addOnCompleteListener(activity!!) { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                val res: Location? = task.result

                (activity as MainActivity).saveFloatPref(res!!.latitude.toFloat(), activity.getString(R.string.latitude_preference_key))
                (activity as MainActivity).saveFloatPref(res!!.longitude.toFloat(), activity.getString(R.string.longitude_preference_key))
                val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                val currentDate = sdf.format(Date())
                (activity as MainActivity).saveStringPref(currentDate, activity.getString(R.string.date_preference_key))
            } else {
                Log.w(ContentValues.TAG, "getLastLocation:exception", task.exception)
            }
        }
    }

    private fun startLocationUpdates(activity: Activity) {

        if (ActivityCompat.checkSelfPermission(activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                activity.resources.getInteger(R.integer.bluetooth_request_code))
            return
        }

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    fun createLocationRequest(activity: Activity) {
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 10000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates(activity)
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(activity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    public fun setUpLocationUpdates(activity: Activity) {
        createLocationRequest(activity)
    }

    public fun updateStateAndStartLocationUpdates(activity: Activity){
        locationUpdateState = true

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                val lastLocation = p0.lastLocation

                  locationList.add(LocationData(LatLng(lastLocation.latitude, lastLocation.longitude)))
            }
        }
        startLocationUpdates(activity)
    }
}