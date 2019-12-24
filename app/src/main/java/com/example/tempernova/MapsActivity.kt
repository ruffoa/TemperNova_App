package com.example.tempernova

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.graphics.Bitmap
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {

        private const val INTENT_LATITUDE = "intentLatitude"
        private const val INTENT_LONGITUDE = "intentLongitude"

        fun newIntent(context: Context, location: LatLng): Intent {
            val intent = Intent(context, MapsActivity::class.java)
            intent.putExtra(INTENT_LATITUDE, location.latitude)
            intent.putExtra(INTENT_LONGITUDE, location.longitude)

            return intent
        }
    }

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val latitude = intent.getDoubleExtra(INTENT_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(INTENT_LONGITUDE, 0.0)

        val mugLocation = LatLng(latitude, longitude)

        val b = BitmapFactory.decodeResource(resources, R.drawable.logo_round)
        val smallMarker = Bitmap.createScaledBitmap(b, 80, 80, false)

        mMap.addMarker(MarkerOptions().position(mugLocation).title("Mug").icon(BitmapDescriptorFactory.fromBitmap(smallMarker)))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mugLocation, 18f))
    }
}
