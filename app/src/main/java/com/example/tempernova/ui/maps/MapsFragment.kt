package com.example.tempernova.ui.maps

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.tempernova.R
import androidx.core.app.ActivityCompat
import com.example.tempernova.MainActivity

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

import java.text.SimpleDateFormat
import java.util.*

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    private lateinit var mapsViewModel: MapsViewModel
    private var mapView: MapView? = null
    private var gMap: GoogleMap? = null
    private val REQUEST_LOCATION_PERMISSIONS = 1
    private var isLocationEnabled = false
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mapsViewModel =
            ViewModelProviders.of(this).get(MapsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_maps, container, false)
        val textView: TextView = root.findViewById(R.id.text_map)
        mapsViewModel.text.observe(this, Observer {
            textView.text = it
        })

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.activity!!)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView) as MapView
        mapView!!.onCreate(savedInstanceState)
        mapView!!.onResume()
        mapView!!.getMapAsync(this)//when you already implement OnMapReadyCallback in your fragment

        GoogleApiClient.Builder(this.context!!).addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
    }

    override fun onMapReady(map: GoogleMap) {
        gMap = map

        if (ContextCompat.checkSelfPermission(
                this.activity!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this.activity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissions()
        } else {
            setLocation()
            isLocationEnabled = true

            gMap!!.setMyLocationEnabled(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the thing
                    isLocationEnabled = true
                    gMap!!.setMyLocationEnabled(true)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    System.out.println("BOO, the user disabled location permissions!")
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this.activity!!,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSIONS
        )
    }

    private fun setLocation() {
        mFusedLocationProviderClient.lastLocation.addOnCompleteListener(this.activity!!) { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                val res: Location? = task.result

                gMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(res!!.latitude, res!!.longitude), 18f))
                (activity as MainActivity).saveFloatPref(res!!.latitude.toFloat(), getString(R.string.latitude_preference_key))
                (activity as MainActivity).saveFloatPref(res!!.longitude.toFloat(), getString(R.string.longitude_preference_key))
                val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
                val currentDate = sdf.format(Date())
                (activity as MainActivity).saveStringPref(currentDate, getString(R.string.date_preference_key))
            } else {
                Log.w(TAG, "getLastLocation:exception", task.exception)
            }
        }
    }

    override fun onConnected(@Nullable bundle: Bundle?) {}

    override fun onConnectionSuspended(i: Int) {}

    override fun onConnectionFailed(@NonNull connectionResult: ConnectionResult) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == (activity as MainActivity).locationHelper.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                (activity as MainActivity).locationHelper.setUpLocationUpdates(this.activity!!)
                (activity as MainActivity).locationHelper.updateStateAndStartLocationUpdates(this.activity!!)
            }
        }
    }
}