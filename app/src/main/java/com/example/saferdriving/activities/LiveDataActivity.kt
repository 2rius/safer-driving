package com.example.saferdriving.activities

import android.content.*

import android.os.Bundle
import android.content.IntentFilter
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityLiveDataDisplayBinding
import com.example.saferdriving.dataclasses.ObdConnectionInfo
import com.example.saferdriving.enums.Permissions
import com.example.saferdriving.enums.Permissions.*
import com.example.saferdriving.services.LiveDataService
import com.example.saferdriving.services.TimerService
import com.example.saferdriving.utils.getRequestPermission
import com.example.saferdriving.utils.showConnectionTypeDialog
import kotlin.math.roundToInt

class LiveDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLiveDataDisplayBinding

    // timer service variables
    private var timerStarted = false
    private lateinit var timerServiceIntent: Intent
    private var time = 0.0

    // livedata service variables
    private lateinit var liveDataServiceIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveDataDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        timerServiceIntent = Intent(applicationContext, TimerService::class.java)
        liveDataServiceIntent = Intent(this, LiveDataService::class.java)

        liveDataServiceIntent.putExtra("FuelType", intent.getStringExtra("FuelType"))

        if (savedInstanceState == null) {
            setupLiveDataService()
        }

        binding.endRecording.setOnClickListener {
            startMainActivity()
        }

        registerReceiver(updateTime, IntentFilter(TimerService.TIMER_UPDATED))
        registerReceiver(livedataServiceError, IntentFilter(LiveDataService.ERROR_BROADCAST))
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
        timerServiceIntent.putExtra(TimerService.TIME_EXTRA, time)
        startService(timerServiceIntent)
        timerStarted = true
    }

    private fun stopTimer()
    {
        stopService(timerServiceIntent)
        timerStarted = false
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


    private fun setupLiveDataService() {
        getRequestPermission(LOCATION.permissions)()

        val requestBluetoothPermission: (() -> Unit) -> () -> Unit =
            { onDenied -> getRequestPermission(BLUETOOTH.permissions, onDenied = onDenied) }

        val futureConnection = showConnectionTypeDialog(this, requestBluetoothPermission)

        // futureConnection.thenAccept will run concurrently, so code beneath this will run at the same time
        futureConnection.thenAccept { connectionInfo ->
            if (
                LOCATION.permissions.all { permission ->
                    PermissionChecker.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_GRANTED
                }
            ) {
                startLiveDataService(connectionInfo)
                startStopTimer()
            } else {
                setErrorMessage("Location permissions not granted")
            }
        }
    }

    private fun startLiveDataService(obdConnectionInfo: ObdConnectionInfo){
        liveDataServiceIntent.apply {
            putExtra("address", obdConnectionInfo.address)
            putExtra("port", obdConnectionInfo.port)
            putExtra("isWifi", obdConnectionInfo.isWifi)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startService(liveDataServiceIntent)
        }
    }


    private fun startMainActivity() {
        resetTimer()
        stopService(liveDataServiceIntent)
        val intent = Intent(
            this,
            MainActivity::class.java
        )
        startActivity(intent)
        finish()
    }

    private fun setErrorMessage(message: String) {
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = getString(R.string.error, message)
    }


    private val updateTime: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            time = intent.getDoubleExtra(TimerService.TIME_EXTRA, 0.0)
            binding.timeTV.text = getTimeStringFromDouble(time)
        }
    }

    private val livedataServiceError: BroadcastReceiver = object : BroadcastReceiver()
    {
        override fun onReceive(context: Context, intent: Intent)
        {
            val errorMessage = intent.getStringExtra(LiveDataService.ERROR_EXTRA)
            if (errorMessage != null) {
                setErrorMessage(errorMessage)
            }
        }
    }
}