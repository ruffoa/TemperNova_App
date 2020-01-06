package com.example.tempernova.components

import android.view.View
import android.view.ViewGroup

import com.sergivonavi.materialbanner.Banner
import com.sergivonavi.materialbanner.BannerInterface

class SimpleDialogComponent {

    fun createDialog(view: View, bannerParent: ViewGroup, message: String, actionMessage: String, iconDrawable: Int,
                     action: BannerInterface.OnClickListener? = null): Banner {
        return Banner.Builder(view.context).setParent(bannerParent)
            .setIcon(iconDrawable)
            .setMessage(message)
            .setLeftButton("Dismiss") {
                it.dismiss()
            }
            .setRightButton(actionMessage, action)
            .create() // or show() if you want to show the Banner immediately
    }
}
