package com.example.tempernova.components

import android.view.View
import android.view.ViewGroup
import com.example.tempernova.R

import com.sergivonavi.materialbanner.Banner
import com.sergivonavi.materialbanner.BannerInterface

class BannerComponent {

    fun createBanner(view: View, bannerParent: ViewGroup, message: String, actionMessage: String?, iconDrawable: Int,
                     action: BannerInterface.OnClickListener? = null): Banner {
        val banner = Banner.Builder(view.context).setParent(bannerParent)
            .setIcon(iconDrawable)
            .setMessage(message)

        if (actionMessage !== null) {
            banner.setRightButton(actionMessage, action)
            .setLeftButton(view.context.getString(R.string.action_message_dismiss)) {
                it.dismiss()
            }
        } else {
            banner.setLeftButton(R.string.action_message_okay) {
                it.dismiss()
            }
        }

        return banner.create() // or show() if you want to show the Banner immediately
    }
}
