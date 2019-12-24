package com.example.tempernova.helpers

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.tempernova.R
import com.example.tempernova.adapters.BTLEDeviceListAdapter
import com.example.tempernova.ui.bluetooth.BluetoothDeviceListFragment
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.internal.disposables.DisposableHelper.dispose
import io.reactivex.disposables.Disposable

private const val SCAN_PERIOD: Long = 10000

class Bluetooth {
    var bluetoothHeadset: BluetoothHeadset? = null
    // Get the default adapter
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()  // code from https://github.com/gabrielseibel1/BLEHeater/blob/master/app/src/main/java/br/com/embs/bleheater/utils/BLEHelper.kt
//    val leDeviceListAdapter: LeDeviceListAdapter = null // this is annoying :(
    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var bluetoothDeviceListAdapter: BTLEDeviceListAdapter
    private var bluetoothDevices: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var rxBleClient: RxBleClient
    private var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var connectedDevice: BluetoothGatt

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

        bluetoothDeviceListAdapter = BTLEDeviceListAdapter(bluetoothDevices, BluetoothDeviceListFragment())
        rxBleClient = RxBleClient.create(context)

        return BluetoothStates.ON
    }

    fun enableBluetooth(activity: Activity) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(activity, enableBtIntent, activity.resources.getInteger(R.integer.bluetooth_request_code), null)
        }
    }

    fun scanDevices(deleteStoredDevices: Boolean = false) { // from https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02
        val scanner = bluetoothAdapter!!.bluetoothLeScanner

        val scanSettings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilters: List<ScanFilter>? = null

        if (deleteStoredDevices)
            deviceList = mutableListOf()

        if (scanner != null) {
            scanner.startScan(scanFilters, scanSettings, scanCallback) // get all devices for now, can choose to filter by name or mac addr later.
            Handler().postDelayed({ scanner.stopScan(scanCallback) }, 5000)    // scan for 5 seconds...
            Log.d("BLUETOOTH", "scan started")
        } else {
            Log.e("BLUETOOTH", "could not get scanner object")
        }
    }

    fun connectToDevice(context: Context, device: BluetoothDevice) {
        //connectGatt(Context context, boolean autoConnect,
        //        BluetoothGattCallback callback)
//        connectedDevice = device.connectGatt(context, true, bluetoothGattCallback, TRANSPORT_LE)
    }

    fun getDeviceList(): List<BluetoothDevice> {
        return deviceList
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Log.d("BLUETOOTH", "Device found: " + device.name + ", " + device.type + ", " + device.address)

            if (!deviceList.contains(device)) {
                deviceList.add(device)
            }
            // ...do whatever you want with this found device
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // Ignore for now
        }

        override fun onScanFailed(errorCode: Int) {
            // Ignore for now
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

//    fun onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
//        if (status == GATT_SUCCESS) {
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                // We successfully connected, proceed with service discovery
//                gatt.discoverServices();
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                // We successfully disconnected on our own request
//                gatt.close();
//            } else {
//                // We're CONNECTING or DISCONNECTING, ignore for now
//            }
//        } else {
//            // An error happened...figure out what happened!
//            ...
//            gatt.close();
//        }
//    }

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

    fun scanForBluetoothDevices() {
        val devices: ScanResult

        val scanSubscription = rxBleClient.scanBleDevices(
            ScanSettings.Builder()
                // .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                // .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                .build()
            // add filters if needed
        )
            .subscribe(
                { scanResult ->
//                    devices = scanResult.bleDevice
                    // Process scan result here.
                },
                { throwable ->
                    // Handle an error here.
                }
            )

// When done, just dispose.
        scanSubscription.dispose()

    }

}
