package com.ruffo.tempernova.helpers

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import com.ruffo.tempernova.MainActivity
import com.ruffo.tempernova.R
import com.ruffo.tempernova.adapters.BTLEDeviceListAdapter
import com.ruffo.tempernova.ui.bluetooth.BluetoothDeviceListFragment
import java.nio.charset.Charset
import java.util.*

private const val SCAN_PERIOD: Long = 10000
private const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

class Bluetooth {
    var bluetoothHeadset: BluetoothHeadset? = null
    // Get the default adapter
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()  // code from https://github.com/gabrielseibel1/BLEHeater/blob/master/app/src/main/java/br/com/embs/bleheater/utils/BLEHelper.kt
    private lateinit var bluetoothDeviceListAdapter: BTLEDeviceListAdapter
    private lateinit var bluetoothGatt: BluetoothGatt
    private var bluetoothDevices: MutableList<BluetoothDevice> = mutableListOf()
    private var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var connectedDevice: BluetoothGatt
    private lateinit var gattServices: List<BluetoothGattService>
    private var notifyingCharacteristics: MutableList<UUID> = mutableListOf()
    private lateinit var notifyingGattCharacteristic: BluetoothGattCharacteristic

    private var commandQueue: Queue<Runnable>? = null
    private var commandQueueBusy = false
    private var nrTries: Int = 0
    private var isRetrying: Boolean = false
    private val MAX_TRIES: Int = 3
    private var bleHandler = Handler()

    private lateinit var appContext: Context

    enum class BluetoothStates {
        UNAVAILABLE, OFF, ON, CONNECTED, DISCONNECTED
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
        if (!::appContext.isInitialized) {
            appContext = context
        }

        if (::bluetoothGatt.isInitialized && bluetoothGatt.device !== null) {
            Log.d(TAG, "Has a valid connection, returning CONNECTED")
            return BluetoothStates.CONNECTED
        }

        return BluetoothStates.ON
    }

    fun enableBluetooth(activity: Activity) {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(activity, enableBtIntent, activity.resources.getInteger(R.integer.bluetooth_request_code), null)
        }
    }

    fun scanDevices(deleteStoredDevices: Boolean = false, specificDeviceToFind: BluetoothDevice? = null, timeToScan: Long = 5000) { // from https://medium.com/@martijn.van.welie/making-android-ble-work-part-1-a736dcd53b02
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
            scanFilters = mutableListOf(filter)
        }

        if (deleteStoredDevices)
            deviceList = mutableListOf()

        if (scanner != null) {
            scanner.startScan(scanFilters, scanSettings, scanCallback) // get all devices for now, can choose to filter by name or mac addr later.
            Handler().postDelayed({ scanner.stopScan(scanCallback) }, timeToScan)    // scan for 5 seconds...
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
        if (commandQueue!!.size() > 0) {
            val bluetoothCommand = commandQueue!!.peek() as Runnable
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
                commandQueue!!.dequeue()
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
//                retryCommand()
            }
        })

        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command")
        }

        return result
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: Int, writeType: Int = WRITE_TYPE_DEFAULT): Boolean {
        // Check if this characteristic actually supports this writeType
        val writeProperty: Int = when (writeType) {
            WRITE_TYPE_DEFAULT -> PROPERTY_WRITE
            WRITE_TYPE_NO_RESPONSE -> PROPERTY_WRITE_NO_RESPONSE
            WRITE_TYPE_SIGNED -> PROPERTY_SIGNED_WRITE
            else -> 0
        }
        if (characteristic.properties and writeProperty === 0) {
            Log.e(
                TAG,
                java.lang.String.format(
                    Locale.ENGLISH,
                    "ERROR: Characteristic <%s> does not support writeType '%s'",
                    characteristic.uuid,
                    writeType
                )
            )
            return false
        }

        val bytesToWrite: ByteArray = data.toString().toByteArray(Charset.defaultCharset())
        characteristic.value = bytesToWrite
        characteristic.writeType = writeType

        if (!bluetoothGatt.writeCharacteristic(characteristic)) {
            Log.e(TAG, String.format("ERROR: writeCharacteristic failed for characteristic: %s", characteristic.uuid))
            completedCommand()
        } else {
            Log.d(TAG, String.format("writing <%s> to characteristic <%s>", bytesToWrite.toString(
                Charset.defaultCharset()), characteristic.uuid))
            nrTries++
        }

        return true
    }

    fun setNotify(characteristic: BluetoothGattCharacteristic, enable: Boolean): Boolean {
        // Check if characteristic is valid
        if(characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring setNotify request")
            return false
        }

        // Get the CCC Descriptor for the characteristic
        val descriptor: BluetoothGattDescriptor = characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID))

        if(descriptor == null) {
            Log.e(TAG, String.format("ERROR: Could not get CCC descriptor for characteristic %s", characteristic.getUuid()))
            return false
        }

        // Check if characteristic has NOTIFY or INDICATE properties and set the correct byte value to be written
        var value: ByteArray

        val properties: Int = characteristic.properties

        value = if (properties !== null && PROPERTY_NOTIFY > 0) {
            Log.d(TAG, "Enabling notifications on ${characteristic.descriptors} ${characteristic.value.toString(Charset.defaultCharset())}")
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else if (properties !== null && PROPERTY_INDICATE > 0) {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            Log.e(TAG, String.format("ERROR: Characteristic %s does not have notify or indicate property", characteristic.getUuid()))
            return false
        }

        lateinit var finalValue: ByteArray

        finalValue = if (enable) {
            value
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        // Queue Runnable to turn on/off the notification now that all checks have been passed
        val result = commandQueue!!.add(Runnable {
                // First set notification for Gatt object
                if (!bluetoothGatt.setCharacteristicNotification(
                        descriptor.characteristic,
                        enable
                    )
                ) {
                    Log.e(
                        TAG,
                        String.format(
                            "ERROR: setCharacteristicNotification failed for descriptor: %s",
                            descriptor.uuid
                        )
                    )
                }

                // Then write to descriptor
                descriptor.value = finalValue
                var result: Boolean = bluetoothGatt.writeDescriptor(descriptor)

                Log.d(TAG, "Calling Runnable func for the descriptor ${descriptor.characteristic} - ${descriptor.value.toString(
                    Charset.defaultCharset())}")
                if (!result) {
                    Log.e(
                        TAG,
                        String.format(
                            "ERROR: writeDescriptor failed for descriptor: %s",
                            descriptor.uuid
                        )
                    )
                    completedCommand()
                } else {
                    nrTries++
//                    retryCommand()
                }
        })

        if(result) {
            nextCommand()
        } else {
            Log.e(TAG, "ERROR: Could not enqueue write command");
        }

        return result
    }


    private fun waitAndGetServices() {
        bluetoothGatt.discoverServices()
    }

    private fun connect(context: Context, device: BluetoothDevice) {
        val gatt = device.connectGatt(context, false, bleGattCallback, TRANSPORT_LE)
        bluetoothGatt = gatt

        Log.d(TAG, "SUCCESS: Connected to ${device.name} (${device.address})")
        (context as MainActivity).bluetoothStatus = BluetoothStates.CONNECTED
        appContext = context

        Handler().postDelayed({ waitAndGetServices() }, 500)    // wait for ~.25 seconds...
        context.displayBluetoothPairedBanner(
            context.findViewById(R.id.nav_host_fragment),
            "${device.name} (${device.address}) connected"
        )
    }

    fun connectToDevice(context: Context, device: BluetoothDevice) {
        // Get device object for a mac address
        val device = bluetoothAdapter!!.getRemoteDevice(device.address)
        // Check if the peripheral is cached or not
        val deviceType = device.type

        if(deviceType == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            // The peripheral is not cached
            // we need to re-scan for it :(

            scanDevices(false, device, 500) // re-scan for ~.5 seconds to get the device info, then let's re-call this func again to try to connect!

            Handler().postDelayed({ connect(context, device) }, 510)    // wait for ~.5 seconds...
        } else {
            // The peripheral is cached - we've already scanned for it!
            // lets call the connect function!
            connect(context, device)
        }
    }

    fun handleTempCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, setNotifications: Boolean = false) {
        val value = characteristic!!.value.toString(Charset.defaultCharset())

        if (value.isNotEmpty() && value.toIntOrNull() !== null) {
            if (!::notifyingGattCharacteristic.isInitialized) {
                notifyingGattCharacteristic = characteristic
            }

            val temp = value.toInt()

            if (appContext !== null) {
                (appContext as MainActivity).currTemp = temp
                (appContext as MainActivity).changeDisabledState(false)
                (appContext as MainActivity).runOnUiThread {
                    (appContext as MainActivity).updateTemp(
                        (appContext as MainActivity).findViewById(
                            R.id.nav_host_fragment
                        )
                    )
                }
            }

            if (setNotifications) {
                setNotify(characteristic, true)
            }
        }
    }

    fun sendDesiredTemp(context: Context) {
        if ((context as MainActivity).bluetoothStatus === BluetoothStates.CONNECTED && ::gattServices.isInitialized && gattServices.isNotEmpty() && ::notifyingGattCharacteristic.isInitialized) {
            val temp = (context as MainActivity).temperature

            writeCharacteristic(notifyingGattCharacteristic, temp)
        }
    }

    private val bleGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(status == GATT_SUCCESS) {
                Log.d(TAG, "New state is: $newState")

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        // We successfully connected, proceed with service discovery

                        val bondState: Int = gatt.device.bondState
                        // Take action depending on the bond state
                        // Take action depending on the bond state
                        if (bondState == BOND_NONE || bondState == BOND_BONDED) { // Connected to device, now proceed to discover it's services but delay a bit if needed
                            var delayWhenBonded = 0
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                                delayWhenBonded = 1000
                            }
                            val delay =
                                if (bondState == BOND_BONDED) delayWhenBonded else 0
                            val discoverServicesRunnable = Runnable {
                                Log.d(
                                    TAG,
                                    java.lang.String.format(
                                        Locale.ENGLISH,
                                        "discovering services of '%s' with delay of %d ms",
                                        gatt.device.name,
                                        delay
                                    )
                                )
                                val result = gatt.discoverServices()
                                if (!result) {
                                    Log.e(TAG, "discoverServices failed to start")
                                }
//                            discoverServicesRunnable = null
                            }
                            bleHandler.postDelayed(discoverServicesRunnable, delay.toLong())
                        } else if (bondState == BOND_BONDING) { // Bonding process in progress, let it complete
                            Log.i(TAG, "waiting for bonding to complete")
                        }

                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        // We successfully disconnected on our own request
                        gatt.close()
                    }
                    else -> {
                        // We're CONNECTING or DISCONNECTING, ignore for now
                        Log.d(TAG, "New state is: $newState")
                        super.onConnectionStateChange(gatt, status, newState)
                    }
                }
            } else {
                // An error happened...figure out what happened!
                Log.d(TAG, "New state is: $newState")

                if (newState === BluetoothProfile.STATE_DISCONNECTED || newState === BluetoothProfile.STATE_DISCONNECTING) {
                    // handle location saving here, as we are disconnecting from the device...

                    if (::appContext.isInitialized) {
                        (appContext as MainActivity).bluetoothStatus = BluetoothStates.DISCONNECTED
                        (appContext as MainActivity).locationHelper.addLastKnownLocationToList(appContext as MainActivity)
                        (appContext as MainActivity).runOnUiThread {
                            (appContext as MainActivity).displayBluetoothDisconnectedBanner(
                                (appContext as MainActivity).findViewById(
                                    R.id.nav_host_fragment
                                ), "${gatt.device.name} Disconnected"
                            )
                            (appContext as MainActivity).changeDisabledState(true)
                            (appContext as MainActivity).updateTemp((appContext as MainActivity).findViewById(
                                R.id.nav_host_fragment
                            ))
                        }
                    }
                }
                gatt.close()
            }
        }

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

            handleTempCharacteristic(gatt!!, characteristic!!, true)
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
            Log.d(TAG,"onCharacteristicChanged - $characteristic - ${characteristic.value.toString(Charset.defaultCharset())}")
            handleTempCharacteristic(gatt, characteristic)

            super.onCharacteristicChanged(gatt, characteristic)
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            gattServices = gatt!!.services
            Log.d(TAG, "Bluetooth GATT Services Found: ${gattServices.size}")

            commandQueue = Queue(mutableListOf<Runnable>())
            gattServices.forEach { service ->
                Log.d(TAG, "Bluetooth GATT Service Found: ${service.characteristics} - ${service.uuid}")
                service.characteristics.forEach {
                    readCharacteristic(it, gatt)
                }
            }

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            // Do some checks first
            val parentCharacteristic: BluetoothGattCharacteristic = descriptor.characteristic
            if (status != GATT_SUCCESS) {
                Log.e(TAG, String.format("ERROR: Write descriptor failed -> characteristic: %s", parentCharacteristic.uuid))
            }

            // Check if this was the Client Configuration Descriptor
            if (descriptor.uuid == UUID.fromString(CCC_DESCRIPTOR_UUID)) {
                if (status == GATT_SUCCESS) {
                    // Check if we were turning notify on or off
                    val value: ByteArray = descriptor.value

                    if (value != null) {
                        if (value[0] != Byte.MIN_VALUE) {
                            // Notify set to on, add it to the set of notifying characteristics
                            notifyingCharacteristics.add(parentCharacteristic.uuid)

                            sendDesiredTemp(appContext) // send temp back -> this way the Arduino knows what the initial setting is...
                        }
                    } else {
                        // Notify was turned off, so remove it from the set of notifying characteristics
                        notifyingCharacteristics.remove(parentCharacteristic.uuid)
                    }
                }

                // This was a setNotify operation
                Log.d(TAG, "onDescriptorWrite called - ${descriptor.characteristic} - ${descriptor.value.toString(
                    Charset.defaultCharset())}")

            } else {
                // This was a normal descriptor write....
                super.onDescriptorWrite(gatt, descriptor, status)
            }

            completedCommand()
        }

    }

    private fun completedCommand() {
        commandQueueBusy = false
        isRetrying = false
        commandQueue!!.dequeue()
        nextCommand()
    }

    fun getDeviceList(): List<BluetoothDevice> {
        return deviceList
    }

    fun getConnectedDevice(): BluetoothGatt {
        return this.connectedDevice
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Log.d("BLUETOOTH", "Device found: " + device.name + ", " + device.type + ", " + device.address)

            if (!deviceList.contains(device) && device.name !== null) { // do we want to only show devices with a name?
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
