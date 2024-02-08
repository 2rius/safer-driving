package com.example.saferdriving.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityMainBinding
import com.example.saferdriving.enums.ObdTypes
import com.github.eltonvs.obd.command.engine.SpeedCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        GlobalScope.launch(Dispatchers.IO) {
            try {
                val obdConnection =
                    ObdTypes.BLUETOOTH.connect(this@MainActivity, "11:22:33:44:55:66")

                val response = obdConnection.run(SpeedCommand()).value
                launch(Dispatchers.Main) {
                    binding.outputText.text = getString(R.string.speed_result, response)
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