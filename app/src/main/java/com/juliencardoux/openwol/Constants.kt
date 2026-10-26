// Modified by lanxize in 2026-10. See CHANGES.md for details.
package com.juliencardoux.openwol

object AppConstants {
    const val PREFS_NAME = "wol_prefs"
    const val PREFS_DEVICE_LIST = "device_list"
    const val PREFS_AUTOMATION_LIST = "automation_list"
    const val PREFS_THEME_MODE = "theme_mode"
    const val CHANNEL_AUTOMATIONS = "automation_channel"

    /** Set once the notification-permission rationale has been shown, so we never nag twice. */
    const val PREFS_NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
    val DEVICE_COLOR_OPTIONS = listOf(0xFFFFC300L, 0xFFF44336L, 0xFF4CAF50L, 0xFF2196F3L, 0xFF9C27B0L, 0xFFA1A1A1L)
}
