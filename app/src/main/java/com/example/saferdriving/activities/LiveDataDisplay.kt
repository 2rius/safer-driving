package com.example.saferdriving.activities

import android.content.*
import android.media.AudioManager
import android.media.MediaPlayer
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
import com.example.saferdriving.services.TimerService
import com.example.saferdriving.services.Geolocation
import com.example.saferdriving.services.Geolocation.Companion.TAG
import com.example.saferdriving.utilities.LOCATION_PERMISSIONS
import com.example.saferdriving.utilities.getRequestPermission
import kotlin.math.roundToInt

class LiveDataDisplay : AppCompatActivity() {

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityLiveDataDisplayBinding
    // A reference to the service used to get location updates.
    private var mService: Geolocation? = null
    // Tracks the bound state of the service.
    private var mBound = false
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button

    //timer variables
    private var timerStarted = false
    private lateinit var serviceIntent: Intent
    private var time = 0.0

    var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val getPermission = getRequestPermission(LOCATION_PERMISSIONS, onGranted = {subscribeToService()})

        binding = ActivityLiveDataDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshButton.setOnClickListener{
            startListeningGeo()
            getPermission()
            Log.i(TAG, "hello")
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.sound)


        binding.startSound.setOnClickListener {
            playAudio()
        }
        binding.pauseSound.setOnClickListener {
            pauseAudio()
        }

        binding.startStopButton.setOnClickListener { startStopTimer() }
        binding.resetButton.setOnClickListener { resetTimer() }

        serviceIntent = Intent(applicationContext, TimerService::class.java)
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

    private fun startListeningGeo(){
        Intent(this, Geolocation::class.java).also { intent ->
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    // Monitors the state of the connection to the service.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: Geolocation.LocalBinder = service as Geolocation.LocalBinder
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

    private fun permissionsToRequest(permissions: ArrayList<String>): ArrayList<String> {
        val result: ArrayList<String> = ArrayList()
        for (permission in permissions)
            if (PermissionChecker.checkSelfPermission(
                    this,
                    permission
                ) != PermissionChecker.PERMISSION_GRANTED)
                result.add(permission)
        return result
    }

    private fun updateUI(lat : Double, long: Double, address: String)  {
        binding.apply {
            longitudeText.text = "Longitude: $long"
            latitudeText.text = "Longitude: $lat"
            //addressTextField?.editText?.setText(address)
        }
    }

    private fun subscribeToService(){
        Log.i(TAG, "service works???")
        mService?.subscribeToLocationUpdates(
            {
                    lastLocation ->
                // Change the color of the default marker to blue
                updateUI(lastLocation.latitude, lastLocation.longitude , "")
            },
            {
                    lat, long, address ->
                updateUI(lat, long , address)
            }


        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
    }




}