package com.example.tempernova.helpers

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.tempernova.R

class Bluetooth {
    var bluetoothHeadset: BluetoothHeadset? = null
    // Get the default adapter
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    enum class BluetoothStates {
        UNAVAILABLE, OFF, ON, CONNECTED
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHeadset = proxy as BluetoothHeadset
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = null
            }
        }
    }

    fun checkBluetooth(context: Context): BluetoothStates {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            println("Bluetooth is not detected!")
            return BluetoothStates.UNAVAILABLE
        }

        if (!bluetoothAdapter.isEnabled) {
            println("Bluetooth is turned off :(")
            return BluetoothStates.OFF
        }

        // Establish connection to the proxy.
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)

        // ... call functions on bluetoothHeadset

        // Close proxy connection after use.
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)

        return BluetoothStates.ON
    }

    fun enableBluetooth(activity: Activity) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(activity, enableBtIntent, activity.resources.getInteger(R.integer.bluetooth_request_code), null)
        }
    }

}
