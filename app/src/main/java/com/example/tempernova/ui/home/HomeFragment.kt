package com.example.tempernova.ui.home

import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.tempernova.MainActivity
import com.example.tempernova.R
import com.example.tempernova.components.BannerComponent
import com.example.tempernova.helpers.Bluetooth
import com.sergivonavi.materialbanner.Banner
import com.sergivonavi.materialbanner.BannerInterface
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.view.WindowManager
import android.widget.PopupWindow
import android.view.Gravity
import com.example.tempernova.components.SimpleDialogComponent

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private val bannerClass = BannerComponent()
    private lateinit var banner: Banner

    private val dialogClass = SimpleDialogComponent()
    private lateinit var dialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
//        val textView: TextView = root.findViewById(R.id.text_home)
//        homeViewModel.text.observe(this, Observer {
//            textView.text = it
//        })

        (activity as MainActivity).bindButtonFunctions(root)
        (activity as MainActivity).updateTemp(root)
        (activity as MainActivity).checkAndUpdateBluetoothStatus()

        checkBluetooth(root)
        scanForDevices(root)

        waitForResult(root)

        return root
    }

    fun checkBluetooth(view: View) {
        print("Device has bluetooth: ")
        println((activity as MainActivity).bluetoothStatus)

        if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.UNAVAILABLE) {
            displayBluetoothDisabledWarningBanner(view, getString(R.string.bluetooth_unavailable), getString(R.string.bluetooth_unavailable_action))
        } else if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.OFF) {
            displayBluetoothDisabledWarningBanner(view, getString(R.string.bluetooth_off), getString(R.string.bluetooth_off_action))
        } else {
            if (::banner.isInitialized) {
                banner.dismiss()
            }
        }
    }

    fun scanForDevices(view: View) {
        if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.ON)
            (activity as MainActivity).bluetoothClass.scanDevices()
    }

    fun showBluetoothDebugPopup(view: View) {
        val args = Bundle()
        args.putString("devices", (activity as MainActivity).bluetoothClass.getDeviceList().toString())

//        val bluetoothPopup = Dialog(context!!, android.R.style.Theme_Black_NoTitleBar)
//        bluetoothPopup.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.argb(100, 0, 0, 0)))
//        bluetoothPopup.setContentView(R.layout.bluetooth_popup)
//        bluetoothPopup.setCancelable(true)
//        bluetoothPopup.show()

        val popupView = LayoutInflater.from(activity).inflate(R.layout.bluetooth_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        // define your view here that found in popup_layout
        // for example let consider you have a button

        val devices = popupView.findViewById(R.id.bluetooth_popup_devices) as TextView
        var devString = ""
        (activity as MainActivity).bluetoothClass.getDeviceList().forEach{dev ->
            devString += dev.name + ": " + dev.address + " " + dev.type + " " + dev.bondState + "\n"
        }

        devices.text = devString

        // If the PopupWindow should be focusable
        popupWindow.isFocusable = true
//        popupWindow.elevation = 2f

        // If you need the PopupWindow to dismiss when when touched outside
        popupWindow.setBackgroundDrawable(ColorDrawable())

        popupView.setOnTouchListener(View.OnTouchListener { v, event ->
            popupWindow.dismiss()
            true
        })

        // Using location, the PopupWindow will be displayed right under anchorView
        popupWindow.showAtLocation(
            view.rootView, Gravity.CENTER, 0, 0)
    }

    fun showDevices(view: View) {
        displayBluetoothPairingBanner(view, getString(R.string.bluetooth_select_device), getString(R.string.bluetooth_select_device_action))
//        showBluetoothDebugPopup(view)
    }

    fun waitForResult(view: View) {
        if ((activity as MainActivity).bluetoothClass.getDeviceList().isNotEmpty())
            showDevices(view)
        else
            Handler().postDelayed({ showDevices(view) }, 5000)    // wait for 5 seconds...
    }

    private fun displayChooseDeviceDialog(view: View, title: String, msg: String, icon: Int, items: List<BluetoothDevice>) {
        dialog = dialogClass.createDialog(view, view.findViewById(R.id.home_root_linear_layout), title, msg, R.drawable.ic_settings_bluetooth_black_24dp, items)
        dialog.show()
    }

    private fun displayBluetoothPairingBanner(view: View, msg: String, actionMsg: String) {
        banner = bannerClass.createBanner(view, view.findViewById(R.id.home_root_linear_layout), msg, actionMsg, R.drawable.ic_bluetooth_black_24dp, BannerInterface.OnClickListener {
            if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.ON) {
                val items = (activity as MainActivity).bluetoothClass.getDeviceList()

                displayChooseDeviceDialog(view, getString(R.string.bluetooth_select_device_action), getString(R.string.bluetooth_select_device_message), R.drawable.logo_round, items)
                it.dismiss()
            }
        })
        println(banner)

        banner.show()
    }

    private fun displayBluetoothDisabledWarningBanner(view: View, msg: String, actionMsg: String) {
        banner = bannerClass.createBanner(view, view.findViewById(R.id.home_root_linear_layout), msg, actionMsg, R.drawable.ic_bluetooth_disabled_black_24dp, BannerInterface.OnClickListener {
            (activity as MainActivity).bluetoothClass.enableBluetooth(this.activity!!)

            if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.ON) {
                it.dismiss()
            }
        })
                println(banner)

        banner.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode === Activity.RESULT_OK && requestCode === resources.getInteger(R.integer.bluetooth_request_code)) {
            println("RESULT OK!")

            (activity as MainActivity).bluetoothStatus = Bluetooth.BluetoothStates.ON
            checkBluetooth(this.view!!)
        }

        if (requestCode == (activity as MainActivity).locationHelper.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                (activity as MainActivity).locationHelper.setUpLocationUpdates(this.activity!!)
                (activity as MainActivity).locationHelper.updateStateAndStartLocationUpdates(this.activity!!)
            }
        }
    }
}