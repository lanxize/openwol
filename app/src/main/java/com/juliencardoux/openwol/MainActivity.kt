// Modified by lanxize in 2026-10. See CHANGES.md for details.
package com.juliencardoux.openwol

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.juliencardoux.openwol.ui.theme.OpenWOLTheme
import com.juliencardoux.openwol.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE) }
            var themeMode by remember {
                mutableStateOf(ThemeMode.values().getOrElse(prefs.getInt(AppConstants.PREFS_THEME_MODE, 0)) { ThemeMode.System })
            }

            OpenWOLTheme(themeMode = themeMode) {
                val viewModel = remember { WOLViewModel(context) }
                val scope = rememberCoroutineScope()

                var deviceToEdit by remember { mutableStateOf<Device?>(null) }
                var deviceToDelete by remember { mutableStateOf<Device?>(null) }
                var showAddDialog by remember { mutableStateOf(false) }
                var showHelpDialog by remember { mutableStateOf(false) }
                var showInfoDialog by remember { mutableStateOf(false) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var activeAnimationColor by remember { mutableStateOf(0xFF2196F3L) }
                var showSentWave by remember { mutableStateOf(false) }
                var isNetworkReady by remember { mutableStateOf(true) }
                var isMenuExpanded by remember { mutableStateOf(false) }
                var isAutomateExpanded by remember { mutableStateOf(false) }
                var showAddAutomationDialog by remember { mutableStateOf(false) }
                var automationToEdit by remember { mutableStateOf<Automation?>(null) }
                var automationToDelete by remember { mutableStateOf<Automation?>(null) }
                var showLocationPermissionRationale by remember { mutableStateOf(false) }
                var showNotificationPermissionRationale by remember { mutableStateOf(false) }

                //Android 10+: Try requesting background location access directly
                //On Android 11+, if the user previously chose "While using the app", launch() returns denied silently
                //In that case, the user is redirected to app Settings to manually grant "Allow all the time"
                val backgroundLocationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        if (viewModel.automations.isNotEmpty()) AutomationService.start(context)
                    } else {
                        val activity = context as? android.app.Activity
                        if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                                activity, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        ) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    }
                }

                val requestBackgroundLocation: () -> Unit = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }

                //When the user returns from the system settings page (Android 11+ path), after
                //granting "Allow all the time", start the service if permissions are satisfied
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME && viewModel.automations.isNotEmpty()) {
                            if (hasLocationPermissions(context)) AutomationService.start(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                //Request both COARSE and FINE together. Android requires COARSE to be declared alongside FINE
                //The result map tells us which was actually granted
                //We need FINE specifically (COARSE alone can't read the Wi-Fi SSID)
                val fineLocationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
                    if (!fineGranted) return@rememberLauncherForActivityResult
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestBackgroundLocation()
                    } else if (viewModel.automations.isNotEmpty()) {
                        AutomationService.start(context)
                    }
                }

                LaunchedEffect(Unit) {
                    while (true) {
                        isNetworkReady = isLocalNetworkAvailable(context)
                        delay(2000)
                    }
                }

                //Ask for notification permission once, on first launch, and explain why.
                //This is not cosmetic: Android 13+ silently drops an app's toasts while its
                //notifications are switched off and it has no visible UI - which is exactly
                //the Quick Settings tile's situation. Without the permission the tile would
                //send its packet and appear to do nothing at all.
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* Answered either way; the flag below means we never ask twice. */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED &&
                        !prefs.getBoolean(AppConstants.PREFS_NOTIFICATION_PERMISSION_ASKED, false)
                    ) {
                        showNotificationPermissionRationale = true
                    }
                }

                //Every time the automate panel opens, verify location permissions
                //Show rationale dialog if any required permission is missing
                LaunchedEffect(isAutomateExpanded) {
                    if (!isAutomateExpanded) return@LaunchedEffect
                    if (!hasLocationPermissions(context)) showLocationPermissionRationale = true
                }

                //Start or stop the service when the automations list changes
                //Require both precise location AND "Allow all the time" (background) before starting
                LaunchedEffect(viewModel.automations.size) {
                    if (viewModel.automations.isEmpty()) {
                        AutomationService.stop(context)
                    } else if (hasLocationPermissions(context)) {
                        AutomationService.start(context)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            val curveHeight = 12.dp
                            val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(2f)
                                    .pointerInput(Unit) {
                                        detectTapGestures { }
                                    }
                                    .drawBehind {
                                        val shadowColor = Color.Black.copy(alpha = 0.5f)
                                        val shadowRadius = 12.dp.toPx()
                                        val ch = curveHeight.toPx()

                                        val shadowPath = android.graphics.Path().apply {
                                            moveTo(0f, ch)
                                            quadTo(size.width / 2f, -ch, size.width, ch)
                                            lineTo(size.width, size.height)
                                            lineTo(0f, size.height)
                                            close()
                                        }
                                        drawIntoCanvas { canvas ->
                                            val paint = android.graphics.Paint().apply {
                                                color = android.graphics.Color.TRANSPARENT
                                                setShadowLayer(shadowRadius, 0f, -4f, shadowColor.toArgb())
                                            }
                                            canvas.nativeCanvas.drawPath(shadowPath, paint)
                                        }
                                    }
                                    .graphicsLayer {
                                        shape = ArchShape(curveHeight)
                                        clip = true
                                    }
                                    .background(backgroundColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 32.dp)
                                        .padding(top = curveHeight + 12.dp, bottom = 0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val automateInteractionSource = remember { MutableInteractionSource() }

                                    //Menu Toggle Button
                                    BottomBarItem(
                                        icon = if (isMenuExpanded) Icons.Default.Close else Icons.Default.Menu,
                                        label = stringResource(R.string.bottom_bar_menu),
                                        onClick = {
                                            if (isAutomateExpanded) {
                                                isAutomateExpanded = false
                                                isMenuExpanded = true
                                            } else {
                                                isMenuExpanded = !isMenuExpanded
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    //Add Button
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .offset(y = (-10).dp)
                                                .size(52.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = false, radius = 36.dp),
                                                    onClick = {
                                                        if (isAutomateExpanded) {
                                                            isAutomateExpanded = false
                                                            if (isNetworkReady) showAddDialog = true
                                                        } else if (isNetworkReady) {
                                                            showAddDialog = true
                                                        }
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = stringResource(R.string.cd_add_device),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    //Automate Button
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = automateInteractionSource,
                                                indication = ripple(bounded = false, radius = 32.dp),
                                                onClick = {
                                                    if (!isAutomateExpanded) isMenuExpanded = false
                                                    isAutomateExpanded = !isAutomateExpanded
                                                }
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (isAutomateExpanded) Icons.Default.Close else Icons.Default.BuildCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.bottom_bar_automate),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->

                        val blurRadius by animateDpAsState(targetValue = if (isMenuExpanded) 8.dp else 0.dp, label = "blur")

                        Box(modifier = Modifier.fillMaxSize()) {

                            //Main blurred content area
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .blur(blurRadius)
                                .padding(top = innerPadding.calculateTopPadding())
                            ) {
                                WOLScreen(
                                    devices = viewModel.devices,
                                    bottomContentPadding = innerPadding.calculateBottomPadding(),
                                    onDelete = { deviceToDelete = it },
                                    onEdit = { deviceToEdit = it },
                                    onWake = { device ->
                                        activeAnimationColor = device.color
                                        scope.launch(Dispatchers.IO) {
                                            sendWakeOnLan(device)
                                            showSentWave = true
                                        }
                                    },
                                    onSetDefault = { viewModel.setDefaultDevice(it) }
                                )

                                if (showAddDialog) { AddDeviceDialog(onDismiss = { showAddDialog = false }, onConfirm = { viewModel.addDevice(it); showAddDialog = false }) }
                                if (deviceToEdit != null) { AddDeviceDialog(deviceToEdit = deviceToEdit, onDismiss = { deviceToEdit = null }, onConfirm = { viewModel.updateDevice(deviceToEdit!!, it); deviceToEdit = null }) }
                                if (deviceToDelete != null) { DeleteConfirmationDialog(deviceName = deviceToDelete!!.name, onDismiss = { deviceToDelete = null }, onConfirm = { viewModel.removeDevice(deviceToDelete!!); deviceToDelete = null }) }
                                if (showHelpDialog) { HelpDialog(onDismiss = { showHelpDialog = false }) }
                                if (showInfoDialog) { InfoDialog(onDismiss = { showInfoDialog = false }) }
                                if (showSettingsDialog) {
                                    SettingsDialog(
                                        currentTheme = themeMode,
                                        onThemeChange = { mode ->
                                            themeMode = mode
                                            prefs.edit().putInt(AppConstants.PREFS_THEME_MODE, mode.ordinal).apply()
                                        },
                                        onDismiss = { showSettingsDialog = false }
                                    )
                                }
                            }

                            //Invisible overlay to catch clicks and close the menu
                            if (isMenuExpanded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.1f))
                                        .pointerInput(Unit) {
                                            detectTapGestures { isMenuExpanded = false }
                                        }
                                )
                            }

                            //Automate Panel
                            AnimatedVisibility(
                                visible = isAutomateExpanded,
                                enter = slideInHorizontally(initialOffsetX = { it }),
                                exit = slideOutHorizontally(targetOffsetX = { it }),
                                modifier = Modifier.fillMaxSize().zIndex(5f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .statusBarsPadding()
                                        .pointerInput(Unit) { detectTapGestures {} }
                                ) {
                                    if (viewModel.automations.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(bottom = innerPadding.calculateBottomPadding()),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(stringResource(R.string.no_automations), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(
                                                start = 16.dp,
                                                top = 16.dp,
                                                end = 16.dp,
                                                bottom = innerPadding.calculateBottomPadding() + 80.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(viewModel.automations) { automation ->
                                                AutomationItem(
                                                    automation = automation,
                                                    devices = viewModel.devices,
                                                    onEdit = { automationToEdit = automation },
                                                    onDelete = { automationToDelete = automation }
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { showAddAutomationDialog = true },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 24.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.add_automation), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
                                    }

                                    if (showAddAutomationDialog) {
                                        AddAutomationDialog(
                                            devices = viewModel.devices,
                                            onDismiss = { showAddAutomationDialog = false },
                                            onConfirm = { viewModel.addAutomation(it); showAddAutomationDialog = false }
                                        )
                                    }
                                    if (automationToEdit != null) {
                                        AddAutomationDialog(
                                            devices = viewModel.devices,
                                            automationToEdit = automationToEdit,
                                            onDismiss = { automationToEdit = null },
                                            onConfirm = { viewModel.updateAutomation(automationToEdit!!, it); automationToEdit = null }
                                        )
                                    }
                                    if (automationToDelete != null) {
                                        DeleteAutomationConfirmationDialog(
                                            automationName = automationToDelete!!.name,
                                            onDismiss = { automationToDelete = null },
                                            onConfirm = { viewModel.removeAutomation(automationToDelete!!); automationToDelete = null }
                                        )
                                    }
                                }
                            }

                            //Menu Panel
                            AnimatedVisibility(
                                visible = isMenuExpanded,
                                enter = slideInVertically(initialOffsetY = { it }),
                                exit = slideOutVertically(targetOffsetY = { it }),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(start = 14.dp, bottom = innerPadding.calculateBottomPadding() - 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(142.dp)
                                        .drawBehind {
                                            val shadowColor = Color.Black.copy(alpha = 0.5f)
                                            val shadowRadius = 18.dp.toPx()
                                            drawIntoCanvas { canvas ->
                                                val paint = android.graphics.Paint().apply {
                                                    color = android.graphics.Color.TRANSPARENT
                                                    setShadowLayer(shadowRadius, 0f, 0f, shadowColor.toArgb())
                                                }
                                                val nativePath = android.graphics.Path().apply {
                                                    addRoundRect(
                                                        0f, 0f, size.width, size.height,
                                                        floatArrayOf(45f, 45f, 45f, 45f, 0f, 0f, 0f, 0f),
                                                        android.graphics.Path.Direction.CW
                                                    )
                                                }
                                                canvas.nativeCanvas.drawPath(nativePath, paint)
                                            }
                                        }
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                        )
                                        .padding(vertical = 20.dp, horizontal = 0.dp)
                                        .offset(0.dp, -12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isMenuExpanded = false; showSettingsDialog = true }
                                            .padding(vertical = 12.dp, horizontal = 28.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.menu_settings), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isMenuExpanded = false; showInfoDialog = true }
                                            .padding(vertical = 12.dp, horizontal = 28.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.menu_info), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isMenuExpanded = false; showHelpDialog = true }
                                            .padding(vertical = 12.dp, horizontal = 28.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.menu_help), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    if (showNotificationPermissionRationale) {
                        NotificationPermissionDialog(
                            onConfirm = {
                                showNotificationPermissionRationale = false
                                prefs.edit().putBoolean(AppConstants.PREFS_NOTIFICATION_PERMISSION_ASKED, true).apply()
                                notificationPermissionLauncher.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                )
                            },
                            onCancel = {
                                showNotificationPermissionRationale = false
                                prefs.edit().putBoolean(AppConstants.PREFS_NOTIFICATION_PERMISSION_ASKED, true).apply()
                            }
                        )
                    }

                    if (showLocationPermissionRationale) {
                        LocationPermissionDialog(
                            onConfirm = {
                                showLocationPermissionRationale = false
                                if (ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.ACCESS_FINE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    fineLocationLauncher.launch(arrayOf(
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                        android.Manifest.permission.ACCESS_FINE_LOCATION
                                    ))
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                    ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    requestBackgroundLocation()
                                }
                            },
                            onCancel = {
                                showLocationPermissionRationale = false
                                isAutomateExpanded = false
                            }
                        )
                    }

                    RemoteWaveOverlay(
                        visible = showSentWave,
                        color = activeAnimationColor,
                        onFinished = { showSentWave = false }
                    )

                    ConnectionWarningOverlay(isReady = isNetworkReady)
                }
            }
        }
    }
}
