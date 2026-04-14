package com.juliencardoux.openwol

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Device(
    val name: String,
    val mac: String,
    val ip: String = "",
    val mask: String = "255.255.255.0",
    val port: Int = 9,
    val color: Long = 0xFF2196F3
)

@Serializable
data class Automation(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val ssid: String,
    val deviceIds: List<String> = emptyList(),
    val notifyOnTrigger: Boolean = false
)
