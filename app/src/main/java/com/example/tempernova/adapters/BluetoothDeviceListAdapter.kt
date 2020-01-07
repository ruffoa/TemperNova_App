package com.example.tempernova.adapters

import android.bluetooth.BluetoothDevice
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.View.OnClickListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.example.tempernova.R

class BluetoothDeviceListAdapter(private val interaction: Interaction? = null) :
    ListAdapter<BluetoothDevice, BluetoothDeviceListAdapter.BluetoothDeviceViewHolder>(BluetoothDeviceDC()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BluetoothDeviceViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.bluetooth_popup, parent, false), interaction
    )

    override fun onBindViewHolder(holder: BluetoothDeviceViewHolder, position: Int) =
        holder.bind(getItem(position))

    fun swapData(data: List<BluetoothDevice>) {
        submitList(data.toMutableList())
    }

    inner class BluetoothDeviceViewHolder(
        itemView: View,
        private val interaction: Interaction?
    ) : RecyclerView.ViewHolder(itemView), OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {

            if (adapterPosition == RecyclerView.NO_POSITION) return

            val clicked = getItem(adapterPosition)
        }

        fun bind(item: BluetoothDevice) = with(itemView) {
            // TODO: Bind the data with View
        }
    }

    interface Interaction {

    }

    private class BluetoothDeviceDC : DiffUtil.ItemCallback<BluetoothDevice>() {
        override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            TODO(
                "not implemented"
            )
        }

        override fun areContentsTheSame(
            oldItem: BluetoothDevice,
            newItem: BluetoothDevice
        ): Boolean {
            TODO(
                "not implemented"
            )
        }
    }
}