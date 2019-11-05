package com.example.gpslocation

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.gpslocation.GPSTracker.Companion.CHECK_GPS_POPUP
import com.example.gpslocation.GPSTracker.Companion.mRequestingLocationUpdates
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    lateinit var gpsTracker: GPSTracker
    var gpsModel: GPSModel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter("sendResult"))
    }


    /**
     * Initialization
     */
    fun initView() {
        gpsTracker = GPSTracker()
        gpsTracker.init(this)
        gpsTracker.getPermission(this,"Main")
    }


    /**
     * GPS Broadcast Receiver
     */
    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val bundle = intent.extras
                if (bundle != null) {
                    gpsModel = bundle.getSerializable("resultPojo") as GPSModel
                    if (gpsModel != null) {
                        when {
                            gpsModel?.address == Messages.gpsServiceMessage -> txtLocation.text =
                                Messages.gpsServiceMessage
                            gpsModel?.address == Messages.gpsNoAddressFound -> txtLocation.text =
                                Messages.gpsNoAddressFound
                            gpsModel?.address == Messages.gpsInvalidCoordinated -> txtLocation.text =
                                Messages.gpsInvalidCoordinated
                            else -> txtLocation.text = gpsModel?.address
                        }
                    }
                }
            } catch (e: Exception) {
                e.message
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mRequestingLocationUpdates!! && gpsTracker.checkPermissions(this)) {
            gpsTracker.gpsLocationPopup(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHECK_GPS_POPUP) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                }
                Activity.RESULT_CANCELED -> mRequestingLocationUpdates = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)
    }
}
