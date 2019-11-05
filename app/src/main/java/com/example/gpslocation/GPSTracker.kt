package com.example.gpslocation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.lang.Exception
import java.util.*

class GPSTracker {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mSettingsClient: SettingsClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest
    private lateinit var mLocationCallback: LocationCallback

    companion object {
        var mCurrentLocation: Location? = null
        const val CHECK_GPS_POPUP = 100
        var mRequestingLocationUpdates: Boolean? = null
    }


    /**
     * GPS Initialise
     */
    fun init(activity: Activity) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        mSettingsClient = LocationServices.getSettingsClient(activity)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                mCurrentLocation = locationResult?.lastLocation
                getGPS_Details(activity)
            }
        }
        mRequestingLocationUpdates = false
        mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
    }


    /**
     * check Permission
     */
    fun checkPermissions(activity: Activity): Boolean {
        val permissionState =
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }


    /**
     * show Access Permission Popup
     */
    fun getPermission(activity: Activity,from: String) {

        Dexter.withActivity(activity)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    mRequestingLocationUpdates = true
                    if(from == "mannual"){
                        gpsLocationPopup(activity)
                    }
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    if (response!!.isPermanentlyDenied) {
                        openSettings(activity)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token!!.continuePermissionRequest()
                }
            }).check()
    }


    /**
     * show LocationPopup
     */
    fun gpsLocationPopup(activity: Activity) {
        mSettingsClient
            .checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(activity) {
                mFusedLocationClient.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, myLooper()
                )
                getGPS_Details(activity)
            }.addOnFailureListener(activity) { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val rae = e as ResolvableApiException
                        rae.startResolutionForResult(activity, CHECK_GPS_POPUP)
                    } catch (sie: IntentSender.SendIntentException) {
                        e.message
                    }

                }
            }
    }

    /**
     * get Latitude
     */
    fun getLatitude(): Double {

        if (mCurrentLocation != null) {
            mCurrentLocation?.latitude
        }
        return 0.0
    }

    fun getLongitude(): Double {
        if (mCurrentLocation != null) {
            mCurrentLocation?.longitude
        }
        return 0.0
    }

    /**
     * Get Lat Long and current address
     */
    fun getGPS_Details(activity: Activity) {
        try {
            if (!CheckNetwork.isConnected(activity)) {
                Toast.makeText(activity, "No Internet Connection", Toast.LENGTH_LONG).show()
                return
            }
            if (mCurrentLocation != null) {
                val address =
                    getAddress(activity, mCurrentLocation?.latitude!!, mCurrentLocation?.longitude!!)
                val model = GPSModel()
                model.address = address
                model.latitude = mCurrentLocation?.latitude!!
                model.longitude = mCurrentLocation?.longitude!!
                getResult(activity, model)
            }
        } catch (e: Exception) {
            Log.d("TAGA",e.message)
        }
    }

    /**
     * Get Address From Lat Long
     */
    private fun getAddress(activity: Activity, lat: Double, lng: Double): String {
        return try {
            val geoCoder = Geocoder(activity, Locale.getDefault())
            val addresses = geoCoder.getFromLocation(lat, lng, 1)
            if (addresses != null) {
                val obj = addresses[0]
                obj.getAddressLine(0)
            } else {
                Messages.gpsNoAddressFound
            }
        } catch (e: IOException) {
            Messages.gpsServiceMessage
        } catch (i: IllegalArgumentException) {
            Messages.gpsInvalidCoordinated
        }

    }

    /** Open Setting*/
    fun openSettings(activity: Activity) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts(
            "package",
            BuildConfig.APPLICATION_ID, null
        )
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
    }



    /** Pass Data to Activity*/
    fun getResult(activity: Activity, model: GPSModel) {
        val bundle = Bundle()
        bundle.putSerializable("resultPojo", model)
        val intent = Intent("sendResult")
        intent.putExtras(bundle)
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
    }
}