package com.example.ui.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.controller.AutomationEngine
import com.example.model.ActionNeededReason
import com.example.model.AspectRatioPreset
import com.example.model.AutomationStatus
import com.example.model.CalibrationTargetType
import com.example.model.GenerationMode

val NeonCyan = Color(0xFF06B6D4)
val NeonPurple = Color(0xFF8B5CF6)
val DarkNavyBg = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val BorderDark = Color(0xFF334155)
val EmeraldGreen = Color(0xFF10B981)
val AmberOrange = Color(0xFFF59E0B)
val RoseRed = Color(0xFFEF4444)

@Composable
fun FloatingBubble(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit
) {
    val state by AutomationEngine.state.collectAsState()

    val pulseColor by animateColorAsState(
        targetValue = when {
            state.isActionNeeded -> RoseRed
            state.status == AutomationStatus.RUNNING -> EmeraldGreen
            state.status == AutomationStatus.PAUSED -> AmberOrange
            state.status == AutomationStatus.ERROR -> RoseRed
            else -> NeonCyan
        },
        label = "bubbleStatusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isActionNeeded || state.status == AutomationStatus.RUNNING) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state.isActionNeeded) 400 else 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier
            .scale(pulseScale)
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onExpand)
            .border(2.5.dp, pulseColor, CircleShape)
            .testTag("floating_bubble"),
        color = DarkNavyBg,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Circular progress ring around bubble
            if (state.totalCount > 0) {
                CircularProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.size(60.dp),
                    strokeWidth = 3.dp,
                    color = NeonCyan,
                    trackColor = BorderDark.copy(alpha = 0.4f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (state.isActionNeeded) Icons.Default.Warning else Icons.Default.VideoLibrary,
                    contentDescription = "Expand Overlay Panel",
                    tint = pulseColor,
                    modifier = Modifier.size(24.dp)
                )

                if (state.isActionNeeded) {
                    Text(
                        text = "ACTION",
                        color = RoseRed,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else if (state.totalCount > 0) {
                    Text(
                        text = "[${state.completedCount}/${state.totalCount}]",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.progressPercent}%",
                        color = NeonCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingControlPanel(
    modifier: Modifier = Modifier,
    onMinimize: () -> Unit,
    onCloseService: () -> Unit
) {
    val context = LocalContext.current
    val state by AutomationEngine.state.collectAsState()

    var rawPromptsText by remember {
        mutableStateOf(
            if (state.prompts.isNotEmpty()) state.prompts.joinToString("\n") { it.text } else ""
        )
    }

    var showPresets by remember { mutableStateOf(false) }
    var showCalibration by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .width(365.dp)
            .testTag("floating_control_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkNavyBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when {
                                    state.isActionNeeded -> RoseRed
                                    state.status == AutomationStatus.RUNNING -> EmeraldGreen
                                    state.status == AutomationStatus.PAUSED -> AmberOrange
                                    state.status == AutomationStatus.ERROR -> RoseRed
                                    else -> NeonCyan
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Follow AI Auto",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(SurfaceDark, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${state.config.generationMode.shortName} • ${state.config.aspectRatio.label}",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier.size(32.dp).testTag("minimize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Minimize overlay",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onCloseService,
                        modifier = Modifier.size(32.dp).testTag("close_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close overlay",
                            tint = RoseRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Action Needed Banner
            if (state.isActionNeeded) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("action_needed_banner"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseRed.copy(alpha = 0.2f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RoseRed))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RoseRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTION NEEDED IN BROWSER",
                                color = RoseRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.actionNeededMessage ?: "Captcha or Login detected. Please solve in browser.",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { AutomationEngine.clearActionNeededAndResume() },
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("solve_and_resume_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Solved! Resume Queue", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Resume Queue Banner (Browser reload / session recovery)
            if (state.hasSavedHistory && state.status == AutomationStatus.IDLE) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("resume_history_card"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonPurple.copy(alpha = 0.2f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonPurple))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unfinished Queue Found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Resume from #${state.savedHistoryIndex} of ${state.savedHistoryTotal}", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Row {
                            OutlinedButton(
                                onClick = { AutomationEngine.clearSavedHistory() },
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Discard", fontSize = 9.sp, color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    AutomationEngine.resumeFromSavedHistory()
                                    rawPromptsText = AutomationEngine.state.value.prompts.joinToString("\n") { it.text }
                                },
                                modifier = Modifier.height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Resume", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Banner with Circular Progress and Counter
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.status.name,
                                color = when (state.status) {
                                    AutomationStatus.RUNNING -> EmeraldGreen
                                    AutomationStatus.PAUSED -> AmberOrange
                                    AutomationStatus.ERROR -> RoseRed
                                    else -> NeonCyan
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (state.isGenerateButtonBusy) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 2.dp,
                                    color = NeonCyan
                                )
                            }
                        }

                        Text(
                            text = "[${state.completedCount}/${state.totalCount} - ${state.progressPercent}%]",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonCyan,
                        trackColor = BorderDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.currentStepMessage,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-line Prompt Input Header & File Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prompts Queue (${state.totalCount}):",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row {
                    Text(
                        text = "📁 Import .txt/.json",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                // Launch MainActivity file picker intent
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("ACTION_IMPORT_PROMPTS", true)
                                }
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("import_file_button")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "✨ Samples",
                        color = NeonPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                val samples = listOf(
                                    "Cinematic aerial shot of cyberpunk neon metropolis at night, 4k",
                                    "Hyper-realistic slow motion drop of water falling into ocean, glowing ripples",
                                    "Sci-fi astronaut discovering ancient alien ruins on Mars with glowing crystals"
                                ).joinToString("\n")
                                rawPromptsText = samples
                                AutomationEngine.loadPrompts(samples)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = rawPromptsText,
                onValueChange = {
                    rawPromptsText = it
                    AutomationEngine.loadPrompts(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .testTag("bulk_prompts_input"),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                placeholder = {
                    Text(
                        "Paste prompts (1 per line) or import .txt/.json:\n1. Cyberpunk neon streets\n2. Ocean slow motion 4k",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Module 7: Generation Mode & Ratio Quick Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPresets = !showPresets }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Presets: ${state.config.generationMode.displayName} • ${state.config.aspectRatio.displayName}",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (showPresets) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = showPresets) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("Generation Mode:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GenerationMode.values().forEach { mode ->
                            FilterChip(
                                selected = state.config.generationMode == mode,
                                onClick = {
                                    AutomationEngine.updateConfig(state.config.copy(generationMode = mode))
                                },
                                label = { Text(mode.shortName, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkNavyBg,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Aspect Ratio Lock:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AspectRatioPreset.values().forEach { ratio ->
                            FilterChip(
                                selected = state.config.aspectRatio == ratio,
                                onClick = {
                                    AutomationEngine.updateConfig(state.config.copy(aspectRatio = ratio))
                                },
                                label = { Text(ratio.label, fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonPurple,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkNavyBg,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Delay Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(15, 30, 45, 60, 90).forEach { sec ->
                    val selected = state.config.delaySeconds == sec
                    FilterChip(
                        selected = selected,
                        onClick = {
                            AutomationEngine.updateConfig(state.config.copy(delaySeconds = sec))
                        },
                        label = { Text("${sec}s", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f).testTag("delay_chip_${sec}s"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.Black,
                            containerColor = SurfaceDark,
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.status == AutomationStatus.RUNNING) {
                    Button(
                        onClick = { AutomationEngine.pauseQueue() },
                        modifier = Modifier.weight(1f).testTag("pause_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else if (state.status == AutomationStatus.PAUSED) {
                    Button(
                        onClick = { AutomationEngine.resumeQueue() },
                        modifier = Modifier.weight(1f).testTag("resume_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (rawPromptsText.isNotBlank() && state.prompts.isEmpty()) {
                                AutomationEngine.loadPrompts(rawPromptsText)
                            }
                            AutomationEngine.startQueue()
                        },
                        modifier = Modifier.weight(1f).testTag("start_queue_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Queue", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { AutomationEngine.stopQueue() },
                    modifier = Modifier.weight(0.7f).testTag("stop_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    shape = RoundedCornerShape(8.dp),
                    enabled = state.status == AutomationStatus.RUNNING || state.status == AutomationStatus.PAUSED
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { AutomationEngine.resetQueue() },
                    modifier = Modifier.size(38.dp).testTag("reset_queue_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Queue", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calibration Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCalibration = !showCalibration }
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Touch Calibration", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    imageVector = if (showCalibration) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = showCalibration) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    CalibrationTargetType.values().forEach { target ->
                        val pt = state.calibrationData.getPoint(target)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(target.displayName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (pt.isCalibrated) "(${pt.x.toInt()}, ${pt.y.toInt()})" else "Default",
                                    color = if (pt.isCalibrated) EmeraldGreen else Color.Gray,
                                    fontSize = 9.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { AutomationEngine.startCalibration(target) },
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Calibrate", fontSize = 8.sp, color = NeonCyan)
                                }
                                OutlinedButton(
                                    onClick = { AutomationEngine.testTapTarget(target) },
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Test", fontSize = 8.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { AutomationEngine.startCalibration() },
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Start 4-Step Calibration Wizard", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Auto-Download & Settings Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSettings = !showSettings }
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Download & Polling", color = NeonPurple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    imageVector = if (showSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Download Completed Videos", color = Color.White, fontSize = 10.sp)
                        Switch(
                            checked = state.config.autoDownloadEnabled,
                            onCheckedChange = { AutomationEngine.updateConfig(state.config.copy(autoDownloadEnabled = it)) },
                            modifier = Modifier.scale(0.75f).testTag("auto_download_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Completion Sound & Haptic Buzz", color = Color.White, fontSize = 10.sp)
                        Switch(
                            checked = state.config.soundAlertEnabled && state.config.hapticAlertEnabled,
                            onCheckedChange = {
                                AutomationEngine.updateConfig(state.config.copy(soundAlertEnabled = it, hapticAlertEnabled = it))
                            },
                            modifier = Modifier.scale(0.75f).testTag("alerts_switch")
                        )
                    }
                }
            }

            // Logs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogs = !showLogs }
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Activity Logs (${state.logs.size})", color = Color.LightGray, fontSize = 11.sp)
                Icon(
                    imageVector = if (showLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = showLogs) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 85.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(state.logs.takeLast(15).reversed()) { logEntry ->
                            Text(
                                text = logEntry,
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
}

@Composable
fun InteractiveCalibrationOverlay(
    modifier: Modifier = Modifier
) {
    val state by AutomationEngine.state.collectAsState()
    val activeTarget = state.activeCalibrationTarget ?: CalibrationTargetType.PROMPT_INPUT
    val targetIndex = CalibrationTargetType.values().indexOf(activeTarget) + 1
    val totalTargets = CalibrationTargetType.values().size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(activeTarget) {
                detectTapGestures { offset ->
                    AutomationEngine.recordCalibrationPoint(activeTarget, offset.x, offset.y)
                }
            }
            .testTag("interactive_calibration_overlay")
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkNavyBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdsClick, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Step $targetIndex of $totalTargets: Calibrate",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = { AutomationEngine.cancelCalibration() },
                        modifier = Modifier.size(28.dp).testTag("cancel_calibration_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = RoseRed, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "👉 TAP ON THE SCREEN WHERE:\n\"${activeTarget.displayName}\" IS LOCATED",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeTarget.description,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Touch anywhere on this browser screen to record coordinates.",
                    color = AmberOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
