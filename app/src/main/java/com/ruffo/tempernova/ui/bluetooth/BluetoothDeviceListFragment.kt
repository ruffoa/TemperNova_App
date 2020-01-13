package com.ruffo.tempernova.ui.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ruffo.tempernova.R
import com.ruffo.tempernova.adapters.BTLEDeviceListAdapter

class BluetoothDeviceListFragment : Fragment() {

    private var listener: OnListFragmentInteractionListener? = null

    lateinit var deviceListAdapter: BTLEDeviceListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bluetooth_device_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            view.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = deviceListAdapter
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
//            deviceListAdapter = BTLEDeviceListAdapter(mutableListOf(), listener!!)
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onDeviceSelected(device: BluetoothDevice)
    }
}
