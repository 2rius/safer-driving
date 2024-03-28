package com.example.saferdriving.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.PermissionChecker
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.saferdriving.R
import com.example.saferdriving.classes.BluetoothObdConnection
import com.example.saferdriving.classes.ObdConnection
import com.example.saferdriving.classes.WifiObdConnection
import com.example.saferdriving.dataclasses.Location
import com.example.saferdriving.dataclasses.Road
import com.example.saferdriving.dataclasses.SpeedingRecording
import com.example.saferdriving.dataclasses.TrafficInfo
import com.example.saferdriving.enums.Permissions
import com.example.saferdriving.enums.RoadType
import com.example.saferdriving.singletons.FirebaseManager
import com.example.saferdriving.utils.getRoad
import com.example.saferdriving.utils.getTrafficInfo
import com.github.eltonvs.obd.command.NonNumericResponseException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LiveDataService : Service() {
    companion object {
        const val TAG = "LiveDataService"
        const val ERROR_BROADCAST = "livedataServiceError"
        const val ERROR_EXTRA = "errorExtra"
        const val CHANNEL_ID = "SaferDriving:LiveData"
        const val CHANNEL_NAME = "LiveData"
    }

    private val firebaseManager = FirebaseManager.getInstance()

    private lateinit var obdConnection: ObdConnection
    private lateinit var queue: RequestQueue
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var wakeLock: WakeLock

    // Fused location provider variables
    /**
     * Provides access to the Fused Location Provider API.
     */
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    /**
     * Callback for changes in location.
     */
    private lateinit var mLocationCallback: LocationCallback
    private var location: Location? = null

    // Relevant to check when the service stops
    private var isServiceActive: Boolean = false

    // Variables for logging the data
    private var speed: Int? = null
    private var rpm: Double? = null
    private var loadLevel: Double? = null
    private var time: Long? = null
    private var startTime: Long = 0
    private var speedingSecondsList: MutableList<SpeedingRecording> = mutableListOf()
    private var roadList: MutableList<Road> = mutableListOf()

    private var currentRoad: Road? = null
    private var isSpeeding: Boolean = false

    private var topSpeed: Int = 0
    private var topSpeedCity: Int = 0
    private var topSpeedCountryRoad: Int = 0
    private var topSpeedHighway: Int = 0

    private var topLocalSpeed: Int = 0
    private var topLocalRPM: Double = 0.0
    private var topLocalLoadLevel: Double = 0.0
    private var localStartTime: Long = 0
    private var localLocation: Location? = null
    private var localRoad: Road? = null
    private var localTraffic: TrafficInfo? = null

    private var currentTrafficInfo: TrafficInfo? = null
    private var fuelType: String = "Unknown"

    override fun onBind(p0: Intent?): IBinder? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand called")

        // Check if the user allows the application to access the location-aware resources.
        if (
            !Permissions.LOCATION.permissions.all { permission ->
                PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
            }
        ) {
            sendError("Location permissions not granted")
            return START_NOT_STICKY
        }

        val obdAddress: String?
        val obdPort: Int
        val isWifi: Boolean

        intent!!.apply {
            obdAddress = getStringExtra("address")
            obdPort = getIntExtra("port", -1)
            isWifi = getBooleanExtra("isWifi", false)
        }

        queue = Volley.newRequestQueue(this)
        mediaPlayer = MediaPlayer.create(this, R.raw.soundreal)

        obdConnection = if (isWifi) {
            when {
                (obdAddress == null || obdAddress == "") && obdPort == -1 -> WifiObdConnection()
                obdPort == -1 -> WifiObdConnection(obdAddress!!)
                (obdAddress == null || obdAddress == "") -> WifiObdConnection(port = obdPort)
                else -> WifiObdConnection(obdAddress, obdPort)
            }
        } else {
            when (obdAddress) {
                null -> BluetoothObdConnection()
                "" -> BluetoothObdConnection()
                else -> BluetoothObdConnection(obdAddress)
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                obdConnection.connect(this@LiveDataService)
                isServiceActive = true

                wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SaferDriving:LiveData")
                wakeLock.acquire(20*60*1000L /*20 minutes*/)

                startLocationAware()

                setupForeground()

                speed = obdConnection.getSpeed().value.toInt()
                rpm = obdConnection.getRPM().value.replace(",", ".").toDouble()
                loadLevel = obdConnection.getEngineLoad().value.replace(",", ".").toDouble()
                fuelType = try {
                    obdConnection.getFuelType().value
                } catch (e: Exception) {
                    intent.getStringExtra("FuelType") ?: "Unknown"
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
                        val loc = Location(
                            task.result.latitude,
                            task.result.longitude
                        )
                        addWeather(loc)
                    }
                }

                // Subscribe to location changes.
                mFusedLocationClient.requestLocationUpdates(
                    locationRequest, mLocationCallback, Looper.getMainLooper()
                )

                startLoops()
            } catch (e: Exception) {
                sendError(e.toString())
                Log.i(TAG, e.stackTraceToString())
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy called")
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        isServiceActive = false

        if (isSpeeding) {
            time?.let { recordSpeeding(it) }
        }

        firebaseManager.addRideInfo(
            startTime = startTime,
            topSpeed = topSpeed,
            topSpeedHighway = topSpeedHighway,
            topSpeedCountryRoad = topSpeedCountryRoad,
            topSpeedCity = topSpeedCity,
            fuelType = fuelType,
            speedingList = speedingSecondsList
        )

        wakeLock.release()

        super.onDestroy()
    }

    private fun setupForeground() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Safer Driving Live Data")
            .setContentText("Live data is being collected...")
            .build()
        startForeground(1, notification)
    }

    private fun addWeather(location: Location) {
        firebaseManager.setWeatherInfo(queue, location)
    }

    private fun startLoops() {
        // Start coroutine updating the road
        CoroutineScope(Dispatchers.IO).launch {
            while (isServiceActive) {
                Log.i(TAG, "Road loop iteration")
                if(location != null) {
                    getRoad(queue, location!!) { road ->
                        roadList.add(road)
                        currentRoad = road
                    }
                    getTrafficInfo(queue, location!!) { trafficInfo ->
                        currentTrafficInfo = trafficInfo
                    }
                }
                // Delay for road API calls
                delay(1000)
            }
        }

        // Start Obd coroutine
        CoroutineScope(Dispatchers.IO).launch {
            while (isServiceActive) {
                Log.i(TAG, "Obd loop iteration")
                obdUpdates()
                // Delay for OBD calls
                delay(1000)
            }
        }
    }

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
                    this@LiveDataService.location = Location(
                        location.latitude, location.longitude
                    )
                }
            }
        }
    }

    private suspend fun obdUpdates(
        delay: Long = 0
    ) {
        if (speed != null && time != null) {
            try {
                rpm = obdConnection.getRPM().value.replace(",", ".").toDouble()
                loadLevel = obdConnection.getEngineLoad().value.replace(",", ".").toDouble()
                val response = obdConnection.getSpeedAndAcceleration(speed!!, time!!, delay)
                response.let { speedAndAcceleration ->
                    if (currentRoad!= null && currentTrafficInfo != null && location != null)
                        firebaseManager.addObdRecording(
                            timeOfRecording = speedAndAcceleration.timeCaptured,
                            speedAndAcceleration = speedAndAcceleration,
                            trafficInfo = currentTrafficInfo!!,
                            road = currentRoad!!,
                            location = location!!,
                            loadLevel = loadLevel!!,
                            rpm = rpm!!,
                            fuelType = fuelType
                        )

                    speed = response.speed.value.toInt()
                    time = response.timeCaptured

                    if (currentRoad != null && currentRoad!!.speedLimit < speed!!) {
                        // If speed increases while speeding, update topLocalSpeed
                        if (speed!! > topLocalSpeed) topLocalSpeed = speed!!
                        if (rpm!! > topLocalRPM) topLocalRPM = rpm!!
                        if (loadLevel!! > topLocalLoadLevel) topLocalLoadLevel = loadLevel!!

                        // If statement to check if it is a new speeding to be recorded
                        if (!isSpeeding) {
                            // If withSound is checked, play audio when the speeding starts
                            if (firebaseManager.getWithSound())
                                mediaPlayer.start()

                            // Save info about when the speeding started
                            localStartTime = speedAndAcceleration.timeCaptured
                            localLocation = location
                            localRoad = currentRoad
                            localTraffic = currentTrafficInfo

                            topLocalSpeed = speed!!
                            topLocalRPM = rpm!!
                            topLocalLoadLevel = loadLevel!!
                        }

                        isSpeeding = true

                    } else if (currentRoad != null && isSpeeding) {
                        recordSpeeding(speedAndAcceleration.timeCaptured)
                    }

                    if (speed!! > topSpeed) topSpeed = speed!!

                    if (currentRoad != null) {
                        when (currentRoad!!.type) {
                            RoadType.CITY ->
                                if (speed!! > topSpeedCity) topSpeedCity = speed!!

                            RoadType.RURAL ->
                                if (speed!! > topSpeedCountryRoad) topSpeedCountryRoad = speed!!

                            RoadType.MOTORWAY ->
                                if (speed!! > topSpeedHighway) topSpeedHighway = speed!!
                        }
                    }
                }
            } catch (e: NonNumericResponseException) {
                // Do nothing, fail sometimes occur
            }
        }
    }

    private fun recordSpeeding(
        endTime: Long
    ) {
        isSpeeding = false

        val secondsSpeeding = (endTime - localStartTime) / 1000

        val speedingRecording = firebaseManager.addSpeedingRecording(
            timeOfRecording = localStartTime,
            location = localLocation!!,
            road = localRoad!!,
            trafficInfo = localTraffic!!,
            topSpeed = topLocalSpeed,
            topRPM = topLocalRPM,
            topEngineLevel = topLocalLoadLevel,
            fuelType = fuelType,
            secondsSpeeding = secondsSpeeding.toInt(),
        )

        speedingSecondsList.add(speedingRecording)

        // Reset local info for next speeding
        localStartTime = 0
        localLocation = null
        localRoad = null
        topLocalSpeed = 0 // reset for next speed
    }

    private fun sendError(message: String) {
        val intent = Intent(ERROR_BROADCAST)
        intent.putExtra(ERROR_EXTRA, message)
        sendBroadcast(intent)
    }
}