package com.example.gpslocation

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import java.lang.Exception

object CheckNetwork {

    fun isConnected(context: Context): Boolean {
        var connected = false
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nInfo = cm.getActiveNetworkInfo()
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected()
            return connected
        } catch (e: Exception) {
            Log.e("Connectivity Exception", e.message)
        }
        return connected
    }
}