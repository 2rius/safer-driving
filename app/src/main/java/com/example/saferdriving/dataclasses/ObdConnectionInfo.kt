package com.example.saferdriving.dataclasses

data class ObdConnectionInfo(
    val address: String? = null,
    val port: Int? = null,
    val isWifi: Boolean
)