package com.example.saferdriving.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.databinding.ActivityRegisterDriverBinding
import com.example.saferdriving.singletons.FirebaseManager

class RegisterDriverActivity : AppCompatActivity() {
    private val firebaseManager = FirebaseManager.getInstance()

    private lateinit var binding: ActivityRegisterDriverBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            goToRecord.setOnClickListener{
                if (editTextAge.text.isNotEmpty()
                    && editTextDrivingExperience.text.isNotEmpty()
                    && editTextResidence.text.isNotEmpty()
                    && editTextJob.text.isNotEmpty()
                    ) {
                    val age = editTextAge.text.toString().toInt()
                    val drivingExperience = editTextDrivingExperience.text.toString().toInt()
                    val residence = editTextResidence.text.toString()
                    val job = editTextJob.text.toString()

                    firebaseManager.addDriver()
                    firebaseManager.setWithSound(checkboxSound.isChecked)

                    firebaseManager.setBasicInfo(
                        age,
                        drivingExperience,
                        residence,
                        job
                    )

                    startLiveDataDisplayActivity()
                }
            }
        }
    }

    private fun startLiveDataDisplayActivity() {
        val fuelType = binding.editTextFuelType.text.toString()
        val intent = Intent(
            this,
            LiveDataActivity::class.java
        )
        intent.putExtra("FuelType", fuelType)
        startActivity(intent)
        finish()
    }
}