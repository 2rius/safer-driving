package com.example.saferdriving.dataclasses

data class ObdRecording(
    var speed: Int? = null,
    var acceleration: Double? = null,
    var fuel: Double? = null
)
