package com.example.saferdriving.obd

import android.content.Context
import com.github.eltonvs.obd.connection.ObdDeviceConnection

interface ObdConnection {
    suspend fun connect(context: Context): ObdDeviceConnection
}