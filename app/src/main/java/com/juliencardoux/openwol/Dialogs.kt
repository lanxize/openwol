package com.juliencardoux.openwol

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.juliencardoux.openwol.ui.theme.ThemeMode

@Composable
fun DialogScrollbar(state: ScrollState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxHeight().width(4.dp)) {
        val maxScroll = state.maxValue.toFloat()
        if (maxScroll <= 0f) return@Canvas
        val thumbFraction = size.height / (size.height + maxScroll)
        val thumbHeight = (size.height * thumbFraction).coerceAtLeast(24.dp.toPx())
        val thumbY = (size.height - thumbHeight) * (state.value.toFloat() / maxScroll)
        drawRoundRect(color = Color.Gray.copy(alpha = 0.15f), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRoundRect(
            color = Color.Gray.copy(alpha = 0.45f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, thumbY),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help") },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(end = 12.dp)
                ) {
                    Text("1. BIOS Setup", fontWeight = FontWeight.Bold)
                    Text("Enable 'Wake on LAN' or 'Power On By PCI-E' in your computer's BIOS settings.")
                    Spacer(Modifier.height(4.dp))
                    Text("2. Windows Setup", fontWeight = FontWeight.Bold)
                    Text("In Device Manager, go to your Network Adapter properties, in Advanced check 'Allow this device to wake the computer', 'Wake on Magic Packet' & 'Wake on magic packet when system is in the S0ix power state'.")
                    Spacer(Modifier.height(4.dp))
                    Text("3. Linux Setup", fontWeight = FontWeight.Bold)
                    Text("You already know how to do it.")
                    Spacer(Modifier.height(4.dp))
                    Text("4. Networking", fontWeight = FontWeight.Bold)
                    Text("If waking fails, use 'Advanced Settings' in OpenWOL to specify your PC's IP and Subnet Mask.")
                    Spacer(Modifier.height(4.dp))
                    Text("⚠️ It's best to set the IP address to avoid spamming your entire network range.")
                }
                DialogScrollbar(state = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun InfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
                Text("OpenWOL")
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(scrollState).padding(end = 12.dp)
                ) {
                    Text("A simple, open-source Wake-on-LAN app for Android.")

                    Spacer(Modifier.height(2.dp))
                    Text("Privacy", fontWeight = FontWeight.Bold)
                    Text("No data is collected, stored, or shared — ever. The app works entirely offline and does not require an account or internet access.")

                    Spacer(Modifier.height(2.dp))
                    Text("Source Code", fontWeight = FontWeight.Bold)
                    Text(
                        "gitlab.com/julien.cardoux/openwol",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gitlab.com/julien.cardoux/openwol")))
                        }
                    )

                    Spacer(Modifier.height(2.dp))
                    Text("Open Source Libraries", fontWeight = FontWeight.Bold)
                    Text("• Kotlin — Apache 2.0 (JetBrains)")
                    Text("• Jetpack Compose — Apache 2.0 (Google)")
                    Text("• Material Design 3 / Icons — Apache 2.0 (Google)")
                    Text("• AndroidX (Core, Lifecycle, SplashScreen) — Apache 2.0 (Google)")
                    Text("• kotlinx.serialization — Apache 2.0 (JetBrains)")
                    Text("• Google Fonts Icons — Apache 2.0 (Google)")

                    Spacer(Modifier.height(2.dp))
                    Text("License", fontWeight = FontWeight.Bold)
                    val licenseText = buildAnnotatedString {
                        append("OpenWOL is released under the ")
                        pushStringAnnotation(tag = "URL", annotation = "https://www.gnu.org/licenses/agpl-3.0.html")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("GNU AGPLv3 License")
                        }
                        pop()
                        append(".")
                    }
                    ClickableText(
                        text = licenseText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            licenseText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item)))
                                }
                        }
                    )
                }
                DialogScrollbar(state = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SettingsDialog(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val themeLabel = when (currentTheme) {
        ThemeMode.System -> "System default"
        ThemeMode.Light  -> "Light"
        ThemeMode.Dark   -> "Dark"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                val density = LocalDensity.current
                var fieldWidthPx by remember { mutableStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { fieldWidthPx = it.width }
                    ) {
                        OutlinedTextField(
                            value = themeLabel,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { dropdownExpanded = true })
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.width(with(density) { fieldWidthPx.toDp() })
                        ) {
                            DropdownMenuItem(text = { Text("System default") }, onClick = { onThemeChange(ThemeMode.System); dropdownExpanded = false })
                            DropdownMenuItem(text = { Text("Light") },          onClick = { onThemeChange(ThemeMode.Light);  dropdownExpanded = false })
                            DropdownMenuItem(text = { Text("Dark") },           onClick = { onThemeChange(ThemeMode.Dark);   dropdownExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun LocationPermissionDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Location Permission Required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Automations need location permission exclusively to read your Wi-Fi network name (SSID), so the app can detect when you join a specific network and wake your devices automatically.")
                Text("This is the only purpose for which location access is used. No location data is ever collected, stored, or shared.")
                Text("When prompted, select \"Allow all the time\" so automations can trigger even when the app is in the background.")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(deviceName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete Device") },
        text = {
            Text(buildAnnotatedString {
                append("Are you sure you want ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(deviceName) }
                append(" to be removed? This cannot be undone.")
            })
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteAutomationConfirmationDialog(automationName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete Automation") },
        text = {
            Text(buildAnnotatedString {
                append("Are you sure you want to delete ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(automationName) }
                append(" automation? This cannot be undone.")
            })
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddDeviceDialog(deviceToEdit: Device? = null, onDismiss: () -> Unit, onConfirm: (Device) -> Unit) {
    val colorOptions = AppConstants.DEVICE_COLOR_OPTIONS
    var name by remember { mutableStateOf(deviceToEdit?.name ?: "") }
    var macText by remember { mutableStateOf(deviceToEdit?.mac?.filter { it.isLetterOrDigit() } ?: "") }
    var ip by remember { mutableStateOf(deviceToEdit?.ip ?: "") }
    var mask by remember { mutableStateOf(deviceToEdit?.mask ?: "255.255.255.0") }
    var selectedPort by remember { mutableIntStateOf(deviceToEdit?.port ?: 9) }
    var selectedColor by remember { mutableStateOf(deviceToEdit?.color ?: colorOptions.first()) }
    var isAdvancedShown by remember { mutableStateOf(deviceToEdit != null && (deviceToEdit.ip.isNotEmpty() || deviceToEdit.port != 9)) }
    var portExpanded by remember { mutableStateOf(false) }

    val isIpValid = remember(ip) {
        ip.isEmpty() || Regex("""^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$""").matches(ip)
    }
    val onDone = KeyboardActions(onDone = { defaultKeyboardAction(ImeAction.Done) })

    val macTransformation = remember {
        VisualTransformation { text ->
            val raw = text.text
            val out = StringBuilder()
            for (i in raw.indices) {
                out.append(raw[i])
                if (i % 2 == 1 && i < raw.length - 1) out.append(':')
            }
            val display = out.toString()
            TransformedText(
                AnnotatedString(display),
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int {
                        var result = offset
                        for (i in 1 until offset step 2) {
                            if (i < raw.length - 1) result++
                        }
                        return result.coerceAtMost(display.length)
                    }
                    override fun transformedToOriginal(offset: Int): Int {
                        var count = 0
                        for (i in 0 until offset.coerceAtMost(display.length)) {
                            if (display[i] != ':') count++
                        }
                        return count.coerceAtMost(raw.length)
                    }
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deviceToEdit == null) "Add Device" else "Edit Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replaceFirstChar { c -> c.uppercaseChar() } },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )
                OutlinedTextField(
                    value = macText,
                    onValueChange = { macText = it.filter { c -> c.isLetterOrDigit() }.take(12).uppercase() },
                    label = { Text("MAC Address") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = macTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isAdvancedShown = !isAdvancedShown }.padding(vertical = 4.dp)) {
                    Checkbox(checked = isAdvancedShown, onCheckedChange = { isAdvancedShown = it })
                    Text("Show Advanced Settings", style = MaterialTheme.typography.bodyMedium)
                }

                AnimatedVisibility(visible = isAdvancedShown) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ip,
                                onValueChange = { input ->
                                    ip = input.filter { it.isDigit() || it == '.' }.take(15)
                                },
                                label = { Text("IP Address") },
                                isError = !isIpValid,
                                placeholder = { Text("192.168.1.1") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = onDone
                            )
                            Box(modifier = Modifier.weight(0.7f)) {
                                OutlinedTextField(value = selectedPort.toString(), onValueChange = {}, readOnly = true, label = { Text("Port") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
                                Box(modifier = Modifier.matchParentSize().clickable { portExpanded = true })
                                DropdownMenu(expanded = portExpanded, onDismissRequest = { portExpanded = false }) {
                                    DropdownMenuItem(text = { Text("9 (Default)") }, onClick = { selectedPort = 9; portExpanded = false })
                                    DropdownMenuItem(text = { Text("7") }, onClick = { selectedPort = 7; portExpanded = false })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = mask,
                            onValueChange = { input ->
                                mask = input.filter { it.isDigit() || it == '.' }.take(15)
                            },
                            label = { Text("Subnet Mask") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = onDone
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Device Color", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        colorOptions.forEach { colorHex ->
                            val isSelected = selectedColor == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(colorHex), CircleShape)
                                    .border(width = if (isSelected) 3.dp else 0.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape)
                                    .clickable { selectedColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Default.Check, null, tint = if (colorHex == 0xFFFFEB3BL) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && macText.length == 12 && isIpValid,
                onClick = {
                    onConfirm(Device(
                        name = name,
                        mac = macText.chunked(2).joinToString(":"),
                        ip = if (isAdvancedShown) ip else "",
                        mask = if (isAdvancedShown) mask else "255.255.255.0",
                        port = if (isAdvancedShown) selectedPort else 9,
                        color = selectedColor
                    ))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddAutomationDialog(
    devices: List<Device>,
    automationToEdit: Automation? = null,
    onDismiss: () -> Unit,
    onConfirm: (Automation) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(automationToEdit?.name ?: "") }
    var ssid by remember { mutableStateOf(automationToEdit?.ssid ?: "") }
    var selectedMacs by remember { mutableStateOf(automationToEdit?.deviceIds?.toSet() ?: setOf()) }
    var notifyOnTrigger by remember { mutableStateOf(automationToEdit?.notifyOnTrigger ?: false) }
    var deviceDropdownExpanded by remember { mutableStateOf(false) }
    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifyOnTrigger = granted }

    val handleNotifyChange: (Boolean) -> Unit = handleNotifyChange@{ checked ->
        if (!checked) { notifyOnTrigger = false; return@handleNotifyChange }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) { notifyOnTrigger = true; return@handleNotifyChange }

        val activity = context as? Activity
        val permanentlyDenied = hasRequestedNotificationPermission &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity, android.Manifest.permission.POST_NOTIFICATIONS
            )

        if (permanentlyDenied) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        } else {
            hasRequestedNotificationPermission = true
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val onDone = KeyboardActions(onDone = { defaultKeyboardAction(ImeAction.Done) })

    LaunchedEffect(Unit) {
        if (automationToEdit == null) ssid = getCurrentSsid(context) ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (automationToEdit == null) "Add Automation" else "Edit Automation") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replaceFirstChar { c -> c.uppercaseChar() } },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Wi-Fi SSID") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when {
                            selectedMacs.isEmpty() -> ""
                            selectedMacs.size == 1 -> devices.find { it.mac == selectedMacs.first() }?.name ?: ""
                            else -> "${selectedMacs.size} devices selected"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Devices") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { deviceDropdownExpanded = true })
                    DropdownMenu(
                        expanded = deviceDropdownExpanded,
                        onDismissRequest = { deviceDropdownExpanded = false }
                    ) {
                        if (devices.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No devices added yet", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {}
                            )
                        } else {
                            devices.forEach { device ->
                                val isChecked = device.mac in selectedMacs
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = isChecked, onCheckedChange = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(device.name)
                                        }
                                    },
                                    onClick = {
                                        selectedMacs = if (isChecked) selectedMacs - device.mac else selectedMacs + device.mac
                                    }
                                )
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { handleNotifyChange(!notifyOnTrigger) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = notifyOnTrigger, onCheckedChange = { handleNotifyChange(it) })
                    Text("Send push notification on trigger", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && ssid.isNotBlank() && selectedMacs.isNotEmpty(),
                onClick = {
                    onConfirm(Automation(
                        id = automationToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name,
                        ssid = ssid,
                        deviceIds = selectedMacs.toList(),
                        notifyOnTrigger = notifyOnTrigger
                    ))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
