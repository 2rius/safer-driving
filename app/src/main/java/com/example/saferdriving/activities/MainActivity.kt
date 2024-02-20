package com.example.saferdriving.activities

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.hardware.SensorEventListener
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityMainBinding
import com.example.saferdriving.obd.Acceleration
import com.example.saferdriving.utilities.BLUETOOTH_PERMISSIONS
import com.example.saferdriving.utilities.getRequestPermission
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.example.saferdriving.utilities.showConnectionTypeDialog
import com.github.eltonvs.obd.command.ObdResponse

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Environment sensor data
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Set click listener for the button
        binding.registerNewRide.setOnClickListener {
            startRegisterDriverActivity()
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
}