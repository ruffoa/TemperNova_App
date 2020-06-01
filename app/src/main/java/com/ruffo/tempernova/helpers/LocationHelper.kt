package com.ruffo.tempernova.helpers

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.ruffo.tempernova.MainActivity
import com.ruffo.tempernova.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class LocationHelper {
    class LocationData(latLng: LatLng, address: Address?, isConnected: Boolean? = false, bitmap: Bitmap? = null) {
        val latitude: Double = latLng.latitude
        val longitude: Double = latLng.longitude
        val time: Date = Date()
        val address: String = address!!.getAddressLine(0)
        val isConnected = isConnected!!
        val bitmap: Bitmap? = bitmap
    }

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var locationRequest: LocationRequest
    private lateinit var placesClient: PlacesClient

    private var locationUpdateState = false

    private val REQUEST_LOCATION_PERMISSIONS = 1
    val REQUEST_CHECK_SETTINGS = 2
    private val LOCATION_LIST_MAX_SIZE = 15
    private val gson = Gson()

    private var locationList: MutableList<LocationData> = mutableListOf()

    public fun setFusedLocationProviderCLient(activity: Activity) {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    }

    public fun getFusedLocationProviderClient(): FusedLocationProviderClient {
        return this.mFusedLocationProviderClient
    }

    fun setFusedLocationProviderListner(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(
                ((activity as MainActivity).baseContext.applicationContext),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                ((activity as MainActivity).baseContext.applicationContext),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mFusedLocationProviderClient.lastLocation.addOnCompleteListener(activity) { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                val res: Location? = task.result

                addPlaceToList(activity, res!!)
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

    private fun createLocationRequest(activity: Activity) {

        locationRequest = LocationRequest()

        locationRequest.interval = 300000    // in ms, so it is set to 5 minutes - will ask for updated location every 5 minutes

        locationRequest.fastestInterval = 5000  // set to 5 seconds - will accept location updates up to every 5 seconds!
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)


        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates(activity)
        }
        task.addOnFailureListener { e ->

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

    private fun getAddressFromLatLng(activity: Activity, latLng: LatLng): Address? {
        // 1
        val geocoder = Geocoder(activity)
        val addresses: List<Address>?

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {
                return addresses[0]
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return null
    }

    private fun getLocationPicture(place: Location) {
//        val fields = ArrayList()
//        fields.add(Place.Field.LAT_LNG)
//        fields.add(Place.Field.ADDRESS)
//        val currentPlaceRequest = FindCurrentPlaceRequest.newInstance(fields)
//
//        val currentPlaceTask = placesClient.findCurrentPlace(currentPlaceRequest)

//

        // Use fields to define the data types to return.
//        List<Place.Field> placeFields = Collections.singletonList(Place.Field.NAME);
//
//        // Use the builder to create a FindCurrentPlaceRequest.
//        FindCurrentPlaceRequest request =
//                FindCurrentPlaceRequest.newInstance(placeFields);
//
//        // Call findCurrentPlace and handle the response (first check that the user has granted permission).
//        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
//            placeResponse.addOnCompleteListener(task -> {
//                if (task.isSuccessful()){
//                    FindCurrentPlaceResponse response = task.getResult();
//                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
//                        Log.i(TAG, String.format("Place '%s' has likelihood: %f",
//                                placeLikelihood.getPlace().getName(),
//                                placeLikelihood.getLikelihood()));
//                    }
//                } else {
//                    Exception exception = task.getException();
//                    if (exception instanceof ApiException) {
//                        ApiException apiException = (ApiException) exception;
//                        Log.e(TAG, "Place not found: " + apiException.getStatusCode());
//                    }
//                }
//            });
//        } else {
//            // A local method to request required permissions;
//            // See https://developer.android.com/training/permissions/requesting
//            getLocationPermission();
//        }

    }

    private fun addPlaceToList(activity: Activity, place: Location) {
        val latLng = LatLng(place.latitude, place.longitude)
        val address = getAddressFromLatLng(activity, latLng)

        val lastLocation = if (locationList.size > 0) locationList.last() else null
        val locationBitmap = getLocationPicture(place)

//        Log.d("TAG", "trying to add to list!")
//        Log.d("TAG", "Address is " + address?.getAddressLine(0))

        if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.DISCONNECTED) {    // only add places if we've lost connection!

            if (address === null || address.getAddressLine(0) == "") {
                return
            }

            if (lastLocation !== null && lastLocation.address !== address.getAddressLine(0)) { // don't just continuously add the last place!
                Log.d(TAG, "Adding ${address.getAddressLine(0)} - last was ${lastLocation.address} -> ${address.getAddressLine(0) === lastLocation.address}")
                locationList.add(LocationData(latLng, address))
                if (locationList.size > LOCATION_LIST_MAX_SIZE) {
                    locationList = locationList.drop(locationList.size - LOCATION_LIST_MAX_SIZE)
                        .toMutableList()   // remove the first n elements from the list where n is the number of elements more than the set max we want to store.
                }
            } else {    // same location, so let's just update the time we saved!
                locationList = locationList.dropLast(1).toMutableList()
                locationList.add(LocationData(latLng, address))
            }
        }
    }

    fun addLastKnownLocationToList(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(
                ((activity as MainActivity).baseContext.applicationContext),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                ((activity as MainActivity).baseContext.applicationContext),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mFusedLocationProviderClient.lastLocation.addOnSuccessListener  {
            val location = it!!
            val latLng = LatLng(location.latitude, location.longitude)
            val address = getAddressFromLatLng(activity, latLng)
            val lastLocation = if (locationList.size > 0) locationList.last() else null


            if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.DISCONNECTED) {    // only add places if we've lost connection!

                if (address!!.getAddressLine(0) == "") {
                    // do nothing, this is "returning" from the listener function!
                }
                else if (lastLocation !== null && lastLocation.address !== address.getAddressLine(0)) { // don't just continuously add the last place!
                    locationList.add(LocationData(latLng, address))
                    if (locationList.size > LOCATION_LIST_MAX_SIZE) {
                        locationList = locationList.drop(locationList.size - LOCATION_LIST_MAX_SIZE)
                            .toMutableList()   // remove the first n elements from the list where n is the number of elements more than the set max we want to store.
                    }
                } else {    // same location, so let's just update the time we saved!
                    locationList = locationList.dropLast(1).toMutableList()
                    locationList.add(LocationData(latLng, address))
                }
            }
        }
    }

    fun setUpPlacesApi(activity: Activity) {
        Places.initialize(activity, activity.getString(R.string.google_maps_key))
    }

    fun setUpLocationUpdates(activity: Activity) {  // public fun by default :)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

        if (mFusedLocationProviderClient !== null) {
            createLocationRequest(activity)
            setFusedLocationProviderListner(activity)
        }
    }

    fun updateStateAndStartLocationUpdates(activity: Activity){
        locationUpdateState = true

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                val lastLocation = p0.lastLocation
                addPlaceToList(activity, lastLocation!!)
            }
        }

        startLocationUpdates(activity)
    }

    fun storeLocationList(activity: Activity) {
        if (activity !== null) {
            val json = gson.toJson(locationList)

            (activity as MainActivity).saveStringPref(json, activity.getString(R.string.location_list_preference_key))
        }
    }

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

    fun parseJsonList(json: String) {

    }

    fun readLocationListFromPref(activity: Activity) {
        if (activity !== null) {
            Log.d("TAG", "Reading location list from preferences! ")

            val jsonString = (activity as MainActivity).readStringSharedPrefs("", activity.getString(R.string.location_list_preference_key))
            if (jsonString !== "") {
                var locationListObject: MutableList<LocationData> = Gson().fromJson(jsonString)
                locationListObject = locationListObject.filter { l: LocationData -> l.address !== null }
                    .toMutableList()

                Log.d("TAG", "Parsed Location list is -> " + locationListObject.size)

                if (locationListObject !== null && locationListObject.size > 0) // check to make sure that the reading worked and we have some data in the object before overwriting the valid default / current data!
                    locationList = locationListObject
            }
        }
    }

    fun getLocationList(): MutableList<LocationData> {
                Log.d("TAG", "Location list length is " + locationList.size)
        return locationList
    }

    fun clearAllButLatest() {
        if (locationList.size > 0)
            locationList = locationList.drop(locationList.size - 1).toMutableList()     // drop all elements but the last one
    }

    fun clearAll() {
        locationList = mutableListOf()     // drop all elements
    }

    fun clearItemAtPosition(pos: Int) {
        if (locationList.size > 0 && locationList.size >= pos)
            locationList.removeAt(pos)     // drop specified element
    }
}