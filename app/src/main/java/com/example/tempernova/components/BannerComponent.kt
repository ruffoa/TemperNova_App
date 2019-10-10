package com.example.tempernova.components

import android.view.View
import android.view.ViewGroup

import com.example.tempernova.R
import com.sergivonavi.materialbanner.Banner
import com.sergivonavi.materialbanner.BannerInterface

class BannerComponent {

    fun createBanner(view: View, bannerParent: ViewGroup, message: String, actionMessage: String, iconDrawable: Int, action: Function<BannerInterface.OnClickListener>? = null): Banner {
        return Banner.Builder(view.context).setParent(bannerParent)
            .setIcon(iconDrawable)
            .setMessage(message)
            .setLeftButton("Dismiss") {
                it.dismiss()
            }
            .setRightButton(actionMessage) {
                if (action !== null) action else null
            }
            .create() // or show() if you want to show the Banner immediately
    }
}
