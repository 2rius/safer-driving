package com.example.saferdriving.services
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.saferdriving.services.LiveDataService.Companion.TAG
import java.util.*

class TimerService : Service()
{
    override fun onBind(p0: Intent?): IBinder? = null

    private val timer = Timer()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
    {

        val time = intent.getDoubleExtra(TIME_EXTRA, 0.0)
        Log.d(TAG, "Time value received by Timer service: $time") // Add this line
        timer.scheduleAtFixedRate(TimeTask(time), 0, 1000)
        return START_NOT_STICKY
    }


    override fun onDestroy()
    {
        timer?.cancel()
        super.onDestroy()
    }

    private inner class TimeTask(private var time: Double) : TimerTask()
    {
        override fun run()
        {
            val intent = Intent(TIMER_UPDATED)
            time++
            intent.putExtra(TIME_EXTRA, time)
            sendBroadcast(intent)
            Log.d(TAG, "Is time increamented? $time")
        }
    }

    companion object
    {
        const val TIMER_UPDATED = "timerUpdated"
        const val TIME_EXTRA = "timeExtra"
    }

}