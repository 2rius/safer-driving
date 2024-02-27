package com.example.saferdriving.activities


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.saferdriving.BuildConfig
import com.example.saferdriving.classes.ObdConnection
import com.example.saferdriving.databinding.ActivityMainBinding
import com.example.saferdriving.services.LiveDataService
import com.example.saferdriving.services.LiveDataService.Companion.TAG
import com.example.saferdriving.utilities.BLUETOOTH_PERMISSIONS
import com.example.saferdriving.utilities.LOCATION_PERMISSIONS
import com.example.saferdriving.utilities.getRequestPermission
import com.example.saferdriving.utilities.getRoad
import com.example.saferdriving.utilities.showConnectionTypeDialog
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject


class MainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMainBinding

    private lateinit var queue: RequestQueue

    private lateinit var obdConnection : ObdConnection

    // weather url to get JSON
    var weather_url1 = ""

    // api id for url
    var api_id1 = BuildConfig.WEATHER_API_KEY

    //Environment sensor data
    private var pressureInMilliBars : Int? = null  //mbar
    private var temperatureInCelsius : Int? = null   //celsius
    private var windspeedInMS : Int? = null //m/s
    private var weatherDiscription : String = ""

    // OSM url for API
    var osm_url = ""

    // A reference to the service used to get location updates.
    private var mService: LiveDataService? = null
    // Tracks the bound state of the service.
    private var mBound = false

    private var latitude : Double = 0.0
    private var longitude : Double = 0.0
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        queue = Volley.newRequestQueue(this)
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
                    connection.connect(this@MainActivity)
                    obdConnection = connection
                    startLiveDataService()
                } catch (e: Exception) {
                    // Handle error, usually problem with connecting to OBD device }
                }
            }
        }

        // Set click listener for the button
        binding.registerNewRide.setOnClickListener {
            startRegisterDriverActivity()
        }
        binding.refreshButton.setOnClickListener{

            getPermission()
            try {
                getRoad(queue, latitude, longitude) { road ->
                    Log.i(TAG, "IT IS IN DA TRY")
                    Toast.makeText(
                        this,
                        "roadname: " + road.name + ", roadtype: " + road.type + ", speedlimit: " + road.speedLimit,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.i(TAG, "IT CAUGHT DA EXCEPTIONA")
            }
        }
        binding.weatherInfoButton.setOnClickListener {
            if (latitude != 0.0 || longitude != 0.0)
                getWeatherInfo(queue)
        }


    }
    private fun startRegisterDriverActivity() {
        val intent = Intent(
            this,
            RegisterDriver::class.java
        )
        intent.putExtra("pressure", pressureInMilliBars)
        intent.putExtra("temperature", temperatureInCelsius)
        intent.putExtra("windSpeed", windspeedInMS)
        intent.putExtra("weatherDescription", weatherDiscription)
        startActivity(intent)
        finish()
    }
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

    private fun getWeatherInfo(queue: RequestQueue) {
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