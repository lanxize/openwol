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
        .setContentTitle("Automation triggered")
        .setContentText("\"$automationName\" fired on Wi-Fi \"$ssid\"")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    nm.notify(automationName.hashCode(), notification)
}
