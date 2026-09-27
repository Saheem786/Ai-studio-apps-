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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.ui.theme.ArushiCyan
import com.example.ui.theme.ArushiCyanGlow
import com.example.ui.theme.ArushiDarkBg
import com.example.ui.theme.ArushiErrorRed
import com.example.ui.theme.ArushiMagenta
import com.example.ui.theme.ArushiPurplePrimary
import com.example.ui.theme.ArushiPurpleSecondary
import com.example.ui.theme.ArushiSurface
import com.example.ui.theme.ArushiSurfaceBorder
import com.example.ui.theme.ArushiSurfaceElevated
import com.example.ui.theme.ArushiTextMuted
import com.example.ui.theme.ArushiTextPrimary
import com.example.ui.theme.ArushiTextSecondary

@Composable
fun ArushiTopBar(
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
                    AssistantState.IDLE -> ArushiTextMuted
                    AssistantState.CONNECTING -> ArushiCyan
                    AssistantState.LISTENING -> ArushiCyanGlow
                    AssistantState.SPEAKING -> ArushiMagenta
                    AssistantState.ERROR -> ArushiErrorRed
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
                        color = ArushiTextPrimary,
                        letterSpacing = 0.5.sp
                    )
                )
                Text(
                    text = "Real-time Voice AI",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ArushiPurpleSecondary,
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
                    .background(ArushiSurfaceElevated)
                    .testTag("diagnostics_button")
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Diagnostics",
                    tint = ArushiCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(ArushiSurfaceElevated)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = ArushiTextSecondary,
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
        AssistantState.IDLE -> Triple("Ready to Talk", ArushiTextSecondary, ArushiSurfaceElevated)
        AssistantState.CONNECTING -> Triple("Connecting Live...", ArushiCyan, ArushiSurfaceElevated)
        AssistantState.LISTENING -> Triple("Listening to You", ArushiCyanGlow, Color(0xFF083344))
        AssistantState.SPEAKING -> Triple("Sam Speaking", ArushiMagenta, Color(0xFF4A044E))
        AssistantState.ERROR -> Triple("Connection Issue", ArushiErrorRed, Color(0xFF450A0A))
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
        colors = CardDefaults.cardColors(containerColor = ArushiSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder)
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
                    tint = ArushiCyan,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Speaker Diagnostic",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ArushiTextPrimary
                        )
                    )
                    Text(
                        text = "Verifies 440Hz output through device speaker",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ArushiTextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Button(
                onClick = onTestSpeaker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArushiCyan.copy(alpha = 0.15f),
                    contentColor = ArushiCyanGlow
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArushiCyan.copy(alpha = 0.5f)),
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
                color = ArushiSurfaceElevated,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onChipClick(prompt.replace(Regex("^[^\"]* "), "")) }
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ArushiTextSecondary,
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
        border = androidx.compose.foundation.BorderStroke(1.dp, ArushiPurplePrimary.copy(alpha = 0.5f)),
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
                tint = ArushiPurpleSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ArushiTextPrimary,
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
        containerColor = ArushiDarkBg,
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
                            color = ArushiTextPrimary
                        )
                    )
                    Text(
                        text = "${logs.size} trace events logged",
                        style = MaterialTheme.typography.bodySmall.copy(color = ArushiTextMuted)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClearLogs) {
                        Text("Clear", color = ArushiCyan)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = ArushiTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = ArushiSurface,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No diagnostic events yet. Start talking to Sam to record live events.",
                            color = ArushiTextMuted,
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
                                        ArushiErrorRed
                                    } else if (log.contains("audio", ignoreCase = true) || log.contains("pcm", ignoreCase = true)) {
                                        ArushiCyanGlow
                                    } else if (log.contains("tool", ignoreCase = true) || log.contains("action", ignoreCase = true)) {
                                        ArushiPurpleSecondary
                                    } else {
                                        ArushiTextSecondary
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
        containerColor = ArushiSurfaceElevated,
        title = {
            Text(
                text = "Multiple Contacts Found",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ArushiTextPrimary
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Sam found several contacts matching your request. Which one would you like to call?",
                    style = MaterialTheme.typography.bodySmall.copy(color = ArushiTextSecondary)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(contacts) { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(contact) },
                            colors = CardDefaults.cardColors(containerColor = ArushiSurface)
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
                                            color = ArushiTextPrimary
                                        )
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall.copy(color = ArushiTextMuted)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = ArushiCyan
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
                Text("Cancel", color = ArushiTextSecondary)
            }
        }
    )
}

@Composable
fun SettingsDialog(
    currentApiKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArushiSurfaceElevated,
        title = {
            Text(
                text = "Sam Ai Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ArushiTextPrimary
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Configure Gemini API credentials and real-time voice settings.",
                    style = MaterialTheme.typography.bodySmall.copy(color = ArushiTextSecondary)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Gemini API Key",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ArushiCyan
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    placeholder = { Text("AIzaSy...", color = ArushiTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ArushiTextPrimary,
                        unfocusedTextColor = ArushiTextPrimary,
                        focusedBorderColor = ArushiCyan,
                        unfocusedBorderColor = ArushiSurfaceBorder,
                        focusedContainerColor = ArushiSurface,
                        unfocusedContainerColor = ArushiSurface
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
                        color = ArushiTextMuted
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Default Model: gemini-2.0-flash-exp\nVoice Profile: Aoede (Expressive & Natural)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = ArushiPurpleSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveKey(apiKeyText) },
                colors = ButtonDefaults.buttonColors(containerColor = ArushiPurplePrimary)
            ) {
                Text("Save", color = ArushiDarkBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ArushiTextSecondary)
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
        color = ArushiSurfaceElevated,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder),
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isVoice = currentMode == AppMode.VOICE
            Surface(
                color = if (isVoice) ArushiPurplePrimary else Color.Transparent,
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
                        tint = if (isVoice) ArushiDarkBg else ArushiTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Voice Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isVoice) FontWeight.Bold else FontWeight.Medium,
                            color = if (isVoice) ArushiDarkBg else ArushiTextSecondary
                        )
                    )
                }
            }

            val isChat = currentMode == AppMode.CHAT
            Surface(
                color = if (isChat) ArushiCyan else Color.Transparent,
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
                        tint = if (isChat) ArushiDarkBg else ArushiTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Chat Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isChat) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChat) ArushiDarkBg else ArushiTextSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ChatScreenView(
    messages: List<ChatMessage>,
    inputText: String,
    isSending: Boolean,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
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
                    color = ArushiTextMuted,
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
                    tint = ArushiTextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.bodySmall.copy(color = ArushiTextMuted)
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                                color = ArushiCyan.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(bottom = 2.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = "Sam",
                                        tint = ArushiCyanGlow,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Surface(
                            color = if (isUser) ArushiPurplePrimary else ArushiSurfaceElevated,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder) else null
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isUser) Color.White else ArushiTextPrimary,
                                        lineHeight = 20.sp
                                    )
                                )

                                if (msg.isAction && msg.actionDetail != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = ArushiDarkBg.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "⚙️ ${msg.actionDetail}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = ArushiCyanGlow,
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
                                            color = if (isUser) Color.White.copy(alpha = 0.7f) else ArushiTextMuted
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
                                                tint = ArushiCyan,
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
                            color = ArushiCyan,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Sam is thinking...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ArushiTextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Quick prompt chips
        val chatSuggestions = listOf(
            "💬 WhatsApp kholo",
            "📞 Mom ko call karo",
            "Ek joke sunao",
            "Mausam kaisa hai?",
            "▶️ Open YouTube"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chatSuggestions.forEach { suggestion ->
                Surface(
                    color = ArushiSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSendMessage(suggestion) }
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ArushiTextSecondary,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bottom Message Input Bar
        Surface(
            color = ArushiSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder),
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
                        .background(ArushiSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice mode",
                        tint = ArushiCyan,
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
                            color = ArushiTextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage(inputText) }),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ArushiTextPrimary,
                        unfocusedTextColor = ArushiTextPrimary,
                        focusedBorderColor = ArushiPurplePrimary,
                        unfocusedBorderColor = ArushiSurfaceBorder,
                        focusedContainerColor = ArushiSurfaceElevated,
                        unfocusedContainerColor = ArushiSurfaceElevated
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
                        .background(if (inputText.isNotBlank()) ArushiPurplePrimary else ArushiSurfaceElevated)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) ArushiDarkBg else ArushiTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
