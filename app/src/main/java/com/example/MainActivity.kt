package com.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.controller.AutomationEngine
import com.example.model.AspectRatioPreset
import com.example.model.AutomationConfig
import com.example.model.AutomationStatus
import com.example.model.CalibrationTargetType
import com.example.model.GenerationMode
import com.example.service.FloatingService
import com.example.ui.theme.MyApplicationTheme

// Modern Cyberpunk Dark Palette
val DarkNavyBg = Color(0xFF090D1A)
val SurfaceDark = Color(0xFF11182B)
val SurfaceDarkElevated = Color(0xFF19233D)
val BorderDark = Color(0xFF233254)
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFFBD00FF)
val EmeraldGreen = Color(0xFF10B981)
val ElectricBlue = Color(0xFF38BDF8)
val AmberOrange = Color(0xFFF59E0B)
val RoseRed = Color(0xFFF43F5E)

// Curated Preset Batch Model
data class PresetBatch(
    val id: String,
    val title: String,
    val category: String,
    val tag: String,
    val accentColor: Color,
    val icon: ImageVector,
    val description: String,
    val prompts: List<String>
)

val CuratedPresetBatches = listOf(
    PresetBatch(
        id = "scifi",
        title = "Cinematic Sci-Fi",
        category = "Sci-Fi",
        tag = "#CYBERPUNK",
        accentColor = NeonCyan,
        icon = Icons.Default.Movie,
        description = "High-octane neon landscapes, futuristic vehicles & space stations",
        prompts = listOf(
            "Ultra-realistic cinematic shot of a cyberpunk hovering speeder car in rainy Neo-Tokyo, 4K HDR, neon reflections on asphalt",
            "Sci-fi deep space station orbiting a giant ringed gas planet with glowing nebulas and hyper-drive starships",
            "Futuristic android waking up in a bioluminescent laboratory, volumetric lighting and floating hologram sparks"
        )
    ),
    PresetBatch(
        id = "nature",
        title = "Nature & Hyperlapse",
        category = "Nature",
        tag = "#NATURE_4K",
        accentColor = EmeraldGreen,
        icon = Icons.Default.Nature,
        description = "Breathtaking landscapes, macro botanicals & ocean biomes",
        prompts = listOf(
            "Cinematic drone view flying through majestic snowy mountain peaks at golden hour, misty clouds and sun rays",
            "Slow motion macro video of vibrant crystal raindrops splashing on a jungle lotus leaf, photorealistic 8K",
            "Bioluminescent ocean waves crashing against a dark rocky beach at midnight under starry aurora sky"
        )
    ),
    PresetBatch(
        id = "anime",
        title = "Cyberpunk Anime & Mecha",
        category = "Anime",
        tag = "#ANIME_MECHA",
        accentColor = NeonPurple,
        icon = Icons.Default.Bolt,
        description = "Dynamic Japanese anime sequences, mecha launch & stylized visuals",
        prompts = listOf(
            "Dynamic anime battle sequence of a giant robotic mecha launching thrusters over a neon illuminated metropolis",
            "Rain-slicked Neo-Shibuya alleyway with a wandering cyborg ronin holding glowing plasma katana, anime aesthetic",
            "Futuristic hologram street vendor preparing glowing ramen in a bustling underground bazaar, vivid cel-shaded style"
        )
    ),
    PresetBatch(
        id = "commercial",
        title = "Commercial & Luxury Ads",
        category = "Commercial",
        tag = "#COMMERCIAL",
        accentColor = AmberOrange,
        icon = Icons.Default.ShoppingCart,
        description = "Premium product showcase, zero-gravity rotations & cinematic lighting",
        prompts = listOf(
            "Luxury titanium wristwatch rotating smoothly in zero gravity surrounded by floating crystal water droplets, studio softbox lighting",
            "Futuristic high-top sneaker assembling itself from illuminated neon ribbons, dramatic dark studio lighting and smoke",
            "Elegant obsidian perfume bottle in swirling iridescent mist with subtle golden particles, photorealistic 8K"
        )
    )
)

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
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val rawText = inputStream.bufferedReader().use { it.readText() }
                    AutomationEngine.loadPrompts(rawText)
                }
            } catch (e: Exception) {
                AutomationEngine.log("File import error: ${e.message}")
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Follow AI Auto",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (state.isOverlayRunning) EmeraldGreen.copy(alpha = 0.2f) else BorderDark,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (state.isOverlayRunning) EmeraldGreen else Color.Gray,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (state.isOverlayRunning) "LIVE" else "STANDBY",
                                        color = if (state.isOverlayRunning) EmeraldGreen else Color.LightGray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Bulk Prompt Injection & Video Automation",
                                fontSize = 11.sp,
                                color = ElectricBlue
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
                        color = NeonCyan,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Dashboard", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Settings & Calib", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_settings")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("How To Use", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
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
                        .widthIn(max = 620.dp)
                ) {
                    when (selectedTab) {
                        0 -> DashboardTab(
                            hasOverlayPermission = hasOverlayPermission,
                            isAccessibilityConnected = state.isAccessibilityConnected,
                            hasNotificationPermission = hasNotificationPermission,
                            isOverlayRunning = state.isOverlayRunning,
                            onRequestOverlayPermission = {
                                val overlayIntent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(overlayIntent)
                            },
                            onRequestAccessibility = {
                                val accessibilityIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(accessibilityIntent)
                            },
                            onRequestNotification = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onToggleOverlay = {
                                if (state.isOverlayRunning) {
                                    val stopIntent = Intent(context, FloatingService::class.java).apply {
                                        action = FloatingService.ACTION_STOP
                                    }
                                    context.startService(stopIntent)
                                } else {
                                    val startIntent = Intent(context, FloatingService::class.java).apply {
                                        action = FloatingService.ACTION_START
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(startIntent)
                                    } else {
                                        context.startService(startIntent)
                                    }
                                }
                            },
                            onImportFile = {
                                filePickerLauncher.launch("*/*")
                            },
                            onNavigateToGuide = {
                                selectedTab = 2
                            }
                        )

                        1 -> SettingsAndCalibrationTab()
                        2 -> HowToUseTab(
                            hasOverlayPermission = hasOverlayPermission,
                            isAccessibilityConnected = state.isAccessibilityConnected,
                            isOverlayRunning = state.isOverlayRunning,
                            onRequestPermissions = { selectedTab = 0 },
                            onLaunchOverlay = {
                                val startIntent = Intent(context, FloatingService::class.java).apply {
                                    action = FloatingService.ACTION_START
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(startIntent)
                                } else {
                                    context.startService(startIntent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// MODULE 1: MODERN HERO DASHBOARD
// -----------------------------------------------------------------------------

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
    onImportFile: () -> Unit,
    onNavigateToGuide: () -> Unit
) {
    val state by AutomationEngine.state.collectAsState()
    val allPermissionsReady = hasOverlayPermission && isAccessibilityConnected

    // Pulse animation for active overlay state
    val infiniteTransition = rememberInfiniteTransition(label = "hero_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Focal Card: Master Launcher & Live Telemetry
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_launcher_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        if (isOverlayRunning) {
                            listOf(EmeraldGreen.copy(alpha = pulseAlpha), NeonCyan.copy(alpha = pulseAlpha))
                        } else if (!allPermissionsReady) {
                            listOf(AmberOrange.copy(alpha = 0.7f), BorderDark)
                        } else {
                            listOf(NeonCyan, NeonPurple)
                        }
                    ),
                    width = if (isOverlayRunning) 2.dp else 1.5.dp
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header Row with glowing radar beacon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = when {
                                                isOverlayRunning -> EmeraldGreen
                                                !allPermissionsReady -> AmberOrange
                                                else -> ElectricBlue
                                            },
                                            shape = CircleShape
                                        )
                                        .border(
                                            2.dp,
                                            if (isOverlayRunning) EmeraldGreen.copy(alpha = pulseAlpha) else Color.Transparent,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isOverlayRunning -> "ENGINE ACTIVE & RUNNING"
                                        !allPermissionsReady -> "SETUP REQUIRED"
                                        else -> "ENGINE STANDBY — READY"
                                    },
                                    color = when {
                                        isOverlayRunning -> EmeraldGreen
                                        !allPermissionsReady -> AmberOrange
                                        else -> ElectricBlue
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isOverlayRunning) "Floating Bubble Active" else "Follow AI Overlay Controller",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isOverlayRunning) EmeraldGreen.copy(alpha = 0.15f) else SurfaceDarkElevated,
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isOverlayRunning) EmeraldGreen else BorderDark,
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isOverlayRunning) "ON SCREEN" else "IDLE",
                                color = if (isOverlayRunning) EmeraldGreen else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isOverlayRunning)
                            "The draggable bubble is active over Kiwi Browser / Chrome. Tap it to expand controls, inject prompts, and automate generation."
                        else
                            "Launch the floating control panel to inject prompts automatically, toggle aspect ratios, and auto-download completed videos.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Live Telemetry Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkNavyBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("STATUS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(state.status.name, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderDark))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("QUEUE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${state.completedCount}/${state.totalCount}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderDark))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DELAY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${state.config.delaySeconds}s", color = AmberOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderDark))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RATIO", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(state.config.aspectRatio.label, color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Hero Button
                    Button(
                        onClick = onToggleOverlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("toggle_overlay_button"),
                        enabled = allPermissionsReady || isOverlayRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayRunning) RoseRed else NeonCyan,
                            disabledContainerColor = SurfaceDarkElevated
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOverlayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isOverlayRunning) Color.White else Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isOverlayRunning) "Stop Floating Overlay" else "Launch Floating Overlay",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverlayRunning) Color.White else Color.Black
                            )
                        }
                    }

                    if (!allPermissionsReady && !isOverlayRunning) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Grant Overlay and Accessibility permissions below to enable the engine.",
                                color = AmberOrange,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Permissions Checklist Section
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Permissions",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (allPermissionsReady) "All Active ✓" else "Attention Required",
                        color = if (allPermissionsReady) EmeraldGreen else AmberOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Permission 1: Overlay
        item {
            GlowingPermissionCard(
                title = "Display Over Other Apps",
                description = "Required to render the draggable floating control bubble over Kiwi/Chrome.",
                isGranted = hasOverlayPermission,
                actionLabel = "Grant Overlay Access",
                onAction = onRequestOverlayPermission,
                icon = Icons.Default.Layers,
                tag = "overlay_permission_card"
            )
        }

        // Permission 2: Accessibility
        item {
            GlowingPermissionCard(
                title = "Follow AI Accessibility Engine",
                description = "Automates prompt typing, button clicking, and rendering status checks without root.",
                isGranted = isAccessibilityConnected,
                actionLabel = "Enable in Accessibility Settings",
                onAction = onRequestAccessibility,
                icon = Icons.Default.Accessibility,
                tag = "accessibility_permission_card"
            )
        }

        // Permission 3: Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            item {
                GlowingPermissionCard(
                    title = "Post Notifications",
                    description = "Maintains the foreground overlay service in memory and alerts you when batch completes.",
                    isGranted = hasNotificationPermission,
                    actionLabel = "Allow Notifications",
                    onAction = onRequestNotification,
                    icon = Icons.Default.Notifications,
                    tag = "notifications_permission_card"
                )
            }
        }

        // Quick Preset Batch Access Banner on Dashboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NeonPurple.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.5f)))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Preset Loaders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onImportFile,
                                modifier = Modifier.testTag("main_import_file_button")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import File", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = onNavigateToGuide) {
                                Text("All Presets →", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CuratedPresetBatches) { preset ->
                            Box(
                                modifier = Modifier
                                    .background(DarkNavyBg, RoundedCornerShape(10.dp))
                                    .border(1.dp, preset.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        AutomationEngine.loadPrompts(preset.prompts.joinToString("\n"))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(preset.icon, contentDescription = null, tint = preset.accentColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(preset.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("+ Load", color = preset.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Activity Logs Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_logs_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Activity Console (${state.logs.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = { AutomationEngine.clearLogs() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear logs", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.logs.isEmpty()) {
                        Text(
                            text = "No engine activity yet. Launch overlay and tap Start Queue to begin.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            state.logs.takeLast(5).reversed().forEach { log ->
                                Text(
                                    text = log,
                                    color = EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GlowingPermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    icon: ImageVector,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isGranted) {
                Brush.horizontalGradient(listOf(EmeraldGreen.copy(alpha = 0.6f), BorderDark))
            } else {
                Brush.horizontalGradient(listOf(AmberOrange.copy(alpha = 0.6f), BorderDark))
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isGranted) EmeraldGreen.copy(alpha = 0.15f) else AmberOrange.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (isGranted) EmeraldGreen.copy(alpha = 0.5f) else AmberOrange.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) EmeraldGreen else AmberOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Verification Pill Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (isGranted) EmeraldGreen.copy(alpha = 0.2f) else AmberOrange.copy(alpha = 0.2f),
                            RoundedCornerShape(50.dp)
                        )
                        .border(
                            1.dp,
                            if (isGranted) EmeraldGreen else AmberOrange,
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "✓ GRANTED" else "SETUP",
                        color = if (isGranted) EmeraldGreen else AmberOrange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("${tag}_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(actionLabel, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// MODULE 2: HOW TO USE TAB WITH GLOWING STEPPER & EXPANDABLE PRESET CARDS
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseTab(
    hasOverlayPermission: Boolean,
    isAccessibilityConnected: Boolean,
    isOverlayRunning: Boolean,
    onRequestPermissions: () -> Unit,
    onLaunchOverlay: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedBatchId by remember { mutableStateOf<String?>("scifi") }
    val context = LocalContext.current

    val categories = listOf("All", "Sci-Fi", "Nature", "Anime", "Commercial")
    val filteredBatches = if (selectedCategory == "All") {
        CuratedPresetBatches
    } else {
        CuratedPresetBatches.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Interactive Glowing Stepper
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.4f)))
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
                                text = "AUTOMATION ROADMAP",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "6-Step Quick Start Guide",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(DarkNavyBg, RoundedCornerShape(50.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Follow AI Ready", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlowingStepperItem(
                        stepNumber = "1",
                        milestoneTag = "STEP 01",
                        title = "Grant Required Permissions",
                        description = "Enable 'Display Over Other Apps' and 'Follow AI Accessibility Engine' in system settings.",
                        accentColor = NeonCyan,
                        isComplete = hasOverlayPermission && isAccessibilityConnected,
                        actionText = if (!(hasOverlayPermission && isAccessibilityConnected)) "Configure Now" else null,
                        onAction = onRequestPermissions
                    )

                    GlowingStepperItem(
                        stepNumber = "2",
                        milestoneTag = "STEP 02",
                        title = "Launch Floating Overlay",
                        description = "Tap 'Launch Floating Overlay' on the dashboard. A draggable bubble appears immediately.",
                        accentColor = ElectricBlue,
                        isComplete = isOverlayRunning,
                        actionText = if (!isOverlayRunning) "Launch Overlay" else null,
                        onAction = onLaunchOverlay
                    )

                    GlowingStepperItem(
                        stepNumber = "3",
                        milestoneTag = "STEP 03",
                        title = "Open Follow AI in Browser",
                        description = "Open Kiwi Browser or Chrome, log in to follow.ai, and navigate to the Video Generator page.",
                        accentColor = NeonPurple,
                        isComplete = false,
                        actionText = "Open follow.ai",
                        onAction = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://follow.ai"))
                            context.startActivity(browserIntent)
                        }
                    )

                    GlowingStepperItem(
                        stepNumber = "4",
                        milestoneTag = "STEP 04",
                        title = "Input Bulk Prompts or Presets",
                        description = "Expand the floating bubble, paste your prompts (one per line), or tap any preset batch below.",
                        accentColor = EmeraldGreen,
                        isComplete = false
                    )

                    GlowingStepperItem(
                        stepNumber = "5",
                        milestoneTag = "STEP 05",
                        title = "Configure Render Delay & Ratio",
                        description = "Choose 30s-60s render delay to match server processing and select aspect ratio (16:9 / 9:16).",
                        accentColor = AmberOrange,
                        isComplete = false
                    )

                    GlowingStepperItem(
                        stepNumber = "6",
                        milestoneTag = "STEP 06",
                        title = "Tap 'Start Queue' & Relax",
                        description = "The engine injects prompts, clicks Generate, handles smart timeouts, and verifies auto-downloads.",
                        accentColor = RoseRed,
                        isComplete = false,
                        isLast = true
                    )
                }
            }
        }

        // Section 2: Preset Prompt Batches Header & Filter Chips
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURATED BATCHES",
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Preset Prompt Library",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(SurfaceDarkElevated, RoundedCornerShape(50.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(50.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${CuratedPresetBatches.size} Batches", color = Color.LightGray, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Modern Quick-Chip Filter Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = SurfaceDark,
                                labelColor = Color.LightGray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = BorderDark,
                                selectedBorderColor = NeonCyan
                            )
                        )
                    }
                }
            }
        }

        // Section 3: Glowing Preset Cards with Accordion
        items(filteredBatches) { batch ->
            val isExpanded = expandedBatchId == batch.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedBatchId = if (isExpanded) null else batch.id
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(batch.accentColor.copy(alpha = if (isExpanded) 0.8f else 0.4f), BorderDark)
                    ),
                    width = if (isExpanded) 1.5.dp else 1.dp
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(batch.accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, batch.accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(batch.icon, contentDescription = null, tint = batch.accentColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = batch.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(DarkNavyBg, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(batch.tag, color = batch.accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(batch.description, color = Color.Gray, fontSize = 10.sp)
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color.LightGray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${batch.prompts.size} Prompts Ready",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Glowing Neon-Accent Pill Button for "Load to Queue"
                        Button(
                            onClick = {
                                AutomationEngine.loadPrompts(batch.prompts.joinToString("\n"))
                                Toast.makeText(context, "Loaded ${batch.prompts.size} prompts into queue!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = batch.accentColor),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Load to Queue",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Accordion Collapsible Prompts List
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            HorizontalDivider(color = BorderDark, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            batch.prompts.forEachIndexed { idx, promptText ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(DarkNavyBg, RoundedCornerShape(10.dp))
                                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            color = batch.accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = promptText,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Prompt", promptText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied prompt #${idx + 1}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GlowingStepperItem(
    stepNumber: String,
    milestoneTag: String,
    title: String,
    description: String,
    accentColor: Color,
    isComplete: Boolean = false,
    isLast: Boolean = false,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Milestone Circular Badge and Vertical Connector Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isComplete) EmeraldGreen.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.15f),
                        CircleShape
                    )
                    .border(
                        1.5.dp,
                        if (isComplete) EmeraldGreen else accentColor,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isComplete) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = stepNumber,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isComplete) EmeraldGreen else accentColor.copy(alpha = 0.5f),
                                    BorderDark
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = milestoneTag,
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (isComplete) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• DONE ✓",
                        color = EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(accentColor, NeonPurple))
                    )
                ) {
                    Text(actionText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// MODULE 3: STRUCTURED SETTINGS & CALIBRATION TAB
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calibration Target Cards
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coordinate_calibration_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), BorderDark))
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
                                text = "SCREEN TOUCH CALIBRATION",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Target Coordinates",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(DarkNavyBg, RoundedCornerShape(50.dp))
                                .border(1.dp, BorderDark, RoundedCornerShape(50.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("DOM Fallback", color = ElectricBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Precise screen coordinates for tap and swipe injection when browser WebViews restrict direct accessibility node input.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CalibrationTargetType.values().forEach { target ->
                        val pt = state.calibrationData.getPoint(target)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkNavyBg),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = if (pt.isCalibrated) {
                                    Brush.horizontalGradient(listOf(EmeraldGreen.copy(alpha = 0.5f), BorderDark))
                                } else {
                                    Brush.horizontalGradient(listOf(BorderDark, BorderDark))
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = target.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (pt.isCalibrated) EmeraldGreen.copy(alpha = 0.2f) else SurfaceDarkElevated,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (pt.isCalibrated) EmeraldGreen else Color.Gray,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (pt.isCalibrated) "✓ CALIBRATED" else "DEFAULT",
                                                color = if (pt.isCalibrated) EmeraldGreen else Color.LightGray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Coordinates: (${pt.x.toInt()}px, ${pt.y.toInt()}px)",
                                        color = if (pt.isCalibrated) EmeraldGreen else ElectricBlue,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = target.description,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            AutomationEngine.startCalibration(target)
                                        },
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                    ) {
                                        Text("Calibrate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            AutomationEngine.testTapTarget(target)
                                        },
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("Test Tap", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Smart Safety & Detection Group
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_detection_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NeonPurple.copy(alpha = 0.5f), BorderDark))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SAFETY & RECOVERY PROTOCOLS",
                        color = NeonPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Smart Polling & Modal Guard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    GlowingToggleRow(
                        title = "Smart Button Polling",
                        subtitle = "Checks if Generate button is disabled or rendering before clicking to prevent spamming",
                        checked = smartPolling,
                        onCheckedChange = { smartPolling = it },
                        accentColor = NeonCyan,
                        tag = "main_smart_polling_switch"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GlowingToggleRow(
                        title = "Captcha & Auth Modal Guard",
                        subtitle = "Auto-pauses queue and triggers alert when bot verification or login modal appears",
                        checked = modalDetect,
                        onCheckedChange = { modalDetect = it },
                        accentColor = NeonPurple,
                        tag = "main_modal_detect_switch"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GlowingToggleRow(
                        title = "Force Coordinate Tap Fallback",
                        subtitle = "Bypasses accessibility DOM tree inspection and executes direct calibrated path taps",
                        checked = useCoords,
                        onCheckedChange = { useCoords = it },
                        accentColor = AmberOrange,
                        tag = "main_use_coords_switch"
                    )
                }
            }
        }

        // Timing & Parameter Configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.5f), BorderDark))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ENGINE PARAMETERS",
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Render Delay & Search Keywords",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Render Delay Interval: ${delaySec}s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90, 120).forEach { s ->
                            val isSelected = delaySec == s
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(
                                        if (isSelected) NeonCyan else DarkNavyBg,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan else BorderDark,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { delaySec = s },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${s}s",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Prompt Field Keywords (comma-separated):", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = inKeywords,
                        onValueChange = { inKeywords = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_keywords_field"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = DarkNavyBg,
                            unfocusedContainerColor = DarkNavyBg
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Generate Button Keywords (comma-separated):", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = genKeywords,
                        onValueChange = { genKeywords = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_keywords_field"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = DarkNavyBg,
                            unfocusedContainerColor = DarkNavyBg
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

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
                            Toast.makeText(context, "Saved configuration successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Configuration", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GlowingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkNavyBg, RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (checked) accentColor.copy(alpha = 0.5f) else BorderDark,
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF94A3B8), fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = SurfaceDark
            )
        )
    }
}
