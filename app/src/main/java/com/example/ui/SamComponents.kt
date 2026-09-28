package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.device.ContactItem
import com.example.ui.theme.AppThemeAccent
import com.example.ui.theme.SamCyan
import com.example.ui.theme.SamCyanGlow
import com.example.ui.theme.SamDarkBg
import com.example.ui.theme.SamErrorRed
import com.example.ui.theme.SamMagenta
import com.example.ui.theme.SamPurplePrimary
import com.example.ui.theme.SamPurpleSecondary
import com.example.ui.theme.SamSurface
import com.example.ui.theme.SamSurfaceBorder
import com.example.ui.theme.SamSurfaceElevated
import com.example.ui.theme.SamTextMuted
import com.example.ui.theme.SamTextPrimary
import com.example.ui.theme.SamTextSecondary

@Composable
fun SamTopBar(
    state: AssistantState,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Status Indicator Dot
            val dotColor by animateColorAsState(
                targetValue = when (state) {
                    AssistantState.IDLE -> SamTextMuted
                    AssistantState.CONNECTING -> SamCyan
                    AssistantState.LISTENING -> SamCyanGlow
                    AssistantState.SPEAKING -> SamMagenta
                    AssistantState.ERROR -> SamErrorRed
                },
                label = "dot_color"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Column {
                Text(
                    text = "Sam Ai",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SamTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Real-time Voice AI",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SamPurpleSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onOpenDiagnostics,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SamSurfaceElevated)
                    .testTag("diagnostics_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Diagnostics",
                    tint = SamCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SamSurfaceElevated)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SamTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AssistantStatusBadge(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val (label, color, bgColor) = when (state) {
        AssistantState.IDLE -> Triple("Ready to Talk", SamTextSecondary, SamSurfaceElevated)
        AssistantState.CONNECTING -> Triple("Connecting Live...", SamCyan, SamSurfaceElevated)
        AssistantState.LISTENING -> Triple("Listening to You", SamCyanGlow, Color(0xFF083344))
        AssistantState.SPEAKING -> Triple("Sam Speaking", SamMagenta, Color(0xFF4A044E))
        AssistantState.ERROR -> Triple("Connection Issue", SamErrorRed, Color(0xFF450A0A))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun SpeakerDiagnosticBar(
    onTestSpeaker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SamSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speaker",
                    tint = SamCyan,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Speaker & TTS Diagnostic",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SamTextPrimary
                        )
                    )
                    Text(
                        text = "Verifies speaker audio & Android Text-to-Speech output",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SamTextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Button(
                onClick = onTestSpeaker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SamCyan.copy(alpha = 0.15f),
                    contentColor = SamCyanGlow
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SamCyan.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("test_speaker_button")
            ) {
                Text(
                    text = "Test Speaker",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun QuickActionChips(
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "👋 Hello Sam",
        "💬 WhatsApp kholo",
        "▶️ Open YouTube",
        "📞 Call Mom",
        "🤖 Hindi mein baat karo",
        "🌐 Open Google",
        "🎶 Open Spotify"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { prompt ->
            Surface(
                color = SamSurfaceElevated,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onChipClick(prompt.replace(Regex("^[^\"]* "), "")) }
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SamTextSecondary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ActionFeedbackBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E1B4B),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SamPurplePrimary.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Action",
                tint = SamPurpleSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SamTextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsBottomSheet(
    logs: List<String>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SamDarkBg,
        scrimColor = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live Audio & API Diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextPrimary
                        )
                    )
                    Text(
                        text = "${logs.size} trace events logged",
                        style = MaterialTheme.typography.bodySmall.copy(color = SamTextMuted)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClearLogs) {
                        Text("Clear", color = SamCyan)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = SamTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = SamSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No diagnostic events yet. Start talking to Sam to record live events.",
                            color = SamTextMuted,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = if (log.contains("error", ignoreCase = true) || log.contains("fail", ignoreCase = true)) {
                                        SamErrorRed
                                    } else if (log.contains("audio", ignoreCase = true) || log.contains("pcm", ignoreCase = true)) {
                                        SamCyanGlow
                                    } else if (log.contains("tool", ignoreCase = true) || log.contains("action", ignoreCase = true)) {
                                        SamPurpleSecondary
                                    } else {
                                        SamTextSecondary
                                    }
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultipleContactsDialog(
    contacts: List<ContactItem>,
    onSelect: (ContactItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SamSurfaceElevated,
        title = {
            Text(
                text = "Multiple Contacts Found",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SamTextPrimary
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Sam found several contacts matching your request. Which one would you like to call?",
                    style = MaterialTheme.typography.bodySmall.copy(color = SamTextSecondary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(contacts) { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(contact) },
                            colors = CardDefaults.cardColors(containerColor = SamSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SamTextPrimary
                                        )
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall.copy(color = SamTextMuted)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = SamCyan
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SamTextSecondary)
            }
        }
    )
}

@Composable
fun SettingsDialog(
    currentApiKey: String,
    voiceMatchEnforced: Boolean,
    onVoiceMatchEnforcedChanged: (Boolean) -> Unit,
    isVoiceCalibrationCompleted: Boolean,
    onCalibrateVoiceClicked: () -> Unit,
    onClearVoiceClicked: () -> Unit,
    floatingWidgetEnabled: Boolean,
    onFloatingWidgetEnabledChanged: (Boolean) -> Unit,
    currentThemeAccent: AppThemeAccent,
    onThemeAccentChanged: (AppThemeAccent) -> Unit,
    onOpenFullThemeSettings: () -> Unit,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SamSurfaceElevated,
        title = {
            Text(
                text = "Sam Ai Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SamTextPrimary
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Configure Gemini API credentials and real-time voice settings.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SamTextSecondary)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SamCyan
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    placeholder = { Text("AIzaSy...", color = SamTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SamTextPrimary,
                        unfocusedTextColor = SamTextPrimary,
                        focusedBorderColor = SamCyan,
                        unfocusedBorderColor = SamSurfaceBorder,
                        focusedContainerColor = SamSurface,
                        unfocusedContainerColor = SamSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Key is securely stored in local app preferences or injected via Secrets panel.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = SamTextMuted
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Theme accent selection
                Text(
                    text = "System Accent Scheme",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SamCyan
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppThemeAccent.values().forEach { accent ->
                        val isSelected = currentThemeAccent == accent
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onThemeAccentChanged(accent) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(accent.primary)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) SamTextPrimary else SamSurfaceBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = accent.displayName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SamTextPrimary else SamTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenFullThemeSettings,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SamCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SamCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("open_full_theme_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Customize Theme Palette (Full)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Floating Widget Overlay Switch
                Text(
                    text = "Jarvis Floating Orb",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SamCyan
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Draggable Floating Widget",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SamTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Access Sam overlay listening anywhere instantly",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = SamTextMuted
                            )
                        )
                    }

                    Switch(
                        checked = floatingWidgetEnabled,
                        onCheckedChange = { onFloatingWidgetEnabledChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SamDarkBg,
                            checkedTrackColor = SamCyan,
                            uncheckedThumbColor = SamTextMuted,
                            uncheckedTrackColor = SamSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Voice matching section
                Text(
                    text = "Biometric Voice Security",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SamCyan
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enforce Voice Match",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SamTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (isVoiceCalibrationCompleted) {
                                "Execute actions only for enrolled owner voice"
                            } else {
                                "Calibrate voiceprint to enable matching restriction"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = SamTextMuted
                            )
                        )
                    }

                    Switch(
                        checked = voiceMatchEnforced && isVoiceCalibrationCompleted,
                        onCheckedChange = { onVoiceMatchEnforcedChanged(it) },
                        enabled = isVoiceCalibrationCompleted,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SamDarkBg,
                            checkedTrackColor = SamCyan,
                            uncheckedThumbColor = SamTextMuted,
                            uncheckedTrackColor = SamSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCalibrateVoiceClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVoiceCalibrationCompleted) SamSurface else SamPurplePrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isVoiceCalibrationCompleted) "Recalibrate" else "Enroll Voice",
                            color = if (isVoiceCalibrationCompleted) SamTextPrimary else SamDarkBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isVoiceCalibrationCompleted) {
                        OutlinedButton(
                            onClick = onClearVoiceClicked,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SamErrorRed),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SamErrorRed.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Clear print",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Default Model: gemini-3.8-flash\nVoice Profile: Aoede (Expressive & Natural)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = SamPurpleSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveKey(apiKeyText) },
                colors = ButtonDefaults.buttonColors(containerColor = SamPurplePrimary)
            ) {
                Text("Save", color = SamDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SamTextSecondary)
            }
        }
    )
}

@Composable
fun AppModeToggle(
    currentMode: AppMode,
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SamSurfaceElevated,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isVoice = currentMode == AppMode.VOICE
            Surface(
                color = if (isVoice) SamPurplePrimary else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onModeSelected(AppMode.VOICE) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voice Mode",
                        tint = if (isVoice) SamDarkBg else SamTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Voice Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isVoice) FontWeight.Bold else FontWeight.Medium,
                            color = if (isVoice) SamDarkBg else SamTextSecondary
                        )
                    )
                }
            }

            val isChat = currentMode == AppMode.CHAT
            Surface(
                color = if (isChat) SamCyan else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onModeSelected(AppMode.CHAT) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chat Mode",
                        tint = if (isChat) SamDarkBg else SamTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Chat Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isChat) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChat) SamDarkBg else SamTextSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LegacyChatScreenView(
    messages: List<ChatMessage>,
    inputText: String,
    isSending: Boolean,
    quickReplies: List<String>,
    onConfigureQuickReplies: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onSwitchToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(), // Apply IME padding ONLY ONCE here at the Scaffold/Root level
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            // Chat Header with Clear button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversation with Sam",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SamTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                )
                TextButton(
                    onClick = onClearChat,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear",
                        tint = SamTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.bodySmall.copy(color = SamTextMuted)
                    )
                }
            }
        },
        bottomBar = {
            // Bottom Input Bar container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Handles Android navigation bar only, DO NOT put another .imePadding() here!
            ) {
                // Quick prompt chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickReplies.forEach { suggestion ->
                        Surface(
                            color = SamSurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSendMessage(suggestion) }
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SamTextSecondary,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Configure Button
                    Surface(
                        color = SamPurplePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SamPurplePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onConfigureQuickReplies() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configure Quick Replies",
                                tint = SamPurpleSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = SamPurpleSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Bottom Message Input Bar
                Surface(
                    color = SamSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Switch to voice icon button
                        IconButton(
                            onClick = onSwitchToVoice,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SamSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice mode",
                                tint = SamCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text Field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = onInputChanged,
                            placeholder = {
                                Text(
                                    text = "Message Sam...",
                                    color = SamTextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSendMessage(inputText) }),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SamTextPrimary,
                                unfocusedTextColor = SamTextPrimary,
                                focusedBorderColor = SamPurplePrimary,
                                unfocusedBorderColor = SamSurfaceBorder,
                                focusedContainerColor = SamSurfaceElevated,
                                unfocusedContainerColor = SamSurfaceElevated
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field")
                        )

                        // Send Button
                        IconButton(
                            onClick = { onSendMessage(inputText) },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) SamPurplePrimary else SamSurfaceElevated)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) SamDarkBg else SamTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { chatInnerPadding ->
        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(chatInnerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == MessageSender.USER
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        if (!isUser) {
                            Surface(
                                color = SamCyan.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(bottom = 2.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Sam",
                                        tint = SamCyanGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Surface(
                            color = if (isUser) SamPurplePrimary else SamSurfaceElevated,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder) else null
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isUser) Color.White else SamTextPrimary,
                                        lineHeight = 20.sp
                                    )
                                )

                                if (msg.isAction && msg.actionDetail != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = SamDarkBg.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "⚙️ ${msg.actionDetail}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SamCyanGlow,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = timeFormat.format(Date(msg.timestamp)),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = if (isUser) Color.White.copy(alpha = 0.7f) else SamTextMuted
                                        )
                                    )

                                    if (!isUser) {
                                        IconButton(
                                            onClick = { onSpeakMessage(msg.text) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Listen",
                                                tint = SamCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isSending) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 40.dp, top = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = SamCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Sam is thinking...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SamTextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceConversationLogsCard(
    messages: List<ChatMessage>,
    onOpenFullChat: () -> Unit,
    onClearHistory: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SamSurfaceElevated.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title, Room DB persistent indicator, and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SamPurplePrimary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Conversation History",
                            tint = SamPurpleSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Conversation Logs",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = SamTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Room Database Persistent (${messages.size})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SamCyanGlow,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onOpenFullChat,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Full Chat",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SamCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear History",
                                tint = SamTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recent messages list (last 4 messages displayed directly)
            val displayList = messages.takeLast(4)
            if (displayList.isEmpty()) {
                Surface(
                    color = SamDarkBg.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No conversation history yet. Start talking to Sam or ask a question to see real-time persistent logs.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SamTextMuted,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    displayList.forEach { msg ->
                        val isUser = msg.sender == MessageSender.USER
                        Surface(
                            color = if (isUser) SamPurplePrimary.copy(alpha = 0.2f) else SamDarkBg.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isUser) SamPurplePrimary.copy(alpha = 0.4f) else SamSurfaceBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        color = if (isUser) SamPurplePrimary else SamCyan.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                                                contentDescription = null,
                                                tint = if (isUser) Color.White else SamCyanGlow,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (isUser) "You" else "Sam",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isUser) SamPurpleSecondary else SamCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Text(
                                                text = timeFormat.format(Date(msg.timestamp)),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = SamTextMuted,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }

                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = SamTextPrimary,
                                                lineHeight = 16.sp,
                                                fontSize = 12.sp
                                            ),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        if (msg.isAction && msg.actionDetail != null) {
                                            Text(
                                                text = "⚙️ ${msg.actionDetail}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = SamCyanGlow,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (!isUser) {
                                    IconButton(
                                        onClick = { onSpeakMessage(msg.text) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Speak",
                                            tint = SamCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickRepliesConfigDialog(
    quickReplies: List<String>,
    onAddReply: (String) -> Unit,
    onRemoveReply: (String) -> Unit,
    onResetReplies: () -> Unit,
    onDismiss: () -> Unit
) {
    var newReplyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SamSurfaceElevated,
        title = {
            Text(
                text = "Quick Replies Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SamTextPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tap a phrase's delete icon to remove it, or enter a new one below to add it to your quick reply chips.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SamTextSecondary)
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable list of current replies
                Surface(
                    color = SamSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    if (quickReplies.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "No quick replies configured.",
                                style = MaterialTheme.typography.bodySmall.copy(color = SamTextMuted)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(quickReplies) { phrase ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SamSurfaceElevated, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = phrase,
                                        style = MaterialTheme.typography.bodySmall.copy(color = SamTextPrimary),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { onRemoveReply(phrase) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = SamErrorRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Add New Phrase",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SamCyan
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newReplyText,
                        onValueChange = { newReplyText = it },
                        placeholder = { Text("e.g. Remind me in 5 mins", color = SamTextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SamTextPrimary,
                            unfocusedTextColor = SamTextPrimary,
                            focusedBorderColor = SamCyan,
                            unfocusedBorderColor = SamSurfaceBorder,
                            focusedContainerColor = SamSurface,
                            unfocusedContainerColor = SamSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (newReplyText.isNotBlank()) {
                                onAddReply(newReplyText)
                                newReplyText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SamCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Add", color = SamDarkBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SamPurplePrimary)
            ) {
                Text("Done", color = SamDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onResetReplies) {
                Text("Reset Defaults", color = SamPurpleSecondary)
            }
        }
    )
}
