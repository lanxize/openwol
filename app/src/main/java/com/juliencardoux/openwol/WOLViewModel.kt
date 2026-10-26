// Modified by lanxize in 2026-10. See CHANGES.md for details.
package com.juliencardoux.openwol

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WOLViewModel(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    var devices = mutableStateListOf<Device>()
        private set
    var automations = mutableStateListOf<Automation>()
        private set

    init {
        loadDevices()
        enforceSingleDefault()
        loadAutomations()
    }

    // ── The default device ────────────────────────────────────────────────────────
    // Invariant: whenever at least one device is stored, exactly one of them is
    // flagged as the default, and that selection can never be cleared from the UI.
    // WolTileService reads this flag to decide which MAC to wake, so it must never be
    // missing and must never be set on two devices at once.

    /**
     * Restores the invariant after loading devices.
     *
     * This doubles as the migration for data written before Quick Settings tiles
     * existed: those devices all decode with `isDefault = false`, so the first one is
     * promoted instead of leaving the tile with nothing to wake.
     */
    private fun enforceSingleDefault() {
        if (devices.isEmpty()) return
        val flagged = devices.indexOfFirst { it.isDefault }
        val winner = if (flagged == -1) 0 else flagged
        var changed = flagged == -1
        for (i in devices.indices) {
            if (devices[i].isDefault != (i == winner)) {
                devices[i] = devices[i].copy(isDefault = i == winner)
                changed = true
            }
        }
        if (changed) saveDevices()
    }

    private fun loadDevices() {
        devices.clear()
        devices.addAll(loadJsonList<Device>(prefs, AppConstants.PREFS_DEVICE_LIST))
    }

    private fun saveDevices() {
        prefs.edit().putString(AppConstants.PREFS_DEVICE_LIST, Json.encodeToString(devices.toList())).apply()
    }

    fun addDevice(device: Device) {
        // The first device ever added becomes the default automatically.
        devices.add(device.copy(isDefault = devices.isEmpty()))
        saveDevices()
    }

    /** Makes [device] the one and only default. The selection cannot be cleared. */
    fun setDefaultDevice(device: Device) {
        val index = devices.indexOf(device)
        if (index == -1 || devices[index].isDefault) return
        for (i in devices.indices) devices[i] = devices[i].copy(isDefault = i == index)
        saveDevices()
    }

    fun updateDevice(old: Device, new: Device) {
        val index = devices.indexOf(old)
        if (index == -1) return
        // Editing a device must never move the default flag onto or off it.
        devices[index] = new.copy(isDefault = old.isDefault)
        saveDevices()

        // Automations reference their devices by MAC. Without this, editing a MAC would
        // silently orphan every rule pointing at it - the rule survives but wakes
        // nothing, with no hint to the user about why.
        if (old.mac != new.mac) replaceMacReferences(old.mac, new.mac)
    }

    /** Repoints every automation that referenced [oldMac] at [newMac]. */
    private fun replaceMacReferences(oldMac: String, newMac: String) {
        var changed = false
        for (i in automations.indices) {
            val ids = automations[i].deviceIds
            if (oldMac in ids) {
                automations[i] = automations[i].copy(
                    deviceIds = ids.map { if (it == oldMac) newMac else it }.distinct()
                )
                changed = true
            }
        }
        if (changed) saveAutomations()
    }

    fun removeDevice(device: Device) {
        val wasDefault = device.isDefault
        devices.remove(device)
        // Removing the default promotes the first remaining device so the tile keeps
        // having something to wake.
        if (wasDefault && devices.isNotEmpty()) devices[0] = devices[0].copy(isDefault = true)
        saveDevices()

        // Drop the deleted device from every rule, and delete rules left with no devices
        // at all since they could never wake anything. Leaving dangling MACs behind would
        // let a rule silently re-attach if the same MAC is ever added back.
        val surviving = automations.mapNotNull { automation ->
            if (automation.deviceIds.isEmpty()) return@mapNotNull automation
            val remaining = automation.deviceIds.filter { mac -> devices.any { it.mac == mac } }
            when {
                remaining.isEmpty() -> null
                remaining.size == automation.deviceIds.size -> automation
                else -> automation.copy(deviceIds = remaining)
            }
        }
        if (surviving != automations.toList()) {
            automations.clear()
            automations.addAll(surviving)
            saveAutomations()
        }
    }

    private fun loadAutomations() {
        automations.clear()
        automations.addAll(loadJsonList<Automation>(prefs, AppConstants.PREFS_AUTOMATION_LIST))
    }

    private fun saveAutomations() {
        prefs.edit().putString(AppConstants.PREFS_AUTOMATION_LIST, Json.encodeToString(automations.toList())).apply()
    }

    fun addAutomation(automation: Automation) { automations.add(automation); saveAutomations() }

    fun updateAutomation(old: Automation, new: Automation) {
        val index = automations.indexOfFirst { it.id == old.id }
        if (index != -1) { automations[index] = new; saveAutomations() }
    }

    fun removeAutomation(automation: Automation) { automations.remove(automation); saveAutomations() }
}
