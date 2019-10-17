package com.example.tempernova.adapters
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.example.tempernova.R
//import kotlinx.android.synthetic.main.card_view.view.*
//
//class CardListAdapter(private var context: Context) : RecyclerView.Adapter<CardListAdapter.ViewHolder>() {
//
//    override fun getItemCount(): Int {
//        //override fun getItemCount() = PlaceData.placeList().size
//
//        return 0
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.row_places, parent, false)
//        return ViewHolder(itemView)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val place = PlaceData.placeList()[position]
//        holder.itemView.cardTitle.text = place.name
//        Picasso.with(context).load(place.getImageResourceId(context)).into(holder.itemView.placeImage)
//    }
//
//    // 2
//    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
//    }
//}
