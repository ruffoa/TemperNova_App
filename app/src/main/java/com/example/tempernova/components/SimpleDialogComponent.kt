package com.example.tempernova.components

import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.tempernova.adapters.BluetoothDeviceListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SimpleDialogComponent: DialogFragment() {
    var bluetoothDeviceListAdapter: BluetoothDeviceListAdapter = BluetoothDeviceListAdapter()

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            // Add customization options here
            .create()
    }

    fun bluetoothDeviceToStringList(items: List<BluetoothDevice>): Array<String> {
        val list = mutableListOf<String>()

        for (device in items) {
            list.add("${device.name} (${device.address})")
        }

        return list.toTypedArray()
    }

    fun createDialog(view: View, dialogParent: ViewGroup, title: String, message: String, iconDrawable: Int, items: List<BluetoothDevice>): Dialog {
        var checkedItem: Int = 0
        val devList = bluetoothDeviceToStringList(items)
        Log.d("TAG", "Dluetooth Devices ${devList.size} ${devList[0]}")

        return MaterialAlertDialogBuilder(view.context)
            // Add customization options here
            .setIcon(iconDrawable)
//            .setMessage(message)
            .setTitle(title)
            // Confirming action
            .setPositiveButton("Confirm") { dialog, which ->
                // Do something for button click

                Toast.makeText(view.context, devList[checkedItem], Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            // Dismissive action
            .setNegativeButton("Dismiss") { dialog, which ->
                // Do something for button click
                dialog.cancel()
            }
//            // Neutral action
//            .setNeutralButton("Neutral") { dialog, which ->
//                // Do something for button click
//            }
            .setSingleChoiceItems(devList, checkedItem) { dialog, which ->
                // Do something for item chosen
                checkedItem = which
            }
            .create() // or show() if you want to show the Banner immediately
    }
}
