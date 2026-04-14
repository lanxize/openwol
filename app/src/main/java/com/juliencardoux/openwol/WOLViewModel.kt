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
        loadAutomations()
    }

    private fun loadDevices() {
        devices.clear()
        devices.addAll(loadJsonList<Device>(prefs, AppConstants.PREFS_DEVICE_LIST))
    }

    private fun saveDevices() {
        prefs.edit().putString(AppConstants.PREFS_DEVICE_LIST, Json.encodeToString(devices.toList())).apply()
    }

    fun addDevice(device: Device) { devices.add(device); saveDevices() }

    fun updateDevice(old: Device, new: Device) {
        val index = devices.indexOf(old)
        if (index != -1) { devices[index] = new; saveDevices() }
    }

    fun removeDevice(device: Device) {
        devices.remove(device)
        saveDevices()
        val emptied = automations.filter { automation ->
            automation.deviceIds.isNotEmpty() &&
            automation.deviceIds.none { mac -> devices.any { d -> d.mac == mac } }
        }
        if (emptied.isNotEmpty()) {
            automations.removeAll(emptied.toSet())
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
