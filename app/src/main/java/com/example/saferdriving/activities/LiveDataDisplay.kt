package com.example.saferdriving.activities

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.example.saferdriving.R
import com.example.saferdriving.databinding.ActivityLiveDataDisplayBinding
import java.io.IOException

class LiveDataDisplay : AppCompatActivity() {

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityLiveDataDisplayBinding
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button

    var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLiveDataDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startSound.setOnClickListener{
            playAudio()
        }
        binding.pauseSound.setOnClickListener{
            pauseAudio()
        }
    }

    private fun playAudio() {
        var audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)


        try {
            mediaPlayer!!.setDataSource(audioUrl)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
        } catch (e: IOException){
            e.printStackTrace()
        }

        Toast.makeText(this, "Sound started", Toast.LENGTH_SHORT).show()
    }
    private fun pauseAudio() {
        if(mediaPlayer!!.isPlaying){
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
        }else{
            Toast.makeText(this, "Audio not playing", Toast.LENGTH_SHORT).show()
        }
    }



}