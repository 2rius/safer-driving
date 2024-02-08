package com.example.saferdriving.activities

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityMainBinding
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var obdIpAddress = "192.168.0.10"
    private var obdPort = 3500

    private var obdBluetoothAddress = "11:22:33:44:55:66"

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val socket = Socket(InetAddress.getByName(obdIpAddress), obdPort)

                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                if (inputStream != null && outputStream != null) {
                    val obdConnection = ObdDeviceConnection(inputStream, outputStream)

                    launch(Dispatchers.Main) {
                        binding.outputText.text = "Speed: is ..."
                    }

                    val response = obdConnection.run(SpeedCommand())
                    launch(Dispatchers.Main) {
                        binding.outputText.text = "Speed: ${response.value}"
                    }
                } else {
                    throw IOException("Input or output stream is null")
                }
            } catch (e: IOException) {
                e.printStackTrace() // Print the exception trace to identify the issue
                launch(Dispatchers.Main) {
                    binding.outputText.text = "Error: ${e.message}"
                }
            }
        }
    }
}