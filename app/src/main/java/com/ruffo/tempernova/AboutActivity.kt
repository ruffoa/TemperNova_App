package com.ruffo.tempernova

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_chart.*

class AboutActivity : AppCompatActivity() {

    companion object {

        fun newIntent(context: Context): Intent {
            val intent = Intent(context, AboutActivity::class.java)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setSupportActionBar(toolbar)
        title = "About"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}