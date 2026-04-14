package com.juliencardoux.openwol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AutomationService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    //Skip the first onAvailable callback, which fires for the already-connected network the moment registerNetworkCallback() is called
    //Without this flag every app open would immediately trigger all matching automations
    private var isInitialCallback = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForegroundCompat()
        registerNetworkCallback()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "Automation Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { setShowBadge(false) }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    AppConstants.CHANNEL_AUTOMATIONS,
                    "Automations",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    private fun buildServiceNotification(): Notification =
        NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Automations active")
            .setContentText("Watching for Wi-Fi changes")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun startForegroundCompat() {
        //FOREGROUND_SERVICE_TYPE_LOCATION is required on Android 10+ because this service
        //reads the Wi-Fi SSID via WifiManager to match user-configured automations
        //Android mandates this type whenever a foreground service accesses location APIs,
        //even though the SSID is the only value read and no location data is stored or shared
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                SERVICE_NOTIFICATION_ID,
                buildServiceNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification())
        }
    }

    private fun registerNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isInitialCallback) {
                    isInitialCallback = false
                    return
                }
                val ssid = getCurrentSsid(applicationContext) ?: return
                val prefs = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                val automations = loadJsonList<Automation>(prefs, AppConstants.PREFS_AUTOMATION_LIST)
                val devices = loadJsonList<Device>(prefs, AppConstants.PREFS_DEVICE_LIST)

                automations
                    .filter { it.ssid.equals(ssid, ignoreCase = true) }
                    .forEach { automation ->
                        automation.deviceIds
                            .mapNotNull { mac -> devices.find { it.mac == mac } }
                            .forEach { device -> Thread { sendWakeOnLan(device) }.start() }
                        if (automation.notifyOnTrigger)
                            sendAutomationNotification(applicationContext, automation.name, ssid)
                    }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        if (::networkCallback.isInitialized)
            connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val SERVICE_NOTIFICATION_ID = 1001
        const val SERVICE_CHANNEL_ID = "automation_service_channel"

        fun start(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AutomationService::class.java))
        }
    }
}
