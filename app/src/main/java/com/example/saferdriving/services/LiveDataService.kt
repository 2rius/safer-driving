package com.example.saferdriving.services

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.*
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Looper
import android.util.Log
import com.android.volley.RequestQueue
import com.example.saferdriving.classes.ObdConnection
import com.example.saferdriving.dataClasses.Acceleration
import com.example.saferdriving.dataClasses.Road
import com.example.saferdriving.dataClasses.SpeedAndAcceleration
import com.example.saferdriving.enums.RoadType
import com.example.saferdriving.utilities.getRoad
import com.github.eltonvs.obd.command.ObdResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*


class LiveDataService : Service(){
    //Sound
    var withSound: Boolean = false

    //OBD data
    var speed: ObdResponse? = null
    //var acceleration: Acceleration? = null
    var time: Long? = null
    var speedAndAccelerationsList: MutableList<SpeedAndAcceleration> = mutableListOf()
    var roadList: MutableList<Road> = mutableListOf()
    var obdConnection: ObdConnection? = null
    var longitude: Double? = null
    var latitude: Double? = null
    var currentRoad: Road? = null
    //Road
    var queue: RequestQueue? = null

    var topSpeed: Int = 0
    var topSpeedCity: Int = 0
    var topSpeedCountryRoad: Int = 0
    var topSpeedHighway: Int = 0

    var amountOfSpeeding: Int = 0
    var amountOfSpeedingCity: Int = 0
    var amountOfSpeedingCountryRoad: Int = 0
    var amountOfSpeedingHighway: Int = 0

    //Firebase
    var database: DatabaseReference = Firebase.database("https://safer-driving-default-rtdb.europe-west1.firebasedatabase.app/").reference

    //Geolocation service
    /**
     * Provides access to the Fused Location Provider API.
     */
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    /**
     * Callback for changes in location.
     */
    private lateinit var mLocationCallback: LocationCallback



    private var updateFunc: (Double, Double) -> Unit = {_: Double, _: Double -> }
    companion object {
        val TAG = LiveDataService::class.java.simpleName
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()")
        startLocationAware()
        return LocalBinder()
    }

    inner class LocalBinder : Binder() {
        fun getService(): LiveDataService = this@LiveDataService
    }

    /**
     * This method checks if the user allows the application uses all location-aware resources to
     * monitor the user's location.
     *
     * @return A boolean value with the user permission agreement.
     */
    private fun checkPermission() =
        PermissionChecker.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PermissionChecker.PERMISSION_GRANTED &&
                PermissionChecker.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PermissionChecker.PERMISSION_GRANTED



    /**
     * Start the location-aware instance and defines the callback to be called when the GPS sensor
     * provides a new user's location.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun startLocationAware() {

        // Show a dialog to ask the user to allow the application to access the device's location.
        // Start receiving location updates.
        mFusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this)

        // Initialize the `LocationCallback`.
        mLocationCallback = object : LocationCallback() {

            /**
             * This method will be executed when `FusedLocationProviderClient` has a new location.
             *
             * @param locationResult The last known location.
             */
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Log.d(TAG, "RUNNING THE LOOOOOOP")
                locationResult.lastLocation?.let { location ->
                    longitude = location.longitude
                    latitude = location.latitude
                    if( queue != null) {
                        getRoad(queue!!, location.latitude, location.longitude) { road ->
                            roadList.add(road)
                            currentRoad = road
                        }
                    }
                    updateFunc(location.latitude, location.longitude)
                }

                GlobalScope.launch(Dispatchers.IO) {
                    if (speed != null && time != null) {
                        val response = obdConnection?.getSpeedAndAcceleration(speed!!, time!!, 500)
                        response?.let { speedAndAcceleration -> speedAndAccelerationsList.add(speedAndAcceleration)
                            speed = response.speed
                            time = response.timeCaptured
                            val speedVal = speed!!.value.toInt()
                            if (currentRoad != null && currentRoad!!.speedLimit > speedVal) {
                                //sound
                                //checkbox
                                //speeding
                            }
                            if (speedVal > topSpeed) topSpeed = speedVal

                            if (currentRoad != null){
                                when (currentRoad!!.type) {
                                    RoadType.CITY ->
                                        if (speedVal > topSpeedCity) topSpeedCity = speedVal
                                    RoadType.RURAL ->
                                        if (speedVal > topSpeedCountryRoad) topSpeedCountryRoad = speedVal
                                    RoadType.MOTORWAY ->
                                        if (speedVal > topSpeedHighway) topSpeedHighway = speedVal
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Subscribes this application to get the location changes via the `locationCallback()`.
     */
    @OptIn(DelicateCoroutinesApi::class)
    public fun subscribeToLocationUpdates(obdConnection: ObdConnection, queue: RequestQueue, withSound: Boolean, updateFunc: (Double, Double) -> Unit, initFunc: (Double, Double) -> Unit)  {
        // Check if the user allows the application to access the location-aware resources.
        if (checkPermission())
            return

        this.updateFunc = updateFunc
        this.obdConnection = obdConnection
        this.queue = queue
        this.withSound = withSound
        val parentNode = if (withSound){
            "data_with_sound"
        } else {
            "data_without_sound"
        }

        database = database.child(parentNode)

        GlobalScope.launch(Dispatchers.IO) {
            speed = obdConnection.getSpeed()
        }
        time = System.currentTimeMillis()

        // Sets the accuracy and desired interval for active location updates.
        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .build()

        val locationResult = mFusedLocationClient.lastLocation
        locationResult.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                initFunc(task.result.latitude, task.result.longitude)
            }
        }
        // Subscribe to location changes.
        mFusedLocationClient.requestLocationUpdates(
            locationRequest, mLocationCallback, Looper.getMainLooper()
        )
    }
    /**
     * Unsubscribes this application of getting the location changes from  the `locationCallback()`.
     */
    public fun unsubscribeToLocationUpdates() {
        // Unsubscribe to location changes.


        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }
}