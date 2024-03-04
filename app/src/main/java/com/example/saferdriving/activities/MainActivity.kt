package com.example.saferdriving.activities


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(){
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.start.setOnClickListener {
            startRegisterDriverActivity()
        }
    }
    private fun startRegisterDriverActivity() {
        val intent = Intent(
            this,
            RegisterDriverActivity::class.java
        )
        startActivity(intent)
        finish()
    }

}