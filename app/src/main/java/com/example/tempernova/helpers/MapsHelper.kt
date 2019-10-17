package com.example.tempernova.helpers

import android.app.Activity
import android.content.ContentValues
import android.location.Location
import android.util.Log
import com.example.tempernova.MainActivity
import com.example.tempernova.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import java.text.SimpleDateFormat
import java.util.*

fun setLocation(activity :Activity) {
    var mFusedLocationProviderClient :FusedLocationProviderClient = null
    mFusedLocationProviderClient.lastLocation.addOnCompleteListener(this.activity!!) { task: Task<Location> ->
        if (task.isSuccessful && task.result != null) {
            val res: Location? = task.result

            (activity as MainActivity).saveFloatPref(res!!.latitude.toFloat(), getString(R.string.latitude_preference_key))
            (activity as MainActivity).saveFloatPref(res!!.longitude.toFloat(), getString(R.string.longitude_preference_key))
            val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
            val currentDate = sdf.format(Date())
            (activity as MainActivity).saveStringPref(currentDate, getString(R.string.date_preference_key))
        } else {
            Log.w(ContentValues.TAG, "getLastLocation:exception", task.exception)
        }
    }
}