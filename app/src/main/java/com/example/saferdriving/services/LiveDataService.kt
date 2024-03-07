package com.example.saferdriving.services

import android.app.Service
import android.content.Intent
import android.location.Location
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.PermissionChecker
import com.android.volley.RequestQueue
import com.example.saferdriving.classes.ObdConnection
import com.example.saferdriving.dataclasses.Road
import com.example.saferdriving.dataclasses.SpeedAndAcceleration
import com.example.saferdriving.dataclasses.SpeedingRecording
import com.example.saferdriving.enums.Permissions
import com.example.saferdriving.enums.RoadType
import com.example.saferdriving.singletons.FirebaseManager
import com.example.saferdriving.utils.getRoad
import com.github.eltonvs.obd.command.NonNumericResponseException
import com.github.eltonvs.obd.command.ObdResponse
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LiveDataService : Service(){
    companion object {
        const val TAG = "LiveDataService"
    }

    private val firebaseManager = FirebaseManager.getInstance()

    private var isServiceActive = false

    //OBD data
    private var speed: ObdResponse? = null

    private var time: Long? = null
    private var speedingSecondsList: MutableList<SpeedingRecording> = mutableListOf()
    private var roadList: MutableList<Road> = mutableListOf()
    private var obdConnection: ObdConnection? = null
    private var mediaPlayer: MediaPlayer? = null
    private var location: Location? = null

    private var currentRoad: Road? = null
    //Road
    private var queue: RequestQueue? = null

    private var isSpeeding: Boolean = false

    private var topSpeed: Int = 0
    private var topSpeedCity: Int = 0
    private var topSpeedCountryRoad: Int = 0
    private var topSpeedHighway: Int = 0

    private var topLocalSpeed: Int = 0
    private var localStartTime: Long = 0
    private var localLocation: Location? = null
    private var localRoad: Road? = null

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
            override fun onLocationResult(locationResult: LocationResult) {
                Log.i(TAG, "New location result")
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    this@LiveDataService.location = location
                }
            }
        }
    }

    /**
     * Subscribes this application to get the location changes via the `locationCallback()`.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun subscribeToLiveData(
        obdConnection: ObdConnection,
        mediaPlayer: MediaPlayer,
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

        isServiceActive = true

        this.obdConnection = obdConnection
        this.mediaPlayer = mediaPlayer
        this.queue = queue

        GlobalScope.launch(Dispatchers.IO) {
            speed = obdConnection.getSpeed()
        }
        time = System.currentTimeMillis()
        startTime = time as Long

        // Sets the accuracy and desired interval for active location updates.
        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 200)
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

        // Start coroutine updating the road
        CoroutineScope(Dispatchers.IO).launch {
            while (isServiceActive) {
                Log.i(TAG, "Road loop iteration")
                if(location != null) {
                    getRoad(queue, location!!) { road ->
                        roadList.add(road)
                        currentRoad = road
                    }
                }
                // Delay for road API calls
                delay(2000)
            }
        }

        // Start Obd coroutine
        CoroutineScope(Dispatchers.IO).launch {
            while (isServiceActive) {
                Log.i(TAG, "Obd loop iteration")
                obdUpdates(1000)
            }
        }
    }

    /**
     * Unsubscribes this application of getting the location changes from  the `locationCallback()`.
     */
    fun unsubscribeToLiveData() {
        // Unsubscribe to location changes.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        isServiceActive = false

        if (isSpeeding) {
            time?.let { recordSpeeding(it) }
        }

        firebaseManager.addRideInfo(
            startTime,
            topSpeed,
            topSpeedHighway,
            topSpeedCountryRoad,
            topSpeedCity,
            speedingSecondsList
        )
    }

    private suspend fun obdUpdates(
        delay: Long
    ) {
        if (speed != null && time != null) {
            try {
                val response = obdConnection?.getSpeedAndAcceleration(speed!!, time!!, delay)
                response?.let { speedAndAcceleration ->
                    val timeOfResponse = (speedAndAcceleration.timeCaptured - startTime)
                    firebaseManager.addObdRecording(timeOfResponse, speedAndAcceleration)

                    speed = response.speed
                    time = response.timeCaptured
                    val speedVal = speed!!.value.toInt()

                    if (currentRoad != null && currentRoad!!.speedLimit < speedVal) {
                        // If speed increases while speeding, update topLocalSpeed
                        if (speedVal > topLocalSpeed) topLocalSpeed = speedVal

                        // If statement to check if it is a new speeding to be recorded
                        if (!isSpeeding) {
                            // If withSound is checked, play audio when the speeding starts
                            if (firebaseManager.getWithSound())
                                mediaPlayer?.start()

                            // Save info about when the speeding started
                            localStartTime = speedAndAcceleration.timeCaptured
                            localLocation = location
                            localRoad = currentRoad
                        }

                        isSpeeding = true

                    } else if (currentRoad != null && isSpeeding) {
                        recordSpeeding(speedAndAcceleration.timeCaptured)
                    }

                    if (speedVal > topSpeed) topSpeed = speedVal

                    if (currentRoad != null) {
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
            } catch (e: NonNumericResponseException) {
                
            }
        }
    }

    private fun recordSpeeding(
        endTime: Long
    ) {
        isSpeeding = false

        val secondsSpeeding = (endTime - localStartTime) / 1000
        val timeOfSpeedingStart = localStartTime - startTime

        val speedingRecording = firebaseManager.addSpeedingRecording(
            timeOfSpeedingStart,
            localLocation!!,
            localRoad!!,
            topSpeed,
            secondsSpeeding.toInt()
        )

        speedingSecondsList.add(speedingRecording)

        // Reset local info for next speeding
        localStartTime = 0
        localLocation = null
        localRoad = null
        topLocalSpeed = 0 // reset for next speed
    }
}