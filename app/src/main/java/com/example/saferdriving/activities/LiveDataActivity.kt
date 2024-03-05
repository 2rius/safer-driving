package com.example.saferdriving.activities

import android.content.*

import android.media.MediaPlayer
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import android.content.IntentFilter
import android.location.Location
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityLiveDataDisplayBinding
import com.example.saferdriving.classes.ObdConnection
import com.example.saferdriving.enums.Permissions.*
import com.example.saferdriving.services.TimerService
import com.example.saferdriving.services.LiveDataService
import com.example.saferdriving.singletons.FirebaseManager
import com.example.saferdriving.utils.getRequestPermission
import com.example.saferdriving.utils.showConnectionTypeDialog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LiveDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveDataDisplayBinding

    private val firebaseManager = FirebaseManager.getInstance()

    private lateinit var wakeLock: WakeLock

    private var mService: LiveDataService? = null
    // Tracks the bound state of the service.
    private var mBound = false

    private lateinit var queue: RequestQueue

    private var startingLocation: Location? = null

    //OBD
    private lateinit var obdConnection : ObdConnection

    //timer variables
    private var timerStarted = false
    private lateinit var serviceIntent: Intent
    private var time = 0.0

    private var mediaPlayer: MediaPlayer? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = Volley.newRequestQueue(this)
        binding = ActivityLiveDataDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check which permissions is needed to ask to the user.
        val requestLocationPermission = getRequestPermission(LOCATION.permissions, onGranted = {subscribeToService()})
        val requestBluetoothPermission: (() -> Unit) -> () -> Unit = { onDenied -> getRequestPermission(BLUETOOTH.permissions, onDenied = onDenied) }

        val futureConnection = showConnectionTypeDialog(this, requestBluetoothPermission)

        val mgr = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SaferDriving:LiveData")
        wakeLock.acquire(20*60*1000L /*20 minutes*/)

        // futureConnection.thenAccept will run concurrently, so code beneath this will run at the same time
        futureConnection.thenAccept {
                connection ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    connection.connect(this@LiveDataActivity)
                    obdConnection = connection
                    requestLocationPermission()
                    startLiveDataService()

                    serviceIntent = Intent(applicationContext, TimerService::class.java)
                    startStopTimer()
                } catch (e: Exception) {
                    // Handle error, usually problem with connecting to OBD device }
                }
            }
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)


        binding.startSound.setOnClickListener {
            playAudio()
        }

        binding.endRecording.setOnClickListener {
            startMainActivity()
        }

        binding.resetButton.setOnClickListener { resetTimer() }

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
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
        }
    }
    private fun updateLocation(location: Location)  {
        startingLocation = location
        firebaseManager.addWeatherInfo(queue, location)
    }
    private fun subscribeToService(){
        mService?.subscribeToLiveData(obdConnection, mediaPlayer!!, queue) { location ->
            updateLocation(location)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        wakeLock.release()
        if (mBound) {
            unbindService(mServiceConnection)
            mService?.unsubscribeToLiveData()
            resetTimer()
            mBound = false
        }
    }

    private fun startMainActivity() {
        val intent = Intent(
            this,
            MainActivity::class.java
        )
        startActivity(intent)
        finish()
    }
}