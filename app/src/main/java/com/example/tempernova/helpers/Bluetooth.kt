package com.example.tempernova.helpers

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import com.example.tempernova.R
import com.example.tempernova.adapters.BTLEDeviceListAdapter
import com.example.tempernova.ui.bluetooth.BluetoothDeviceListFragment
import java.util.*

private const val SCAN_PERIOD: Long = 10000

class Bluetooth {
    var bluetoothHeadset: BluetoothHeadset? = null
    // Get the default adapter
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()  // code from https://github.com/gabrielseibel1/BLEHeater/blob/master/app/src/main/java/br/com/embs/bleheater/utils/BLEHelper.kt
//    val leDeviceListAdapter: LeDeviceListAdapter = null // this is annoying :(
    private val handler: Handler = Handler(Looper.getMainLooper())
    private lateinit var bluetoothDeviceListAdapter: BTLEDeviceListAdapter
    private lateinit var bluetoothGatt: BluetoothGatt
    private var bluetoothDevices: MutableList<BluetoothDevice> = mutableListOf()
    private var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var connectedDevice: BluetoothGatt

    private val commandQueue: Queue<Runnable>? = null
    private var commandQueueBusy = false
    private var mScanning: Boolean = false
    private var nrTries: Int = 0
    private var isRetrying: Boolean = false
    private val MAX_TRIES: Int = 3
    private var bleHandler = Handler()

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

        return BluetoothStates.ON
    }

    fun enableBluetooth(activity: Activity) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(activity, enableBtIntent, activity.resources.getInteger(R.integer.bluetooth_request_code), null)
        }
    }

    fun scanDevices(deleteStoredDevices: Boolean = false, specificDeviceToFind: BluetoothDevice? = null) { // from https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02
        val scanner = bluetoothAdapter!!.bluetoothLeScanner

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        var scanFilters: MutableList<ScanFilter>? = null

        if (specificDeviceToFind !== null) {
            val filter = ScanFilter.Builder()
                .setDeviceAddress(specificDeviceToFind.address)
                .build()
            scanFilters!!.add(filter)
        }

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

    private fun nextCommand() { // If there is still a command being executed then bail out
        if (commandQueueBusy) {
            return
        }
        // Check if we still have a valid gatt object
        if (bluetoothGatt == null) {
            Log.e(
                TAG,
                java.lang.String.format(
                    "ERROR: GATT is 'null' for peripheral '%s', clearing command queue",
                    connectedDevice.device
                )
            )
            commandQueue!!.clear()
            commandQueueBusy = false
            return
        }
        // Execute the next command in the queue
        if (commandQueue!!.size > 0) {
            val bluetoothCommand = commandQueue.peek()
            commandQueueBusy = true
            nrTries = 0
            bleHandler.post(Runnable {
                try {
                    bluetoothCommand.run()
                } catch (ex: Exception) {
                    Log.e(
                        TAG,
                        java.lang.String.format(
                            "ERROR: Command exception for device '%s'",
                            bluetoothGatt.device
                        ),
                        ex
                    )
                }
            })
        }
    }

    private fun retryCommand() {
        commandQueueBusy = false
        val currentCommand = commandQueue!!.peek()
        if (currentCommand != null) {
            if (nrTries >= MAX_TRIES) { // Max retries reached, give up on this one and proceed
                Log.v(TAG, "Max number of tries reached")
                commandQueue.poll()
            } else {
                isRetrying = true
            }
        }
        nextCommand()
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?, gatt: BluetoothGatt): Boolean {
        if (gatt == null) {
            Log.e(TAG, "ERROR: Gatt is 'null', ignoring read request")
            return false
        }
        // Check if characteristic is valid
        if (characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request")
            return false
        }
        // Check if this characteristic actually has READ property
        if (characteristic.properties and PROPERTY_READ == 0) {
            Log.e(TAG, "ERROR: Characteristic cannot be read")
            return false
        }
        // Enqueue the read command now that all checks have been passed
        val result = commandQueue!!.add(Runnable {
            if (!gatt.readCharacteristic(characteristic)) {
                Log.e(
                    TAG,
                    String.format(
                        "ERROR: readCharacteristic failed for characteristic: %s",
                        characteristic.uuid
                    )
                )
                completedCommand()

            } else {
                Log.d(
                    TAG,
                    String.format(
                        "reading characteristic <%s>",
                        characteristic.uuid
                    )
                )
                nrTries++
            }
        })

        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command")
        }

        return result
    }


    private fun connect(context: Context, device: BluetoothDevice) {
        val gatt = device.connectGatt(context, false, bleGattCallback, TRANSPORT_LE)
        bluetoothGatt = gatt
    }

    fun connectToDevice(context: Context, device: BluetoothDevice) {
        //connectGatt(Context context, boolean autoConnect,
        //        BluetoothGattCallback callback)
        //        connectedDevice = device.connectGatt(context, true, bluetoothGattCallback, TRANSPORT_LE)

        // Get device object for a mac address
        val device = bluetoothAdapter!!.getRemoteDevice(device.address)
        // Check if the peripheral is cached or not
        val deviceType = device.type

        if(deviceType == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            // The peripheral is not cached
            // we need to re-scan for it :(

            scanDevices(false, device)
        } else {
            // The peripheral is cached - we've already scanned for it!
            // lets call the connect function!
            connect(context, device)
        }
    }

    private val bleGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)

            // Perform some checks on the status field
            if (status != GATT_SUCCESS) {
                Log.e(TAG, String.format(Locale.ENGLISH,"ERROR: Read failed for characteristic: %s, status %d", characteristic!!.getUuid(), status))
                completedCommand()

                return
            }

            // Characteristic has been read so processes it
            // ...
            // We done, complete the command
            completedCommand()
        }


        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != GATT_SUCCESS) {
                Log.d("onCharacteristicWrite", "Failed write, retrying")
                gatt.writeCharacteristic(characteristic)
            }
//            Log.d("onCharacteristicWrite", byteArrToHex(characteristic.value))
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
//            Log.d("onCharacteristicChanged", byteArrToHex(characteristic.value))
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun completedCommand() {
        commandQueueBusy = false
        isRetrying = false
        commandQueue!!.poll()
        nextCommand()
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
}
