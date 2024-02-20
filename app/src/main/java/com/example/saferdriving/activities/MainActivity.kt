package com.example.saferdriving.activities


import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.content.ServiceConnection
import android.content.ComponentName
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

class MainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMainBinding

    //Environment sensor data
    private lateinit var sensorManager: SensorManager

    private var pressureInMilliBars : Int = 0  //mbar
    private var temperatureInCelsius : Int? = null      //celsius


    private val pressureListener: SensorEventListener = object : SensorEventListener {

        /**
         * Called when there is a new sensor event. Note that "on changed" is somewhat of a
         * misnomer, as this will also be called if we have a new reading from a sensor with the
         * exact same sensor values (but a newer timestamp).
         *
         * The application doesn't own the `android.hardware.SensorEvent` object passed as a
         * parameter and therefore cannot hold on to it. The object may be part of an internal pool
         * and may be reused by the framework.
         *
         * @param event The SensorEvent instance.
         */
        override fun onSensorChanged(event: SensorEvent) {
            pressureInMilliBars = event.values[0].toInt()
        }

        /**
         * Called when the accuracy of the registered sensor has changed. Unlike
         * `onSensorChanged()`, this is only called when this accuracy value changes.
         *
         * @param sensor An instance of the `Sensor` class.
         * @param accuracy The new accuracy of this sensor, one of `SensorManager.SENSOR_STATUS_`
         */
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }

    }

    private val ambientTemperatureListener: SensorEventListener = object : SensorEventListener {

        /**
         * Called when there is a new sensor event. Note that "on changed" is somewhat of a
         * misnomer, as this will also be called if we have a new reading from a sensor with the
         * exact same sensor values (but a newer timestamp).
         *
         * The application doesn't own the `android.hardware.SensorEvent` object passed as a
         * parameter and therefore cannot hold on to it. The object may be part of an internal pool
         * and may be reused by the framework.
         *
         * @param event The SensorEvent instance.
         */
        override fun onSensorChanged(event: SensorEvent) {
            temperatureInCelsius = event.values[0].toInt()
        }

        /**
         * Called when the accuracy of the registered sensor has changed. Unlike
         * `onSensorChanged()`, this is only called when this accuracy value changes.
         *
         * @param sensor An instance of the `Sensor` class.
         * @param accuracy The new accuracy of this sensor, one of `SensorManager.SENSOR_STATUS_`
         */
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }

    }

    // A reference to the service used to get location updates.
    private var mService: Geolocation? = null
    // Tracks the bound state of the service.
    private var mBound = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Environment sensor data
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

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
    override fun onResume() {
        // Register a listener for the sensor.
        super.onResume()

        // Register the pressure listener if the pressure sensor is available
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (pressure != null)
            sensorManager.registerListener(pressureListener,
                pressure, SensorManager.SENSOR_DELAY_NORMAL)

        // Get an instance of the ambient temperature sensor and register the sensor listener.
        val ambientTemperature = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (ambientTemperature != null)
            sensorManager.registerListener(ambientTemperatureListener,
                ambientTemperature, SensorManager.SENSOR_DELAY_NORMAL)

    }
    override fun onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause()
        sensorManager.unregisterListener(pressureListener)
        sensorManager.unregisterListener(ambientTemperatureListener)
    }
    private fun startRegisterDriverActivity() {
        val intent = Intent(
            this,
            RegisterDriver::class.java
        )
        intent.putExtra("pressure", pressureInMilliBars)
        //intent.putExtra("temperature", temperatureInCelsius)
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