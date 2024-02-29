package com.example.saferdriving.activities

import android.content.*

import android.media.MediaPlayer
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import android.text.format.Time
import com.example.saferdriving.databinding.ActivityLiveDataDisplayBinding
import androidx.core.content.PermissionChecker
import com.example.saferdriving.BuildConfig
import com.example.saferdriving.classes.ObdConnection
import com.example.saferdriving.services.TimerService
import com.example.saferdriving.services.LiveDataService
import com.example.saferdriving.services.LiveDataService.Companion.TAG
import com.example.saferdriving.utilities.BLUETOOTH_PERMISSIONS
import com.example.saferdriving.utilities.LOCATION_PERMISSIONS
import com.example.saferdriving.utilities.getRequestPermission
import com.example.saferdriving.utilities.showConnectionTypeDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

class LiveDataDisplay : AppCompatActivity() {

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityLiveDataDisplayBinding
    // A reference to the service used to get location updates.
    // A reference to the service used to get location updates.
    private var mService: LiveDataService? = null
    // Tracks the bound state of the service.
    private var mBound = false

    private lateinit var queue: RequestQueue
    // weather url to get JSON
    var weather_url1 = ""

    // api id for url
    var api_id1 = BuildConfig.WEATHER_API_KEY
    private var latitude: Double? = null
    private var longitude: Double? = null

    //Environment sensor data
    private var pressureInMilliBars : Int? = null  //mbar
    private var temperatureInCelsius : Int? = null   //celsius
    private var windspeedInMS : Int? = null //m/s
    private var weatherDiscription : String = ""

    //OBD
    private lateinit var obdConnection : ObdConnection

    // OSM url for API
    var osm_url = ""

    //timer variables
    private var timerStarted = false
    private lateinit var serviceIntent: Intent
    private var time = 0.0

    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = Volley.newRequestQueue(this)
        binding = ActivityLiveDataDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check which permissions is needed to ask to the user.
        val getPermission = getRequestPermission(LOCATION_PERMISSIONS, onGranted = {subscribeToService()})
        val requestPermission: (() -> Unit) -> () -> Unit = { onDenied -> getRequestPermission(BLUETOOTH_PERMISSIONS, onDenied = onDenied) }

        val futureConnection = showConnectionTypeDialog(this, requestPermission)

        // futureConnection.thenAccept will run concurrently, so code beneath this will run at the same time
        futureConnection.thenAccept {
                connection ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    connection.connect(this@LiveDataDisplay)
                    obdConnection = connection
                    getPermission()
                    startLiveDataService()
                } catch (e: Exception) {
                    e.message?.let { Log.d(TAG, it) }
                    // Handle error, usually problem with connecting to OBD device }
                }
            }
        }

        binding.refreshButton.setOnClickListener{
            Log.i(TAG, "hello")
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)


        binding.startSound.setOnClickListener {
            playAudio()
        }
        binding.pauseSound.setOnClickListener {
            pauseAudio()
        }

        binding.resetButton.setOnClickListener { resetTimer() }

        serviceIntent = Intent(applicationContext, TimerService::class.java)
        startStopTimer()
        binding.startStopButton.setOnClickListener { startStopTimer() }
        registerReceiver(updateTime, IntentFilter(TimerService.TIMER_UPDATED))
    }

    private fun playAudio() {
        if (!mediaPlayer?.isPlaying!!) {
            mediaPlayer?.start()
            Toast.makeText(this, "Sound started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Audio is already playing", Toast.LENGTH_SHORT).show()
        }
    }
    private fun pauseAudio() {
        if(mediaPlayer!!.isPlaying){
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
        }else{
            Toast.makeText(this, "Audio not playing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetTimer()
    {
        stopTimer()
        time = 0.0
        binding.timeTV.text = getTimeStringFromDouble(time)
    }

    private fun startStopTimer()
    {
        if(timerStarted)
            stopTimer()
        else
            startTimer()
    }

    private fun startTimer()
    {
        serviceIntent.putExtra(TimerService.TIME_EXTRA, time)
        startService(serviceIntent)
        binding.startStopButton.text = "Stop"
        timerStarted = true
        Log.d(TAG, "what is the time? $time")
    }

    private fun stopTimer()
    {
        stopService(serviceIntent)
        binding.startStopButton.text = "Start"
        timerStarted = false
    }

    private val updateTime: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            time = intent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0)
            binding.timeTV.text = getTimeStringFromDouble(time)
            Log.d(TAG, "what is the time? $time")
        }
    }

    private fun getTimeStringFromDouble(time: Double): String
    {
        val resultInt = time.roundToInt()
        val hours = resultInt % 86400 / 3600
        val minutes = resultInt % 86400 % 3600 / 60
        val seconds = resultInt % 86400 % 3600 % 60

        return makeTimeString(hours, minutes, seconds)
    }

    private fun makeTimeString(hour: Int, min: Int, sec: Int): String = String.format("%02d:%02d:%02d", hour, min, sec)

    private fun startLiveDataService(){
        Intent(this, LiveDataService::class.java).also { intent ->
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    // Monitors the state of the connection to the service.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: LiveDataService.LocalBinder = service as LiveDataService.LocalBinder
            mService = binder.getService()
            mBound = true
            //subscribeToService()
            Log.i(TAG, "Service works")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService?.unsubscribeToLocationUpdates()
            mService = null
            mBound = false
            Log.i(TAG, "Service does not work")
        }
    }
    private fun updateLatLong(lat : Double, long: Double)  {
        latitude = lat
        longitude = long
    }
    private fun subscribeToService(){
        Log.i(TAG, "service works???")
        mService?.subscribeToLocationUpdates(obdConnection, queue, withSound = false,
            { lat, long ->
                updateLatLong(lat, long)
            },{
                    lat, long ->
                weather_url1 = "https://api.weatherbit.io/v2.0/current?lat=$lat&lon=$long&key=$api_id1"
            })
    }
    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
    }

    fun getWeatherInfo(queue: RequestQueue) {
        val url: String = weather_url1
        Log.e("lat", url)

        // Request a string response
        // from the provided URL.
        val stringReq = StringRequest(Request.Method.GET, url,
            { response ->
                Log.e("lat", response.toString())

                // get the JSON object
                val obj = JSONObject(response)

                // get the Array from obj of name - "data"
                val arr = obj.getJSONArray("data")
                Log.e("lat obj1", arr.toString())

                // get the JSON object from the
                // array at index position 0
                val obj2 = arr.getJSONObject(0)
                Log.e("lat obj2", obj2.toString())

                // set the temperature and the city
                // name using getString() function
                val temperature = obj2.getString("temp")
                val pressure = obj2.getString("pres")
                val windSpeed = obj2.getString("wind_spd")
                val weatherDescriptionFromJSON = obj2.getJSONObject("weather").getString("description")

                // Update the temperatureInCelsius variable
                temperatureInCelsius = temperature.toFloatOrNull()?.toInt()
                pressureInMilliBars = pressure.toFloatOrNull()?.toInt()
                windspeedInMS = windSpeed.toFloatOrNull()?.toInt()
                weatherDiscription = weatherDescriptionFromJSON

            },
            // In case of any error
            { Toast.makeText(this, "Error getting temperature!", Toast.LENGTH_SHORT).show() })
        queue.add(stringReq)
    }




}