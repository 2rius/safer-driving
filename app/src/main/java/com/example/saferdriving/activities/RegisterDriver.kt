package com.example.saferdriving.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.dataClasses.Driver
import com.example.saferdriving.databinding.ActivityRegisterDriverBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterDriver : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterDriverBinding
    private lateinit var database: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database =
            Firebase.database("https://safer-driving-default-rtdb.europe-west1.firebasedatabase.app/").reference.child("drivers")

        binding.registerNewRide.setOnClickListener{
            val age = binding.editTextAge.text.toString().toIntOrNull()
            val drivingExperience = binding.editTextDrivingExperience.text.toString().toIntOrNull()
            val residence = binding.editTextResidence.text.toString()
            val job = binding.editTextJob.text.toString()

            // Retrieve air temperature and air pressure from environment sensors
            val pressure = intent.getIntExtra("pressure", 0)
            val temperature = intent.getIntExtra("temperature", 0)
            val windSpeed = intent.getIntExtra("windSpeed", 0)
            val weatherDescription = intent.getStringExtra("weatherDescription")


            // Create a new Driver object
            val newDriver = Driver(
                age = age,
                drivingExperience = drivingExperience,
                residence = residence,
                job = job,
                airPressure = pressure,
                airTemperature = temperature,
                windSpeed = windSpeed,
                weatherDescription = weatherDescription
                // Add other properties as needed
            )

            // Push the new driver data to the database
            database.push().setValue(newDriver)
                .addOnSuccessListener {
                    Toast.makeText(this, "Driver added successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add driver: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

    }
}