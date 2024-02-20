package com.example.saferdriving.activities

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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}