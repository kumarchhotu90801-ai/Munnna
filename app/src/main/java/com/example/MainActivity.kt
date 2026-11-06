package com.example

import android.Manifest
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.controller.AutomationEngine
import com.example.model.AutomationConfig
import com.example.service.FloatingService
import com.example.ui.overlay.BorderDark
import com.example.ui.overlay.DarkNavyBg
import com.example.ui.overlay.EmeraldGreen
import com.example.ui.overlay.NeonCyan
import com.example.ui.overlay.NeonPurple
import com.example.ui.overlay.RoseRed
import com.example.ui.overlay.SurfaceDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AutomationEngine.initialize(this)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by AutomationEngine.state.collectAsState()

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    val raw = reader.readText()
                    AutomationEngine.loadPrompts(raw)
                }
            } catch (e: Exception) {
                AutomationEngine.log("Error importing file: ${e.message}")
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // Handle import trigger from Floating Overlay
    val activity = context as? ComponentActivity
    val intent = activity?.intent
    DisposableEffect(intent) {
        if (intent?.getBooleanExtra("ACTION_IMPORT_PROMPTS", false) == true) {
            intent.removeExtra("ACTION_IMPORT_PROMPTS")
            filePickerLauncher.launch("*/*")
        }
        onDispose {}
    }

    // Refresh permission states on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Follow AI Auto",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Bulk Video Generation Engine",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkNavyBg
                )
            )
        },
        containerColor = DarkNavyBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkNavyBg,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Settings & Calib", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_settings")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("How To Use", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_guide")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp)
                ) {
                    when (selectedTab) {
                        0 -> DashboardTab(
                            hasOverlayPermission = hasOverlayPermission,
                            isAccessibilityConnected = state.isAccessibilityConnected,
                            hasNotificationPermission = hasNotificationPermission,
                            isOverlayRunning = state.isOverlayRunning,
                            onRequestOverlayPermission = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            onRequestAccessibility = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            onRequestNotification = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onToggleOverlay = {
                                if (state.isOverlayRunning) {
                                    val intent = Intent(context, FloatingService::class.java).apply {
                                        action = FloatingService.ACTION_STOP
                                    }
                                    context.startService(intent)
                                } else {
                                    val intent = Intent(context, FloatingService::class.java).apply {
                                        action = FloatingService.ACTION_START
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            },
                            onImportFile = {
                                filePickerLauncher.launch("*/*")
                            }
                        )

                        1 -> SettingsAndCalibrationTab()
                        2 -> HowToUseTab()
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    hasOverlayPermission: Boolean,
    isAccessibilityConnected: Boolean,
    hasNotificationPermission: Boolean,
    isOverlayRunning: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestNotification: () -> Unit,
    onToggleOverlay: () -> Unit,
    onImportFile: () -> Unit
) {
    val state by AutomationEngine.state.collectAsState()
    val allPermissionsReady = hasOverlayPermission && isAccessibilityConnected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Module 6: Resume Unfinished Queue Card
        if (state.hasSavedHistory && state.status == com.example.model.AutomationStatus.IDLE) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("main_resume_history_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonPurple))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unfinished Queue Found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Resume progress from prompt #${state.savedHistoryIndex} of ${state.savedHistoryTotal}", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Row {
                            OutlinedButton(
                                onClick = { AutomationEngine.clearSavedHistory() },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Discard", fontSize = 10.sp, color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { AutomationEngine.resumeFromSavedHistory() },
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Resume", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Module 6 & 7: Mode Presets & File Importer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("mode_presets_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Presets & Bulk File Import", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Button(
                            onClick = onImportFile,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp).testTag("main_import_file_button")
                        ) {
                            Text("Import .txt / .json", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Generation Mode:", color = Color.White, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.example.model.GenerationMode.values().forEach { mode ->
                            androidx.compose.material3.FilterChip(
                                selected = state.config.generationMode == mode,
                                onClick = { AutomationEngine.updateConfig(state.config.copy(generationMode = mode)) },
                                label = { Text(mode.displayName, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Aspect Ratio Lock:", color = Color.White, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        com.example.model.AspectRatioPreset.values().forEach { ratio ->
                            androidx.compose.material3.FilterChip(
                                selected = state.config.aspectRatio == ratio,
                                onClick = { AutomationEngine.updateConfig(state.config.copy(aspectRatio = ratio)) },
                                label = { Text(ratio.label, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        // Master Launcher Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("master_launcher_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (isOverlayRunning) EmeraldGreen else NeonCyan,
                            NeonPurple
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isOverlayRunning) "Overlay Active" else "Overlay Inactive",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = if (isOverlayRunning)
                                    "Floating panel ready over Kiwi/Chrome"
                                else
                                    "Launch floating panel to automate video generation",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (isOverlayRunning) EmeraldGreen else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onToggleOverlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("toggle_overlay_button"),
                        enabled = allPermissionsReady || isOverlayRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayRunning) RoseRed else EmeraldGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOverlayRunning) "Stop Floating Overlay" else "Launch Floating Overlay",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!allPermissionsReady && !isOverlayRunning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ Please grant Overlay and Accessibility permissions below to enable the launcher.",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Permissions Checklist Section
        item {
            Text(
                text = "Required Permissions",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Permission 1: Overlay
        item {
            PermissionCard(
                title = "Display Over Other Apps",
                description = "Required to show the draggable floating bubble and control panel over your mobile browser.",
                isGranted = hasOverlayPermission,
                actionLabel = "Grant Permission",
                onAction = onRequestOverlayPermission,
                icon = Icons.Default.Layers,
                tag = "overlay_permission_card"
            )
        }

        // Permission 2: Accessibility
        item {
            PermissionCard(
                title = "Follow AI Accessibility Engine",
                description = "Enables automated prompt typing and clicking the Generate button on follow.ai.",
                isGranted = isAccessibilityConnected,
                actionLabel = "Enable in Settings",
                onAction = onRequestAccessibility,
                icon = Icons.Default.Accessibility,
                tag = "accessibility_permission_card"
            )
        }

        // Permission 3: Notifications (if Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                PermissionCard(
                    title = "Post Notifications",
                    description = "Keeps the floating overlay service alive in the background and notifies on queue completion.",
                    isGranted = hasNotificationPermission,
                    actionLabel = "Allow Notifications",
                    onAction = onRequestNotification,
                    icon = Icons.Default.Notifications,
                    tag = "notifications_permission_card"
                )
            }
        }

        // Live Queue Status summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("queue_summary_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Engine & Queue Status",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Status: ${state.status.name}", color = Color.White, fontSize = 12.sp)
                        Text(text = "Delay: ${state.config.delaySeconds}s", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Current: ${state.currentStepMessage}",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Recent Activity Logs
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("recent_logs_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Activity Logs (${state.logs.size})",
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        IconButton(
                            onClick = { AutomationEngine.clearLogs() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear logs", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (state.logs.isEmpty()) {
                        Text(
                            text = "No engine activity yet. Launch overlay and start queue to see live logs.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            state.logs.takeLast(6).reversed().forEach { log ->
                                Text(
                                    text = log,
                                    color = Color.Green.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isGranted) EmeraldGreen.copy(alpha = 0.5f) else BorderDark
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) EmeraldGreen else NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = description,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = if (isGranted) "Granted" else "Required",
                    tint = if (isGranted) EmeraldGreen else Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("${tag}_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(actionLabel, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsAndCalibrationTab() {
    val state by AutomationEngine.state.collectAsState()
    var delaySec by remember(state.config.delaySeconds) { mutableIntStateOf(state.config.delaySeconds) }
    var maxTimeoutSec by remember(state.config.maxTimeoutSeconds) { mutableIntStateOf(state.config.maxTimeoutSeconds) }
    var inKeywords by remember(state.config.inputKeywords) { mutableStateOf(state.config.inputKeywords) }
    var genKeywords by remember(state.config.generateKeywords) { mutableStateOf(state.config.generateKeywords) }
    var useCoords by remember(state.config.useCoordinates) { mutableStateOf(state.config.useCoordinates) }
    var smartPolling by remember(state.config.smartPollingEnabled) { mutableStateOf(state.config.smartPollingEnabled) }
    var modalDetect by remember(state.config.modalDetectionEnabled) { mutableStateOf(state.config.modalDetectionEnabled) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Module 3: 4 Calibrated Target Coordinates
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("coordinate_calibration_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Calibrated Screen Targets",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Fallback touch coordinates for browser WebViews (tap and swipe simulation).",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    com.example.model.CalibrationTargetType.values().forEach { target ->
                        val pt = state.calibrationData.getPoint(target)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkNavyBg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = target.displayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (pt.isCalibrated) "Calibrated: (${pt.x.toInt()}, ${pt.y.toInt()})" else "Default: (${pt.x.toInt()}, ${pt.y.toInt()})",
                                        color = if (pt.isCalibrated) EmeraldGreen else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = target.description,
                                        color = Color.LightGray,
                                        fontSize = 9.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            AutomationEngine.startCalibration(target)
                                        },
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Calibrate", fontSize = 10.sp, color = NeonCyan)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            AutomationEngine.testTapTarget(target)
                                        },
                                        modifier = Modifier.height(32.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Test Tap", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Module 4: Smart Polling & Detection
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("smart_detection_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Smart State Detection & Safety",
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Poll Generate button readiness and auto-pause on Captcha or Login modals.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Button Polling", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Wait until Generate is not busy/rendering before clicking", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = smartPolling,
                            onCheckedChange = { smartPolling = it },
                            modifier = Modifier.testTag("main_smart_polling_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Captcha / Login Auto-Pause", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Automatically pause queue when verification modal appears", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = modalDetect,
                            onCheckedChange = { modalDetect = it },
                            modifier = Modifier.testTag("main_modal_detect_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Force Coordinate Tap Fallback", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Bypass DOM tree search and always use calibrated taps", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = useCoords,
                            onCheckedChange = { useCoords = it },
                            modifier = Modifier.testTag("main_use_coords_switch")
                        )
                    }
                }
            }
        }

        // Target Selectors & Timeouts
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Keywords & Timing",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Prompt Field Keywords (comma-separated):", color = Color.White, fontSize = 12.sp)
                    OutlinedTextField(
                        value = inKeywords,
                        onValueChange = { inKeywords = it },
                        modifier = Modifier.fillMaxWidth().testTag("input_keywords_field"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Generate Button Keywords (comma-separated):", color = Color.White, fontSize = 12.sp)
                    OutlinedTextField(
                        value = genKeywords,
                        onValueChange = { genKeywords = it },
                        modifier = Modifier.fillMaxWidth().testTag("generate_keywords_field"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Video Render Delay: ${delaySec}s", color = Color.White, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90, 120).forEach { s ->
                            OutlinedButton(
                                onClick = { delaySec = s },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (delaySec == s) NeonCyan else Color.Transparent
                                )
                            ) {
                                Text(
                                    "${s}s",
                                    fontSize = 10.sp,
                                    color = if (delaySec == s) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val newConfig = AutomationConfig(
                                delaySeconds = delaySec,
                                maxTimeoutSeconds = maxTimeoutSec,
                                inputKeywords = inKeywords,
                                generateKeywords = genKeywords,
                                useCoordinates = useCoords,
                                smartPollingEnabled = smartPolling,
                                modalDetectionEnabled = modalDetect
                            )
                            AutomationEngine.updateConfig(newConfig)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Configuration", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HowToUseTab() {
    val sampleBatches = listOf(
        "Cinematic Sci-Fi" to listOf(
            "Ultra-realistic cinematic shot of a cyberpunk hovering speeder car in rainy Neo-Tokyo, 4K HDR",
            "Sci-fi deep space station orbiting a giant ringed gas planet with glowing nebulas",
            "Futuristic android waking up in a bioluminescent laboratory, volumetric lighting"
        ),
        "Nature & Scenery" to listOf(
            "Cinematic drone view flying through majestic snowy mountain peaks at golden hour",
            "Slow motion macro video of vibrant crystal raindrops splashing on a jungle lotus leaf",
            "Bioluminescent ocean waves crashing against a dark rocky beach at midnight under starry sky"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Start Guide for Follow AI",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StepItem("1", "Grant Required Permissions", "Enable 'Display Over Other Apps' and activate 'Follow AI Automation Engine' in Android Accessibility settings.")
                    StepItem("2", "Launch Floating Overlay", "Tap 'Launch Floating Overlay' in the Dashboard tab. A draggable bubble icon appears.")
                    StepItem("3", "Open Mobile Browser", "Open Kiwi Browser or Chrome, navigate to follow.ai, and go to the Video Creation workspace.")
                    StepItem("4", "Paste Bulk Prompts", "Tap the floating bubble to expand the control panel. Paste your prompts (one per line) or use a sample preset.")
                    StepItem("5", "Set Render Delay", "Select how many seconds to wait between generations (e.g. 30s or 60s) to give the server rendering time.")
                    StepItem("6", "Tap 'Start Queue'", "The accessibility engine automatically locates the prompt input box, pastes each prompt, and clicks the Generate button.")
                }
            }
        }

        item {
            Text(
                text = "Preset Prompt Batches",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        items(sampleBatches) { (title, prompts) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Button(
                            onClick = {
                                AutomationEngine.loadPrompts(prompts.joinToString("\n"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Load to Queue", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    prompts.forEachIndexed { i, p ->
                        Text(
                            text = "${i + 1}. $p",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepItem(num: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = num, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = description, color = Color.LightGray, fontSize = 11.sp)
        }
    }
}
