package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreenView(
    messages: List<ChatMessage>,
    inputText: String,
    isSending: Boolean,
    quickReplies: List<String>,
    attachedImageBase64: String? = null,
    onRemoveAttachedImage: () -> Unit = {},
    onOpenToolsSheet: () -> Unit = {},
    onConfigureQuickReplies: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onSwitchToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(), // Root IME padding only - NO duplicate padding in bottom bar!
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            // Chat Header
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // sits flush directly above soft keyboard!
            ) {
                // Quick prompt chips row
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

                // Image preview strip above input
                attachedImageBase64?.let { base64 ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, SamPurplePrimary, RoundedCornerShape(8.dp))
                        ) {
                            val bitmap = remember(base64) {
                                try {
                                    val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Attached image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { onRemoveAttachedImage() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attachment",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // Input Bar Container
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
                                .size(40.dp)
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

                        // Attachment / Plus icon button
                        IconButton(
                            onClick = onOpenToolsSheet,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SamSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Open Advanced Tools Drawer",
                                tint = SamPurpleSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text Input Field
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
                            enabled = inputText.isNotBlank() || attachedImageBase64 != null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() || attachedImageBase64 != null) {
                                        SamPurplePrimary
                                    } else {
                                        SamSurfaceElevated
                                    }
                                )
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() || attachedImageBase64 != null) SamDarkBg else SamTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { chatInnerPadding ->
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
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Message Image Attachment (if present)
                                if (!msg.imageBase64.isNullOrEmpty()) {
                                    val msgBitmap = remember(msg.imageBase64) {
                                        try {
                                            val bytes = android.util.Base64.decode(msg.imageBase64, android.util.Base64.DEFAULT)
                                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (msgBitmap != null) {
                                        Column {
                                            androidx.compose.foundation.Image(
                                                bitmap = msgBitmap.asImageBitmap(),
                                                contentDescription = "Message Image Attachment",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 180.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .padding(bottom = 6.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                            
                                            // Image Action Overlay: Download and Share buttons
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        Toast.makeText(context, "Image downloaded to gallery! 📥", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                        contentColor = if (isUser) SamDarkBg else MaterialTheme.colorScheme.primary
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp).testTag("download_image_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Download", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = {
                                                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                                                            action = android.content.Intent.ACTION_SEND
                                                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out this beautiful AI image generated by Sam AI!")
                                                            type = "text/plain"
                                                        }
                                                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                                        context.startActivity(shareIntent)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = SamSurfaceBorder.copy(alpha = 0.3f),
                                                        contentColor = if (isUser) SamDarkBg else SamTextPrimary
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp).testTag("share_image_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Share", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Message Video Playback Card (if present)
                                if (!msg.videoUrl.isNullOrEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .padding(bottom = 8.dp)
                                            .testTag("video_playback_card"),
                                        colors = CardDefaults.cardColors(containerColor = SamDarkBg),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Ambient background gradient
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                                Color.Black.copy(alpha = 0.8f)
                                                            )
                                                        )
                                                    )
                                            )
                                            
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(msg.videoUrl))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Streaming video...", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .testTag("play_video_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play Video",
                                                        tint = SamDarkBg,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Tap to Stream Video",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = SamTextPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            
                                            // Video Duration Badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.7f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = msg.videoDuration ?: "15s",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // Selection container for long press copy selection
                                SelectionContainer {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isUser) SamDarkBg else SamTextPrimary,
                                            fontWeight = if (isUser) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    )
                                }

                                if (msg.isAction && msg.actionDetail != null) {
                                    Text(
                                        text = "⚙️ ${msg.actionDetail}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = SamCyanGlow,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Footer row with metadata & copy/speak action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = timeFormat.format(Date(msg.timestamp)),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isUser) SamDarkBg.copy(alpha = 0.6f) else SamTextMuted,
                                            fontSize = 10.sp
                                        )
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Copy Action Button
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(msg.text))
                                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy message text",
                                                tint = if (isUser) SamDarkBg.copy(alpha = 0.6f) else SamTextMuted,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        // Speaker Action Button (Assistant Messages Only)
                                        if (!isUser) {
                                            IconButton(
                                                onClick = { onSpeakMessage(msg.text) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Speak message",
                                                    tint = SamCyan,
                                                    modifier = Modifier.size(13.dp)
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
    }
}
