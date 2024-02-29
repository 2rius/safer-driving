package com.example.saferdriving.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.dataClasses.BasicInfo
import com.example.saferdriving.dataClasses.RideInfo
import com.example.saferdriving.databinding.ActivityRegisterDriverBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class RegisterDriver : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterDriverBinding
    private var database: DatabaseReference = Firebase.database("https://safer-driving-default-rtdb.europe-west1.firebasedatabase.app/").reference.child("data_without_sound").child("drivers")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.checkboxSound.setOnClickListener {
            if (binding.checkboxSound.isChecked){
                database =
                    Firebase.database("https://safer-driving-default-rtdb.europe-west1.firebasedatabase.app/").reference.child("data_with_sound").child("drivers")
            } else{
                database = Firebase.database("https://safer-driving-default-rtdb.europe-west1.firebasedatabase.app/").reference.child("data_without_sound").child("drivers")
            }
        }

        binding.goToRecord.setOnClickListener{
            val age = binding.editTextAge.text.toString().toIntOrNull()
            val drivingExperience = binding.editTextDrivingExperience.text.toString().toIntOrNull()
            val residence = binding.editTextResidence.text.toString()
            val job = binding.editTextJob.text.toString()

            // Create a new Driver object
            val newBasicInfo = BasicInfo(
                age = age,
                drivingExperience = drivingExperience,
                residence = residence,
                job = job,
                // Add other properties as needed
            )

            // Push the new driver data to the database
            // Push the new driver data to the database and retrieve the auto-generated ID
            val newDriverRef = database.push()
            val driverId = newDriverRef.key // Get the auto-generated ID

            // Set the value of the newly created driver using the retrieved ID
            newDriverRef.setValue(newBasicInfo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Driver added successfully!", Toast.LENGTH_SHORT).show()
                    if (driverId != null) {
                        startLiveDataDisplayActivity(binding.checkboxSound.isChecked, driverId)
                    }
                    // Now you can use driverId to reference this specific driver if needed
                    //val specificDriverRef = database.child(driverId!!)
                    // Use specificDriverRef to perform operations on this specific driver
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add driver: ${it.message}", Toast.LENGTH_SHORT).show()
                }


        }

    }

    private fun startLiveDataDisplayActivity(withSound:Boolean, driverId: String) {
        val intent = Intent(
            this,
            LiveDataDisplay::class.java
        )
        intent.putExtra("withSound", withSound)
        intent.putExtra("driverID", driverId)
        startActivity(intent)
        finish()
    }
}