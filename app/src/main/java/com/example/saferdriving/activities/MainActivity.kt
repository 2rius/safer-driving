package com.example.saferdriving.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityMainBinding
import com.example.saferdriving.obd.SpeedCommand
import com.example.saferdriving.utilities.BLUETOOTH_PERMISSIONS
import com.example.saferdriving.utilities.getRequestPermission
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

        val futureConnection = showConnectionTypeDialog(this, requestPermission)

        futureConnection.thenAccept { connection ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val obdDeviceConnection = connection.connect(this@MainActivity)

                    while (true) {
                        val response = obdDeviceConnection.run(SpeedCommand(), useCache = false, delayTime = 500)
                        launch(Dispatchers.Main) {
                            binding.outputText.text = getString(R.string.speed_result, response.formattedValue)
                        }
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        binding.outputText.text = getString(R.string.error, e.message)
                    }
                }
            }
        }
    }
}