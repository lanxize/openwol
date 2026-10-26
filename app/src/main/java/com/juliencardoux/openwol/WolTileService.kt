package com.juliencardoux.openwol

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings tile that wakes the default device with a single tap, without ever
 * launching the app.
 *
 * Long-pressing the tile opens [MainActivity], which the system resolves through the
 * `QS_TILE_PREFERENCES` intent filter declared on the activity.
 *
 * Note on honesty: Wake-on-LAN is fire-and-forget UDP. There is no acknowledgement
 * from the target machine, so this tile can only report that a magic packet was handed
 * to the network stack — never that a machine actually powered on. That is also why the
 * tile is a momentary button rather than a toggle: it has no state to show.
 */
class WolTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        refreshTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        // Called every time the panel becomes visible, which is also how the tile picks
        // up a default device that was changed inside the app in the meantime.
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val device = loadDefaultDevice(this)

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_wol)
        tile.label = device?.let { getString(R.string.tile_title, it.name) }
            ?: getString(R.string.tile_label)
        // Non-toggleable momentary button: there is no on/off state to represent.
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        // Order matters: "no device configured" is the more actionable problem, so it
        // wins over the network check when both would fail.
        val device = loadDefaultDevice(this)
        if (device == null) {
            reject(getString(R.string.tile_no_device))
            return
        }
        if (!isLocalNetworkAvailable(this)) {
            reject(getString(R.string.tile_network_error))
            return
        }

        // onClick() runs on the service's main thread, so the blocking UDP send goes
        // to its own thread — otherwise the panel would stutter.
        Thread { sendWakeOnLan(device) }.start()

        // Haptics first: they still fire when the system drops the toast below, which
        // happens whenever the app has no visible UI and its notifications are off.
        vibrate(this, HAPTIC_SENT)
        toast(getString(R.string.tile_packet_sent))
    }

    /**
     * Rejects a tap: two pulses, deliberately distinct from the single pulse that marks a
     * successful send, followed by the toast that explains the reason.
     */
    private fun reject(message: String) {
        vibrate(this, HAPTIC_REJECTED)
        toast(message)
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        /** Single short buzz: the packet went out. */
        private val HAPTIC_SENT = longArrayOf(0, 50)

        /** Two pulses: nothing was sent. */
        private val HAPTIC_REJECTED = longArrayOf(0, 45, 90, 45)
    }
}
