package com.ruffo.tempernova.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.ruffo.tempernova.MainActivity
import com.ruffo.tempernova.MapsActivity

import com.ruffo.tempernova.R
import com.ruffo.tempernova.adapters.CardListAdapter
import com.ruffo.tempernova.helpers.LocationHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.card_view.*
import kotlinx.android.synthetic.main.fragment_notifications.*

class NotificationsFragment : Fragment() {
    private lateinit var staggeredLayoutManager: StaggeredGridLayoutManager
    private lateinit var adapter: CardListAdapter

    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var locationList: MutableList<LocationHelper.LocationData>

    private val onItemClickListener = object : CardListAdapter.OnItemClickListener {
        override fun onItemClick(view: View, position: Int) {
            Toast.makeText(context, "${locationList[position].address}", Toast.LENGTH_LONG).show()

//            var fragment = MapsFragment()
//
//            activity!!.supportFragmentManager
//                .beginTransaction()
//                .replace(R.id.nav_host_fragment, fragment)
//                .addToBackStack(tag)
//                .commit()
            val latLng = LatLng(locationList[position].latitude, locationList[position].longitude)

            val intent = MapsActivity.newIntent(activity!!, latLng)
            Log.d("TAG", "Location being sent (latlng) is: " + latLng.latitude + " - " + latLng.longitude)

            // 1
            val placeImage = view.findViewById<ImageView>(R.id.cardImage)
            val placeNameHolder = view.findViewById<LinearLayout>(R.id.cardTitleHolder)

            // 2
            val imagePair = Pair(cardImage as View, "tImage")
            val holderPair = Pair(cardTitleHolder as View, "tNameHolder")

            // 3
//            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!,
//                imagePair, holderPair)
            ActivityCompat.startActivity(activity!!, intent, null)
        }
    }

    private val onItemSwipeListener = object: ItemTouchHelper.SimpleCallback(0,
        ItemTouchHelper.RIGHT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            removeNotification(viewHolder.adapterPosition)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        notificationsViewModel =
            ViewModelProviders.of(this).get(NotificationsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_notifications, container, false)

        staggeredLayoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
        setHasOptionsMenu(true)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notifications_list.layoutManager = staggeredLayoutManager

        locationList = (activity as MainActivity).locationHelper.getLocationList()

        adapter = CardListAdapter(context!!, locationList as MutableList<Any>)
        notifications_list.adapter = adapter

        adapter.setOnItemClickListener(onItemClickListener)


        val myHelper = ItemTouchHelper(onItemSwipeListener)
        myHelper.attachToRecyclerView(notifications_list)
    }

    // create an action bar button
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.clear_menu_button, menu)    // add the clear button to the toolbar (orange bar) menu
        return super.onCreateOptionsMenu(menu, inflater)    // return the inflated menu with the parent menu (default_menu.xml) plus our added button!
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_clear_all_notifications -> {
            // User chose the "Clear all" item, lets remove all locations from the list but the latest one...

            clearAllData()
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun clearAllData() {
        val oldSize = locationList.size
        (activity as MainActivity).locationHelper.clearAllButLatest()
        locationList = (activity as MainActivity).locationHelper.getLocationList()
        adapter.notifyItemRangeRemoved(0, oldSize - locationList.size)
    }

    private fun removeNotification(pos: Int) {
        (activity as MainActivity).locationHelper.clearItemAtPosition(pos)
        locationList = (activity as MainActivity).locationHelper.getLocationList()
        adapter.notifyItemRemoved(pos)
    }
}
