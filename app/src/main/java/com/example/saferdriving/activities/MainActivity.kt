package com.example.saferdriving.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityMainBinding
import com.example.saferdriving.enums.ObdTypes
import com.example.saferdriving.obd.SpeedCommand
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                // val obdConnection =
                //     ObdTypes.BLUETOOTH.connect(this@MainActivity, "00:1D:A5:05:74:E0")

                val obdConnection =
                        ObdTypes.WIFI.connect(this@MainActivity, "192.168.0.112", 35000)

                var tries = 1

                while (true) {
                    //Thread.sleep(1000)
                    val response = obdConnection.run(SpeedCommand(), useCache = false, delayTime = 500)
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
        }
    }
}