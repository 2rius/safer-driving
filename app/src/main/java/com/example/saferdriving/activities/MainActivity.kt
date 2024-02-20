package com.example.saferdriving.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.example.saferdriving.databinding.ActivityMainBinding
import com.example.saferdriving.services.Geolocation
import com.example.saferdriving.services.Geolocation.Companion.TAG
import com.example.saferdriving.utilities.LOCATION_PERMISSIONS
import com.example.saferdriving.utilities.getRequestPermission

class MainActivity : AppCompatActivity() {

    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private lateinit var binding: ActivityMainBinding
    // A reference to the service used to get location updates.
    private var mService: Geolocation? = null
    // Tracks the bound state of the service.
    private var mBound = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Check which permissions is needed to ask to the user.
        val getPermission = getRequestPermission(LOCATION_PERMISSIONS, onGranted = {subscribeToService()})
        // Set click listener for the button
        binding.registerNewRide.setOnClickListener {
            startRegisterDriverActivity()
        }
        binding.refreshButton.setOnClickListener{
            startListeningGeo()
            getPermission()
            Log.i(TAG, "hello")
        }


    }

    private fun startRegisterDriverActivity() {
        val intent = Intent(
            this,
            RegisterDriver::class.java
        )
        startActivity(intent)
        finish()
    }

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



}