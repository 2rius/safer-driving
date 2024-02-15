package com.example.saferdriving.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityMainBinding
import com.example.saferdriving.utilities.BLUETOOTH_PERMISSIONS
import com.example.saferdriving.utilities.getRequestPermission
import kotlinx.coroutines.DelicateCoroutinesApi
import showConnectionTypeDialog

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestPermission: (() -> Unit, () -> Unit) -> () -> Unit = { onGranted, onDenied -> getRequestPermission(BLUETOOTH_PERMISSIONS, onGranted, onDenied) }

        showConnectionTypeDialog(this, requestPermission)

        /*GlobalScope.launch(Dispatchers.IO) {
            try {
                // val obdConnection = BluetoothObdConnection()

                val obdConnection = WifiObdConnection()

                val obdDeviceConnection = obdConnection.connect(this@MainActivity)

                var tries = 1

                while (true) {
                    val response = obdDeviceConnection.run(SpeedCommand(), useCache = false, delayTime = 500)
                    launch(Dispatchers.Main) {
                        binding.outputText.text = getString(R.string.speed_result, "try: " + tries++ + ", response: " + response.formattedValue)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace() // Print the exception trace to identify the issue
                launch(Dispatchers.Main) {
                    binding.outputText.text = getString(R.string.speed_result, e.message)
                }
            }
        }*/
    }
}