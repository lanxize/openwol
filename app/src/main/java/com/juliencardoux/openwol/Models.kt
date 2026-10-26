// Modified by lanxize in 2026-10. See CHANGES.md for details.
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
    val color: Long = 0xFF2196F3,
    /**
     * Marks the device that the Quick Settings tile wakes.
     *
     * Exactly one stored device carries this flag whenever the list is non-empty;
     * [WOLViewModel] is responsible for keeping that invariant. It defaults to false
     * so devices persisted by earlier versions still deserialize cleanly, and because
     * kotlinx.serialization skips default values the flag costs nothing in the JSON
     * for every non-default device.
     */
    val isDefault: Boolean = false
)

@Serializable
data class Automation(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val ssid: String,
    val deviceIds: List<String> = emptyList(),
    val notifyOnTrigger: Boolean = false
)
