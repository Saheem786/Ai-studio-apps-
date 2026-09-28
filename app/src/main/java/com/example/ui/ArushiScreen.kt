package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
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
fun ArushiScreen(
    viewModel: ArushiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startSession()
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result handled dynamically */ }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    viewModel.setAttachedImage(base64)
                    Toast.makeText(context, "Image attached!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Image error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleMicClick() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.toggleSession()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ArushiDarkBg,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ArushiTopBar(
                    state = uiState.state,
                    onOpenDiagnostics = { viewModel.toggleDiagnosticsSheet(true) },
                    onOpenSettings = { viewModel.toggleSettingsDialog(true) }
                )
                AppModeToggle(
                    currentMode = uiState.currentMode,
                    onModeSelected = { viewModel.setAppMode(it) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.currentMode == AppMode.CHAT) {
            ChatScreenView(
                messages = uiState.chatMessages,
                inputText = uiState.inputText,
                isSending = uiState.isSendingChat,
                quickReplies = uiState.quickReplies,
                attachedImageBase64 = uiState.attachedImageBase64,
                onRemoveAttachedImage = { viewModel.setAttachedImage(null) },
                onOpenToolsSheet = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onConfigureQuickReplies = { viewModel.toggleQuickRepliesConfigDialog(true) },
                onInputChanged = { viewModel.updateInputText(it) },
                onSendMessage = { viewModel.sendChatMessage(it) },
                onSpeakMessage = { viewModel.speakMessage(it) },
                onClearChat = { viewModel.clearChatHistory() },
                onSwitchToVoice = {
                    viewModel.setAppMode(AppMode.VOICE)
                    handleMicClick()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Assistant State Badge
                AssistantStatusBadge(state = uiState.state)

                Spacer(modifier = Modifier.height(16.dp))

                // Action Banner (if active tool execution or feedback)
                AnimatedVisibility(
                    visible = uiState.actionBanner != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.actionBanner?.let { banner ->
                        ActionFeedbackBanner(message = banner)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Error Message Banner (if error occurred)
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.errorMessage?.let { err ->
                        Surface(
                            color = ArushiErrorRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ArushiErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFFDA4AF),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Central Orb Visualizer
                ArushiOrbVisualizer(
                    state = uiState.state,
                    amplitude = uiState.amplitude,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Live User & Arushi Transcript
                Surface(
                    color = ArushiSurfaceElevated.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArushiSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = uiState.transcript,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ArushiTextPrimary,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Action Suggestion Chips
                QuickActionChips(
                    onChipClick = { prompt -> viewModel.sendQuickPrompt(prompt) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Persistent Conversation Logs (Room Database) displayed alongside the voice assistant interaction
                VoiceConversationLogsCard(
                    messages = uiState.chatMessages,
                    onOpenFullChat = { viewModel.setAppMode(AppMode.CHAT) },
                    onClearHistory = { viewModel.clearChatHistory() },
                    onSpeakMessage = { viewModel.speakMessage(it) }
                )
            }

            // Bottom Section: Speaker Diagnostic & Big Power/Mic Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 16.dp)
            ) {
                // Speaker Diagnostic Bar (Section 15 Requirement)
                SpeakerDiagnosticBar(
                    onTestSpeaker = { viewModel.testSpeaker() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Big Tactile Microphone / Power Button
                val buttonBgColor by animateColorAsState(
                    targetValue = when (uiState.state) {
                        AssistantState.IDLE -> ArushiPurplePrimary
                        AssistantState.CONNECTING -> ArushiCyan
                        AssistantState.LISTENING -> ArushiCyanGlow
                        AssistantState.SPEAKING -> ArushiMagenta
                        AssistantState.ERROR -> ArushiErrorRed
                    },
                    label = "btn_bg"
                )

                val buttonScale by animateFloatAsState(
                    targetValue = if (uiState.isMicActive) 1.08f else 1.0f,
                    label = "btn_scale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .scale(buttonScale)
                ) {
                    // Outer glow ring when active
                    if (uiState.isMicActive) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(buttonBgColor.copy(alpha = 0.25f))
                        )
                    }

                    // Main interactive button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(16.dp, CircleShape, spotColor = buttonBgColor)
                            .clip(CircleShape)
                            .background(buttonBgColor)
                            .clickable { handleMicClick() }
                            .testTag("microphone_power_button")
                    ) {
                        when (uiState.state) {
                            AssistantState.CONNECTING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = ArushiDarkBg,
                                    strokeWidth = 3.dp
                                )
                            }
                            AssistantState.LISTENING -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone Active",
                                    tint = ArushiDarkBg,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            AssistantState.SPEAKING -> {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Speaking",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Start Sam",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (uiState.isMicActive) "Tap to pause or stop Sam" else "Tap to activate Sam",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ArushiTextMuted,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

    // Diagnostics Bottom Sheet
    if (uiState.showDiagnosticsSheet) {
        DiagnosticsBottomSheet(
            logs = uiState.diagnosticLogs,
            onDismiss = { viewModel.toggleDiagnosticsSheet(false) },
            onClearLogs = { viewModel.clearLogs() }
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            currentApiKey = uiState.apiKey,
            onSaveKey = { newKey -> viewModel.updateApiKey(newKey) },
            onDismiss = { viewModel.toggleSettingsDialog(false) }
        )
    }

    // Quick Replies Configuration Dialog
    if (uiState.showQuickRepliesConfigDialog) {
        QuickRepliesConfigDialog(
            quickReplies = uiState.quickReplies,
            onAddReply = { phrase -> viewModel.addQuickReply(phrase) },
            onRemoveReply = { phrase -> viewModel.removeQuickReply(phrase) },
            onResetReplies = { viewModel.resetQuickReplies() },
            onDismiss = { viewModel.toggleQuickRepliesConfigDialog(false) }
        )
    }

    // Canvas Workspace Panel View
    CanvasView(
        isVisible = uiState.showCanvas,
        canvasTitle = uiState.canvasTitle,
        canvasContent = uiState.canvasContent,
        contentType = uiState.canvasContentType,
        onClose = { viewModel.closeCanvas() },
        onCopyContent = { text ->
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Canvas Content", text)
            clipboard.setPrimaryClip(clip)
        }
    )

    // Multiple Contacts Resolution Dialog
    uiState.multipleContacts?.let { contacts ->
        MultipleContactsDialog(
            contacts = contacts,
            onSelect = { contact -> viewModel.selectContactToCall(contact) },
            onDismiss = { viewModel.dismissContactsDialog() }
        )
    }
}
