package com.example.tempernova.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tempernova.R
import com.example.tempernova.helpers.LocationHelper
import kotlinx.android.synthetic.main.card_view.view.*

class CardListAdapter(private var context: Context, private var data: MutableList<Any>) :
    RecyclerView.Adapter<CardListAdapter.ViewHolder>() {

    lateinit var itemClickListener: OnItemClickListener

    override fun getItemCount(): Int {
        //override fun getItemCount() = PlaceData.placeList().size
        if (data.isNullOrEmpty())
            return 0

        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.card_view, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {  // override fun onBindViewHolder(holder: ViewHolder, position: Int) { //
        val event = data[position] as LocationHelper.LocationData

        holder.itemView.cardTitle.text = event.address
        holder.itemView.cardText.text = event.time.toString()
        Log.d("TAG", "Address is " + event.address + " size is " + data.size)

        holder.itemView.cardImage.setImageDrawable(this.context.getDrawable(R.drawable.ic_sync_disabled_black_24dp))
//        Picasso.with(context).load(place.getImageResourceId(context)).into(holder.itemView.placeImage)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.cardHolder.setOnClickListener(this)
        }

        override fun onClick(view: View) = itemClickListener.onItemClick(itemView, adapterPosition)
    }

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    fun setOnItemClickListener(itemClickListener: OnItemClickListener) {
        this.itemClickListener = itemClickListener
    }
}

