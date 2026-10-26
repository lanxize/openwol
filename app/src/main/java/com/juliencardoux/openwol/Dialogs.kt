// Modified by lanxize in 2026-10. See CHANGES.md for details.
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
import androidx.compose.ui.res.stringResource
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
        title = { Text(stringResource(R.string.help_title)) },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(end = 12.dp)
                ) {
                    Text(stringResource(R.string.help_bios_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.help_bios_body))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.help_windows_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.help_windows_body))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.help_linux_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.help_linux_body))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.help_network_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.help_network_body))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.help_warning))
                }
                DialogScrollbar(state = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
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
                Text(stringResource(R.string.app_name))
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(scrollState).padding(end = 12.dp)
                ) {
                    Text(stringResource(R.string.info_tagline))

                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.info_privacy_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.info_privacy_body))

                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.info_source_title), fontWeight = FontWeight.Bold)
                    Text(
                        "gitlab.com/julien.cardoux/openwol",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://gitlab.com/julien.cardoux/openwol")))
                        }
                    )

                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.info_libraries_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.info_library_kotlin))
                    Text(stringResource(R.string.info_library_compose))
                    Text(stringResource(R.string.info_library_material))
                    Text(stringResource(R.string.info_library_androidx))
                    Text(stringResource(R.string.info_library_serialization))
                    Text(stringResource(R.string.info_library_icons))

                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.info_license_title), fontWeight = FontWeight.Bold)
                    // stringResource() is a composable call, so the fragments are resolved
                    // before entering the non-composable buildAnnotatedString() scope.
                    val licensePrefix = stringResource(R.string.info_license_prefix)
                    val licenseLink = stringResource(R.string.info_license_link)
                    val licenseSuffix = stringResource(R.string.info_license_suffix)
                    val licenseText = buildAnnotatedString {
                        append(licensePrefix)
                        pushStringAnnotation(tag = "URL", annotation = "https://www.gnu.org/licenses/agpl-3.0.html")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(licenseLink)
                        }
                        pop()
                        append(licenseSuffix)
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
            Button(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
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
        ThemeMode.System -> stringResource(R.string.theme_system)
        ThemeMode.Light  -> stringResource(R.string.theme_light)
        ThemeMode.Dark   -> stringResource(R.string.theme_dark)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Box(modifier = Modifier.heightIn(max = 360.dp)) {
                val density = LocalDensity.current
                var fieldWidthPx by remember { mutableStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_theme), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
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
                            DropdownMenuItem(text = { Text(stringResource(R.string.theme_system)) }, onClick = { onThemeChange(ThemeMode.System); dropdownExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.theme_light)) },  onClick = { onThemeChange(ThemeMode.Light);  dropdownExpanded = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.theme_dark)) },   onClick = { onThemeChange(ThemeMode.Dark);   dropdownExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

@Composable
fun LocationPermissionDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.location_permission_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.location_permission_body_1))
                Text(stringResource(R.string.location_permission_body_2))
                Text(stringResource(R.string.location_permission_body_3))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun NotificationPermissionDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.notification_permission_body_1))
                Text(stringResource(R.string.notification_permission_body_2))
                Text(stringResource(R.string.notification_permission_body_3))
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.notification_permission_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.notification_permission_cancel)) }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(deviceName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.delete_device_title)) },
        text = {
            val prefix = stringResource(R.string.delete_device_prefix)
            val suffix = stringResource(R.string.delete_device_suffix)
            Text(buildAnnotatedString {
                append(prefix)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(deviceName) }
                append(suffix)
            })
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun DeleteAutomationConfirmationDialog(automationName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.delete_automation_title)) },
        text = {
            val prefix = stringResource(R.string.delete_automation_prefix)
            val suffix = stringResource(R.string.delete_automation_suffix)
            Text(buildAnnotatedString {
                append(prefix)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(automationName) }
                append(suffix)
            })
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
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
        title = { Text(stringResource(if (deviceToEdit == null) R.string.add_device_title else R.string.edit_device_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replaceFirstChar { c -> c.uppercaseChar() } },
                    label = { Text(stringResource(R.string.field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )
                OutlinedTextField(
                    value = macText,
                    onValueChange = { macText = it.filter { c -> c.isLetterOrDigit() }.take(12).uppercase() },
                    label = { Text(stringResource(R.string.field_mac)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = macTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isAdvancedShown = !isAdvancedShown }.padding(vertical = 4.dp)) {
                    Checkbox(checked = isAdvancedShown, onCheckedChange = { isAdvancedShown = it })
                    Text(stringResource(R.string.show_advanced), style = MaterialTheme.typography.bodyMedium)
                }

                AnimatedVisibility(visible = isAdvancedShown) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ip,
                                onValueChange = { input ->
                                    ip = input.filter { it.isDigit() || it == '.' }.take(15)
                                },
                                label = { Text(stringResource(R.string.field_ip)) },
                                isError = !isIpValid,
                                placeholder = { Text(stringResource(R.string.placeholder_ip)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = onDone
                            )
                            Box(modifier = Modifier.weight(0.7f)) {
                                OutlinedTextField(value = selectedPort.toString(), onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.field_port)) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
                                Box(modifier = Modifier.matchParentSize().clickable { portExpanded = true })
                                DropdownMenu(expanded = portExpanded, onDismissRequest = { portExpanded = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.port_default)) }, onClick = { selectedPort = 9; portExpanded = false })
                                    DropdownMenuItem(text = { Text("7") }, onClick = { selectedPort = 7; portExpanded = false })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = mask,
                            onValueChange = { input ->
                                mask = input.filter { it.isDigit() || it == '.' }.take(15)
                            },
                            label = { Text(stringResource(R.string.field_subnet_mask)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = onDone
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.device_color), style = MaterialTheme.typography.labelMedium)
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
                        color = selectedColor,
                        // Carry the flag through: this dialog rebuilds the whole Device
                        // from scratch, so dropping it here would silently clear the
                        // default selection whenever a device is edited.
                        isDefault = deviceToEdit?.isDefault ?: false
                    ))
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
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
        title = { Text(stringResource(if (automationToEdit == null) R.string.add_automation_title else R.string.edit_automation_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replaceFirstChar { c -> c.uppercaseChar() } },
                    label = { Text(stringResource(R.string.field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text(stringResource(R.string.field_ssid)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = onDone
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = when {
                            selectedMacs.isEmpty() -> ""
                            selectedMacs.size == 1 -> devices.find { it.mac == selectedMacs.first() }?.name ?: ""
                            else -> stringResource(R.string.devices_selected_count, selectedMacs.size)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_devices)) },
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
                                text = { Text(stringResource(R.string.no_devices), color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                    Text(stringResource(R.string.notify_on_trigger), style = MaterialTheme.typography.bodyMedium)
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
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
