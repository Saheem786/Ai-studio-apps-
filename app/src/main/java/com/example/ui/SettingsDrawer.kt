package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AssistantLanguage
import com.example.ui.theme.AppThemeAccent
import com.example.ui.theme.SamCyan
import com.example.ui.theme.SamCyanGlow
import com.example.ui.theme.SamDarkBg
import com.example.ui.theme.SamErrorRed
import com.example.ui.theme.SamMagenta
import com.example.ui.theme.SamPurpleSecondary
import com.example.ui.theme.SamSurface
import com.example.ui.theme.SamSurfaceBorder
import com.example.ui.theme.SamSurfaceElevated
import com.example.ui.theme.SamTextMuted
import com.example.ui.theme.SamTextPrimary
import com.example.ui.theme.SamTextSecondary

/**
 * Modern Material 3 Settings Drawer sheet.
 * Features an intuitive, dynamic Language Selection UI for switching between
 * Hindi, English, Hinglish, and international languages with instant live
 * SpeechRecognizer intent reconfiguration for optimal acoustic recognition.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDrawerSheet(
    selectedLanguage: AssistantLanguage,
    onLanguageSelected: (AssistantLanguage) -> Unit,
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    currentSelectedModel: String,
    onModelSelected: (String) -> Unit,
    voiceMatchEnforced: Boolean,
    onVoiceMatchEnforcedChanged: (Boolean) -> Unit,
    isVoiceCalibrationCompleted: Boolean,
    onOpenVoiceEnrollment: () -> Unit,
    floatingWidgetEnabled: Boolean,
    onFloatingWidgetEnabledChanged: (Boolean) -> Unit,
    wakeWordEnabled: Boolean,
    onWakeWordEnabledChanged: (Boolean) -> Unit,
    personalIntelligenceEnabled: Boolean,
    onPersonalIntelligenceToggled: (Boolean) -> Unit,
    currentThemeAccent: AppThemeAccent,
    onOpenFullThemeSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onClearChat: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyText by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var showApiKeyInput by remember { mutableStateOf(false) }

    val standardModels = listOf(
        "Autonomous Smart Routing",
        "gemini-3.8-flash",
        "gemini-3.1-pro-preview",
        "gemini-3.7-flash"
    )
    val initialCleanModel = currentSelectedModel.trim().removePrefix("models/").ifBlank { "Autonomous Smart Routing" }
    var selectedModelText by remember(currentSelectedModel) {
        mutableStateOf(
            if (initialCleanModel == "gemini-1.5-flash" || initialCleanModel == "gemini-2.0-flash") "Autonomous Smart Routing" else initialCleanModel
        )
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(340.dp)
            .fillMaxHeight(),
        drawerContainerColor = SamDarkBg,
        drawerContentColor = SamTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                Brush.linearGradient(listOf(SamCyan, SamMagenta)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Voice & Engine Configuration",
                            style = MaterialTheme.typography.labelSmall,
                            color = SamTextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SamSurface, CircleShape)
                        .testTag("close_settings_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings Drawer",
                        tint = SamTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SECTION: SPEECH RECOGNITION LANGUAGE (PRIMARY FOCUS)
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SamSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, SamCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = SamCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Voice Recognition Language",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        // Live status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SamCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DYNAMIC STT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SamCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch language dynamically to optimize Android SpeechRecognizer accuracy and vocal response.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = SamTextMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Language Options (Hinglish, Hindi, English-IN, English-US)
                    val primaryLanguages = listOf(
                        AssistantLanguage.HINGLISH,
                        AssistantLanguage.HINDI,
                        AssistantLanguage.ENGLISH_IN,
                        AssistantLanguage.ENGLISH_US
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        primaryLanguages.forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            LanguageCard(
                                language = lang,
                                isSelected = isSelected,
                                onClick = {
                                    onLanguageSelected(lang)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Other Supported Languages
                    Text(
                        text = "More Languages",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SamTextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val otherLanguages = listOf(
                        AssistantLanguage.SPANISH,
                        AssistantLanguage.FRENCH,
                        AssistantLanguage.GERMAN,
                        AssistantLanguage.JAPANESE
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        otherLanguages.forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { onLanguageSelected(lang) },
                                label = {
                                    Text(
                                        text = "${lang.flag} ${lang.displayName}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SamSurface,
                                    labelColor = SamTextSecondary,
                                    selectedContainerColor = SamMagenta.copy(alpha = 0.25f),
                                    selectedLabelColor = SamMagenta
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SamSurfaceBorder,
                                    selectedBorderColor = SamMagenta,
                                    borderWidth = 1.dp
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Configuration Telemetry Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SamDarkBg.copy(alpha = 0.6f))
                            .border(1.dp, SamSurfaceBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = SamCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Active SpeechRecognizer Profile",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SamCyan
                                    )
                                }
                                Text(
                                    text = selectedLanguage.accuracyBadge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SamMagenta
                                )
                            }

                            Text(
                                text = "• Speech Locale: ${selectedLanguage.speechLocale}  |  TTS: ${selectedLanguage.ttsLanguage}_${selectedLanguage.ttsCountry}",
                                fontSize = 10.sp,
                                color = SamTextSecondary
                            )
                            if (selectedLanguage.additionalLocales.isNotEmpty()) {
                                Text(
                                    text = "• Multilingual Switch: [${selectedLanguage.additionalLocales.joinToString(", ")}]",
                                    fontSize = 10.sp,
                                    color = SamTextMuted
                                )
                            }
                            if (selectedLanguage.samplePrompt.isNotBlank()) {
                                Text(
                                    text = "• Quick Prompt: \"${selectedLanguage.samplePrompt}\"",
                                    fontSize = 10.sp,
                                    color = SamCyanGlow
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SECTION: GEMINI BRAIN & API CREDENTIALS
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SamSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = SamPurpleSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Gemini AI Engine",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        TextButton(
                            onClick = { showApiKeyInput = !showApiKeyInput }
                        ) {
                            Text(
                                text = if (showApiKeyInput) "Done" else "API Key",
                                fontSize = 11.sp,
                                color = SamCyan
                            )
                        }
                    }

                    if (showApiKeyInput) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = {
                                apiKeyText = it
                                onSaveApiKey(it)
                            },
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
                                .testTag("drawer_api_key_input")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    } else {
                        val keyPreview = if (currentApiKey.isNotBlank()) "Key Configured (•••${currentApiKey.takeLast(4)})" else "Default / Secrets Panel"
                        Text(
                            text = "Status: $keyPreview",
                            fontSize = 11.sp,
                            color = if (currentApiKey.isNotBlank()) SamCyan else SamTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Multi-Brain Model Selection",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SamTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val modelOptions = listOf(
                        "Autonomous Smart Routing" to "⚡ Smart Auto",
                        "gemini-3.8-flash" to "3.8 Flash",
                        "gemini-3.1-pro-preview" to "3.1 Pro",
                        "gemini-3.7-flash" to "3.7 Flash"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        modelOptions.forEach { (modelId, label) ->
                            val isSelected = selectedModelText == modelId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedModelText = modelId
                                    onModelSelected(modelId)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SamSurface,
                                    labelColor = SamTextSecondary,
                                    selectedContainerColor = SamPurpleSecondary.copy(alpha = 0.25f),
                                    selectedLabelColor = SamPurpleSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = SamSurfaceBorder,
                                    selectedBorderColor = SamPurpleSecondary,
                                    borderWidth = 1.dp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SECTION: ASSISTANT CONTROLS & WIDGETS
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SamSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Voice & Security",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice Match Calibration Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Calibration",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SamTextPrimary
                            )
                            Text(
                                text = if (isVoiceCalibrationCompleted) "Voice pattern trained ✓" else "Not calibrated",
                                fontSize = 10.sp,
                                color = if (isVoiceCalibrationCompleted) SamCyan else SamTextMuted
                            )
                        }
                        TextButton(onClick = onOpenVoiceEnrollment) {
                            Text("Calibrate", fontSize = 11.sp, color = SamCyan)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SamSurfaceBorder)

                    // Floating Orb Widget Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Orb Widget",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SamTextPrimary
                            )
                            Text(
                                text = "Overlay mic orb on any screen",
                                fontSize = 10.sp,
                                color = SamTextMuted
                            )
                        }
                        Switch(
                            checked = floatingWidgetEnabled,
                            onCheckedChange = onFloatingWidgetEnabledChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SamCyan,
                                uncheckedTrackColor = SamSurface
                            ),
                            modifier = Modifier.testTag("drawer_switch_floating_widget")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SamSurfaceBorder)

                    // Wake Word Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wake Word ('Hey Sam')",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SamTextPrimary
                            )
                            Text(
                                text = "Hands-free voice trigger activation",
                                fontSize = 10.sp,
                                color = SamTextMuted
                            )
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = onWakeWordEnabledChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SamMagenta,
                                uncheckedTrackColor = SamSurface
                            ),
                            modifier = Modifier.testTag("drawer_switch_wake_word")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SamSurfaceBorder)

                    // Personal Intelligence Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Personal Context Memory",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SamTextPrimary
                            )
                            Text(
                                text = "Remember user preferences & context",
                                fontSize = 10.sp,
                                color = SamTextMuted
                            )
                        }
                        Switch(
                            checked = personalIntelligenceEnabled,
                            onCheckedChange = onPersonalIntelligenceToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SamCyan,
                                uncheckedTrackColor = SamSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SECTION: QUICK SHORTCUTS & SYSTEM
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Theme Settings Shortcut
                DrawerActionRow(
                    title = "App Theme & Accents",
                    subtitle = "Current: ${currentThemeAccent.displayName}",
                    icon = Icons.Default.Palette,
                    tint = SamMagenta,
                    onClick = onOpenFullThemeSettings
                )

                // Voice Commands Guide Shortcut
                DrawerActionRow(
                    title = "Voice Commands Guide",
                    subtitle = "Learn alarms, SMS, Wi-Fi & calls commands",
                    icon = Icons.AutoMirrored.Filled.Help,
                    tint = SamCyan,
                    onClick = onOpenHelp
                )

                // Diagnostic Logs Shortcut
                DrawerActionRow(
                    title = "Speech & System Diagnostics",
                    subtitle = "Live logs & SpeechRecognizer telemetry",
                    icon = Icons.Default.BugReport,
                    tint = SamCyanGlow,
                    onClick = onOpenDiagnostics
                )

                // Clear Chat History
                DrawerActionRow(
                    title = "Clear Chat History",
                    subtitle = "Delete local conversation database",
                    icon = Icons.Default.DeleteOutline,
                    tint = SamErrorRed,
                    onClick = onClearChat
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Individual high-impact card for selecting primary voice recognition languages (Hinglish, Hindi, English).
 */
@Composable
private fun LanguageCard(
    language: AssistantLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) SamCyan else SamSurfaceBorder,
        label = "border_color"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SamCyan.copy(alpha = 0.12f) else SamSurface,
        label = "bg_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("language_card_${language.code}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = language.flag, fontSize = 20.sp)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = language.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SamCyan else Color.White
                                )
                            )
                            if (language.nativeName.isNotBlank() && language.nativeName != language.displayName) {
                                Text(
                                    text = "(${language.nativeName})",
                                    fontSize = 11.sp,
                                    color = SamTextMuted
                                )
                            }
                        }
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(SamCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(1.dp, SamTextMuted, CircleShape)
                    )
                }
            }

            if (language.description.isNotBlank()) {
                Text(
                    text = language.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        color = if (isSelected) SamTextPrimary else SamTextSecondary
                    )
                )
            }

            if (language.samplePrompt.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Example:",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SamCyan
                    )
                    Text(
                        text = "\"${language.samplePrompt}\"",
                        fontSize = 9.5.sp,
                        color = SamTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SamSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SamSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(tint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = SamTextMuted
                )
            }
        }
    }
}
