package com.example.saferdriving.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.saferdriving.R
import com.example.saferdriving.dataClasses.Driver
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.example.saferdriving.databinding.ActivityRegisterDriverBinding
import com.google.firebase.ktx.Firebase

class RegisterDriver : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterDriverBinding
    private lateinit var database: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_driver)

        database =
            Firebase.database("https://safer-driving-default-rtdb.europe-west1.firebasedatabase.app/").reference

        binding.registerNewRide.setOnClickListener{
            // Retrieve data from EditText fields
            val age = binding.editTextAge.text.toString().toInt()
            val drivingExperience = binding.editTextDrivingExperience.text.toString().toInt()
            val residence = binding.editTextResidence.text.toString()
            val job = binding.editTextJob.text.toString()

            // Create a Driver object
            val driver = Driver(
                age = age,
                drivingExperience = drivingExperience,
                residence = residence,
                job = job
                // Add other properties as needed
            )

            // Push the Driver object to Firebase under "drivers" node
            database.child("drivers").push().setValue(driver)
                .addOnSuccessListener {
                    Toast.makeText(this, "Driver added successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add driver: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

    }
}