package com.juliencardoux.openwol

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun WOLScreen(
    devices: List<Device>,
    bottomContentPadding: Dp,
    onDelete: (Device) -> Unit,
    onEdit: (Device) -> Unit,
    onWake: (Device) -> Unit
) {
    if (devices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No devices added yet", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp + bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device,
                    onEdit = { onEdit(device) },
                    onDelete = { onDelete(device) },
                    onWake = { onWake(device) }
                )
            }
        }
    }
}

@Composable
fun DeviceItem(device: Device, onEdit: () -> Unit, onDelete: () -> Unit, onWake: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        onClick = onWake
    ) {
        Box(modifier = Modifier.height(IntrinsicSize.Min), contentAlignment = Alignment.CenterStart) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val glowBrush = Brush.radialGradient(
                    colors = listOf(Color(device.color).copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(x = 0f, y = size.height / 2),
                    radius = size.width * 0.7f
                )
                drawRect(brush = glowBrush)
            }
            Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(Color(device.color), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = device.mac, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun AutomationItem(
    automation: Automation,
    devices: List<Device>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val automationDevices = automation.deviceIds.mapNotNull { mac -> devices.find { it.mac == mac } }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (automation.notifyOnTrigger) {
                        Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(automation.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("SSID: ${automation.ssid}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (automationDevices.isNotEmpty()) {
                    Text(
                        automationDevices.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

@Composable
fun RemoteWaveOverlay(visible: Boolean, color: Long, onFinished: () -> Unit) {
    val textColor = if (color == 0xFFFFEB3BL || color == 0xFFFFC300L) Color.Black else Color.White
    val context = LocalContext.current
    LaunchedEffect(visible) {
        if (visible) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(70)
                }
            }
            delay(1200)
            onFinished()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { with(density) { 100.dp.roundToPx() } }) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { with(density) { -800.dp.roundToPx() } },
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(400))
        ) {
            Surface(
                color = Color(color).copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 12.dp,
                modifier = Modifier.widthIn(min = 220.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Packet Sent",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionWarningOverlay(isReady: Boolean) {
    AnimatedVisibility(
        visible = !isReady,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.98f),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Local Network Required", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Wake-on-LAN packets require a WiFi or VPN connection to reach your devices.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
