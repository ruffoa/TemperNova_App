package com.example.tempernova.helpers

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.tempernova.R
import com.example.tempernova.adapters.BTLEDeviceListAdapter

private const val SCAN_PERIOD: Long = 10000

class Bluetooth {
    var bluetoothHeadset: BluetoothHeadset? = null
    // Get the default adapter
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()  // code from https://github.com/gabrielseibel1/BLEHeater/blob/master/app/src/main/java/br/com/embs/bleheater/utils/BLEHelper.kt
//    val leDeviceListAdapter: LeDeviceListAdapter = null // this is annoying :(
    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var bluetoothDeviceListAdapter: BTLEDeviceListAdapter
    private var bluetoothDevices: MutableList<BluetoothDevice> = mutableListOf()

    private var mScanning: Boolean = false

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

        bluetoothDeviceListAdapter = BTLEDeviceListAdapter(bluetoothDevices, context)
        return BluetoothStates.ON
    }

    fun enableBluetooth(activity: Activity) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(activity, enableBtIntent, activity.resources.getInteger(R.integer.bluetooth_request_code), null)
        }
    }

    private class BLEScannerCallback(val deviceListAdapter: BTLEDeviceListAdapter) : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.d("BLE_SCAN", "Scan result : $result")
            deviceListAdapter.addDevice(result?.device)
            deviceListAdapter.notifyDataSetChanged()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("BLE_SCAN", "Scan failed with error $errorCode")
        }
    }


    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
//        runOnUiThread {
//            leDeviceListAdapter.addDevice(device)
//            leDeviceListAdapter.notifyDataSetChanged()
//        }
        bluetoothDeviceListAdapter.addDevice(device)
        bluetoothDeviceListAdapter.notifyDataSetChanged()
    }

    private fun scanLeDevice(enable: Boolean) {

//        val scanCallback = BLEScannerCallback(deviceListAdapter)
//        deviceListAdapter.clearDevices()

        when (enable) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    mScanning = false
                    bluetoothAdapter!!.stopLeScan(leScanCallback)
                }, SCAN_PERIOD)
                mScanning = true
                bluetoothAdapter!!.startLeScan(leScanCallback)
            }
            else -> {
                mScanning = false
                bluetoothAdapter!!.stopLeScan(leScanCallback)
            }
        }
    }

}
