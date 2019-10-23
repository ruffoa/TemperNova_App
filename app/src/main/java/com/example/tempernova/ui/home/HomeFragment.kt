package com.example.tempernova.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.tempernova.MainActivity
import com.example.tempernova.R
import com.example.tempernova.components.BannerComponent
import com.example.tempernova.helpers.Bluetooth
import com.sergivonavi.materialbanner.Banner
import com.sergivonavi.materialbanner.BannerInterface

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private val bannerClass = BannerComponent()
    private lateinit var banner: Banner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(this, Observer {
            textView.text = it
        })

        (activity as MainActivity).bindButtonFunctions(root)
        (activity as MainActivity).updateTemp(root)
        (activity as MainActivity).checkAndUpdateBluetoothStatus()

        checkBluetooth(root)

        return root
    }

    fun checkBluetooth(view: View) {
        print("Device has bluetooth: ")
        println((activity as MainActivity).bluetoothStatus)

        if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.UNAVAILABLE) {
            displayWarningBanner(view, getString(R.string.bluetooth_unavailable), getString(R.string.bluetooth_unavailable_action))
        } else if ((activity as MainActivity).bluetoothStatus === Bluetooth.BluetoothStates.OFF) {
            displayWarningBanner(view, getString(R.string.bluetooth_off), getString(R.string.bluetooth_off_action))
        } else {
            if (::banner.isInitialized) {
                banner.dismiss()
            }
        }
    }

    private fun displayWarningBanner(view: View, msg: String, actionMsg: String) {
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
                (activity as MainActivity).locationHelper.updateStateAndStartLocationUpdates(this.activity!!)
            }
        }


    }
}