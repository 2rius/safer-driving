package com.example.saferdriving.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.content.PermissionChecker
import com.android.volley.RequestQueue
import com.example.saferdriving.classes.ObdConnection
import com.example.saferdriving.dataclasses.Road
import com.example.saferdriving.dataclasses.SpeedingRecording
import com.example.saferdriving.enums.Permissions
import com.example.saferdriving.enums.RoadType
import com.example.saferdriving.singletons.FirebaseManager
import com.example.saferdriving.utils.getRoad
import com.github.eltonvs.obd.command.ObdResponse
import com.google.android.gms.location.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class LiveDataService : Service(){
    val firebaseManager = FirebaseManager.getInstance()

    //OBD data
    var speed: ObdResponse? = null
    //var acceleration: Acceleration? = null
    var time: Long? = null
    var speedingSecondsList: MutableList<SpeedingRecording> = mutableListOf()
    var roadList: MutableList<Road> = mutableListOf()
    var obdConnection: ObdConnection? = null
    var location: Location? = null

    var currentRoad: Road? = null
    //Road
    var queue: RequestQueue? = null

    var isSpeeding: Boolean = false

    var topSpeed: Int = 0
    var topSpeedCity: Int = 0
    var topSpeedCountryRoad: Int = 0
    var topSpeedHighway: Int = 0

    var topLocalSpeed: Int = 0
    var localSecondsOverSpeed: Long? = null

    private var startTime: Long = 0

    //Geolocation service
    /**
     * Provides access to the Fused Location Provider API.
     */
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    /**
     * Callback for changes in location.
     */
    private lateinit var mLocationCallback: LocationCallback


    override fun onBind(intent: Intent?): IBinder {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        startLocationAware()
        return LocalBinder()
    }

    inner class LocalBinder : Binder() {
        fun getService(): LiveDataService = this@LiveDataService
    }

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
            @SuppressLint("SimpleDateFormat")
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    this@LiveDataService.location = location
                    if( queue != null) {
                        getRoad(queue!!, location) { road ->
                            roadList.add(road)
                            currentRoad = road
                        }
                    }

                    GlobalScope.launch(Dispatchers.IO) {
                        if (speed != null && time != null) {
                            val response = obdConnection?.getSpeedAndAcceleration(speed!!, time!!, 500)
                            response?.let { speedAndAcceleration ->
                                val timeOfResponse = (speedAndAcceleration.timeCaptured - startTime)
                                firebaseManager.addObdRecording(timeOfResponse, speedAndAcceleration)

                                speed = response.speed
                                time = response.timeCaptured
                                val speedVal = speed!!.value.toInt()

                                if (currentRoad != null && currentRoad!!.speedLimit < speedVal) {
                                    //sound
                                    //speeding
                                    if (speedVal > topLocalSpeed) topLocalSpeed = speedVal
                                    if (!isSpeeding){
                                        localSecondsOverSpeed = speedAndAcceleration.timeCaptured
                                    }

                                    isSpeeding = true

                                } else if (currentRoad != null && isSpeeding){
                                    val secondsSpeeding = (speedAndAcceleration.timeCaptured - localSecondsOverSpeed!!) / 1000

                                    val speedingRecording = firebaseManager.addSpeedingRecording(
                                        timeOfResponse,
                                        location,
                                        currentRoad!!,
                                        topSpeed,
                                        secondsSpeeding.toInt()
                                    )

                                    isSpeeding = false
                                    speedingSecondsList.add(speedingRecording)
                                    topLocalSpeed = 0 // reset for next speed
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
    }

    /**
     * Subscribes this application to get the location changes via the `locationCallback()`.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun subscribeToLocationUpdates(
        obdConnection: ObdConnection,
        queue: RequestQueue,
        initFunc: (Location) -> Unit
    )  {
        // Check if the user allows the application to access the location-aware resources.
        if (
            !Permissions.LOCATION.permissions.all { permission ->
                PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
            }
        )
            return

        this.obdConnection = obdConnection
        this.queue = queue

        GlobalScope.launch(Dispatchers.IO) {
            speed = obdConnection.getSpeed()
        }
        time = System.currentTimeMillis()
        startTime = time as Long

        // Sets the accuracy and desired interval for active location updates.
        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .build()

        val locationResult = mFusedLocationClient.lastLocation
        locationResult.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                initFunc(task.result)
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
    fun unsubscribeToLocationUpdates() {
        // Unsubscribe to location changes.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)

        firebaseManager.addRideInfo(
            startTime,
            topSpeed,
            topSpeedHighway,
            topSpeedCountryRoad,
            topSpeedCity,
            speedingSecondsList
        )
    }
}