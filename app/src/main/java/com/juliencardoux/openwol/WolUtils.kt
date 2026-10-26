// Modified by lanxize in 2026-10. See CHANGES.md for details.
package com.juliencardoux.openwol

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

inline fun <reified T> loadJsonList(prefs: SharedPreferences, key: String): List<T> =
    try {
        Json.decodeFromString<List<T>>(prefs.getString(key, "[]") ?: "[]")
    } catch (e: Exception) {
        emptyList()
    }

/**
 * Resolves the device that the Quick Settings tile should wake, straight from storage.
 *
 * [WolTileService] runs as its own service instance and never touches [WOLViewModel],
 * so it reads the persisted list itself. The fallback to the first device covers
 * lists written before the default flag existed, where nothing would be flagged.
 */
fun loadDefaultDevice(context: Context): Device? {
    val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    val devices = loadJsonList<Device>(prefs, AppConstants.PREFS_DEVICE_LIST)
    return devices.firstOrNull { it.isDefault } ?: devices.firstOrNull()
}

/**
 * Checks whether all location permissions required for the Automations feature are granted.
 *
 * Android requires location permissions to read the Wi-Fi SSID via [android.net.wifi.WifiManager].
 * On Android 10+ (Q+), ACCESS_BACKGROUND_LOCATION is additionally required because the
 * [AutomationService] reads the SSID from a foreground service while the app may not be
 * in the foreground. No location data is ever collected, stored, or transmitted — the
 * permission is used exclusively to detect which Wi-Fi network the device is connected to,
 * so that matching automations can be triggered.
 */
fun hasLocationPermissions(context: Context): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasBackground = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    return hasFine && hasBackground
}

fun calculateBroadcast(ip: String, mask: String): String {
    return try {
        val ipParts = ip.split(".").map { it.toInt() }
        val maskParts = mask.split(".").map { it.toInt() }
        val broadcast = IntArray(4)
        for (i in 0..3) {
            broadcast[i] = ipParts[i] or (maskParts[i].inv() and 255)
        }
        broadcast.joinToString(".")
    } catch (e: Exception) { "255.255.255.255" }
}

fun sendWakeOnLan(device: Device) {
    try {
        val cleanMac = device.mac.replace(":", "").replace("-", "")
        val macBytes = ByteArray(6)
        for (i in 0 until 6) macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        val packet = ByteArray(102)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 1..16) System.arraycopy(macBytes, 0, packet, i * 6, 6)

        val broadcastIP = if (device.ip.isNotBlank()) calculateBroadcast(device.ip, device.mask) else "255.255.255.255"
        val address = InetAddress.getByName(broadcastIP)

        DatagramSocket().use { it.broadcast = true; it.send(DatagramPacket(packet, packet.size, address, device.port)) }
    } catch (e: Exception) { e.printStackTrace() }
}

fun isLocalNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        networkInfo != null && (networkInfo.type == ConnectivityManager.TYPE_WIFI || networkInfo.type == ConnectivityManager.TYPE_VPN)
    }
}

@Suppress("DEPRECATION")
fun getCurrentSsid(context: Context): String? {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        info?.ssid
            ?.takeIf { it != "<unknown ssid>" && it.isNotBlank() }
            ?.removeSurrounding("\"")
    } catch (e: Exception) { null }
}

/**
 * Plays a haptic pattern on the default vibrator.
 *
 * [timings] alternates off/on durations in milliseconds and must start with an off
 * duration, e.g. `longArrayOf(0, 45)` is a single 45 ms buzz.
 *
 * The usage hint matters more than it looks. A Quick Settings tile runs while the app has
 * no visible UI, and the platform drops vibrations from background apps unless the usage
 * is on its `BACKGROUND_PROCESS_USAGE_ALLOWLIST`
 * (`VibrationSettings.shouldIgnoreVibration`). The default USAGE_TOUCH is *not* on that
 * list - it would be logged as `ignored_background` - while USAGE_PHYSICAL_EMULATION is,
 * and "simulating a physical button press" is exactly what tapping a tile is.
 *
 * VIBRATE is a normal permission granted at install, so this needs no prompt and cannot be
 * revoked by the user - which makes haptics the tile's only guaranteed feedback channel.
 */
fun vibrate(context: Context, timings: LongArray) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (!vibrator.hasVibrator()) return

    when {
        // The VibrationAttributes overload only exists from API 33.
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> vibrator.vibrate(
            VibrationEffect.createWaveform(timings, -1),
            VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION)
                .build()
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        else -> {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}

fun sendAutomationNotification(context: Context, automationName: String, ssid: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, AppConstants.CHANNEL_AUTOMATIONS)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.notification_automation_title))
        .setContentText(context.getString(R.string.notification_automation_text, automationName, ssid))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    nm.notify(automationName.hashCode(), notification)
}
