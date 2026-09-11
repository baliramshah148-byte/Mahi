package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.VoiceState
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.ChatBubble
import com.example.ui.components.MahiAvatarVisualizer
import com.example.ui.components.PersonalitySettingsSheet
import com.example.ui.components.QuickPromptBar
import com.example.ui.theme.BrightRose
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonRose
import com.example.ui.theme.SassyPink

@Composable
fun MahiScreen(
    viewModel: MahiViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var textInput by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showApiKeyInfoDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed for voice chat with Mahi!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val onMicAction = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleVoiceListening()
        } else {
            showPermissionRationale = true
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = NeonRose
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Let Mahi Hear You!",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Mahi wants to have real-time voice conversations with you! Allow microphone access so you can speak directly to her without typing. 💅",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                    modifier = Modifier.testTag("grant_mic_permission_button")
                ) {
                    Text("Allow Microphone")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Not Now")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showApiKeyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonRose
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini API Key Setup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "To enable live Gemini AI generation:",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Open the Secrets panel in AI Studio.\n" +
                                "2. Add your GEMINI_API_KEY.\n" +
                                "3. Mahi will immediately tap into Gemini 3.5 Flash!\n\n" +
                                "In the meantime, Mahi's smart local persona replies are fully functional and voiced! ✨",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showApiKeyInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
                ) {
                    Text("Got It, Darling!")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSettingsSheet) {
        PersonalitySettingsSheet(
            personalityVibe = uiState.personalityVibe,
            onPersonalityVibeChanged = { viewModel.setPersonalityVibe(it) },
            isHandsFreeMode = uiState.isHandsFreeMode,
            onHandsFreeModeChanged = { viewModel.setHandsFreeMode(it) },
            isVoiceAutoSpeak = uiState.isVoiceAutoSpeak,
            onVoiceAutoSpeakChanged = { viewModel.setVoiceAutoSpeak(it) },
            voicePitch = uiState.voicePitch,
            onVoicePitchChanged = { viewModel.setVoicePitch(it) },
            voiceSpeed = uiState.voiceSpeed,
            onVoiceSpeedChanged = { viewModel.setVoiceSpeed(it) },
            onClearChat = { viewModel.clearChat() },
            onDismiss = { showSettingsSheet = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MahiTopAppBar(
                currentMood = uiState.currentMood,
                activeViewMode = uiState.activeViewMode,
                isVoiceAutoSpeak = uiState.isVoiceAutoSpeak,
                isApiKeyConfigured = uiState.isApiKeyConfigured,
                onViewModeToggled = {
                    viewModel.setViewMode(
                        if (uiState.activeViewMode == ViewMode.STAGE) ViewMode.CHAT else ViewMode.STAGE
                    )
                },
                onVoiceToggle = { viewModel.setVoiceAutoSpeak(!uiState.isVoiceAutoSpeak) },
                onSettingsClick = { showSettingsSheet = true },
                onApiKeyInfoClick = { showApiKeyInfoDialog = true },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background Artwork of Mahi Girl
            Image(
                painter = painterResource(id = R.drawable.img_mahi_art),
                contentDescription = "Mahi AI Girl Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.85f
            )

            when (uiState.activeViewMode) {
                ViewMode.STAGE -> {
                    StageView(
                        uiState = uiState,
                        onMicAction = onMicAction,
                        onAvatarClick = { viewModel.onAvatarPoke() },
                        onPromptSelected = { prompt ->
                            viewModel.sendMessage(prompt, isVoice = false)
                        },
                        onStopSpeaking = { viewModel.stopSpeaking() }
                    )
                }

                ViewMode.CHAT -> {
                    ChatView(
                        uiState = uiState,
                        textInput = textInput,
                        onTextInputChanged = { textInput = it },
                        onSendText = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput, isVoice = false)
                                textInput = ""
                            }
                        },
                        onMicAction = onMicAction,
                        onSpeakMessage = { viewModel.speakText(it) },
                        onPromptSelected = { prompt ->
                            viewModel.sendMessage(prompt, isVoice = false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MahiTopAppBar(
    currentMood: com.example.model.MahiMood,
    activeViewMode: ViewMode,
    isVoiceAutoSpeak: Boolean,
    isApiKeyConfigured: Boolean,
    onViewModeToggled: () -> Unit,
    onVoiceToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onApiKeyInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Brand & Persona Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonRose, ElectricViolet)
                            )
                        )
                ) {
                    Text("M", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Mahi AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(currentMood.glowColor)
                        )
                    }
                    Text(
                        text = "Your Sassy Bestie ✨",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonRose
                    )
                }
            }

            // Quick Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isApiKeyConfigured) {
                    IconButton(
                        onClick = onApiKeyInfoClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("api_key_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "API Key Info",
                            tint = SassyPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Voice sound toggle
                IconButton(
                    onClick = onVoiceToggle,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (isVoiceAutoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Voice",
                        tint = if (isVoiceAutoSpeak) NeonRose else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Switch between Stage and Chat view
                IconButton(
                    onClick = onViewModeToggled,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_view_mode_button")
                ) {
                    Icon(
                        imageVector = if (activeViewMode == ViewMode.STAGE) Icons.Default.ChatBubble else Icons.Default.GraphicEq,
                        contentDescription = "Switch View Mode",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StageView(
    uiState: MahiUiState,
    onMicAction: () -> Unit,
    onAvatarClick: () -> Unit,
    onPromptSelected: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Center Animated Avatar
        MahiAvatarVisualizer(
            mood = uiState.currentMood,
            voiceState = uiState.voiceState,
            rmsLevel = uiState.rmsLevel,
            onAvatarClick = onAvatarClick
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Live Audio Waveform Bars
        AudioWaveformBar(
            voiceState = uiState.voiceState,
            rmsLevel = uiState.rmsLevel,
            primaryColor = uiState.currentMood.glowColor,
            secondaryColor = uiState.currentMood.secondaryColor,
            modifier = Modifier.height(36.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Active Spoken Text / Live Transcription Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            border = BorderStroke(1.dp, uiState.currentMood.glowColor.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("live_caption_card")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                val captionText = when {
                    uiState.voiceState == VoiceState.LISTENING -> {
                        if (uiState.liveTranscription.isNotBlank()) {
                            "You: \"${uiState.liveTranscription}\""
                        } else {
                            "I'm listening, sweetheart... Speak to me!"
                        }
                    }
                    uiState.voiceState == VoiceState.PROCESSING -> {
                        "Thinking of something spicy..."
                    }
                    uiState.activeSpokenMahiText.isNotBlank() -> {
                        uiState.activeSpokenMahiText
                    }
                    uiState.messages.isNotEmpty() -> {
                        val lastMahi = uiState.messages.lastOrNull { it.sender == com.example.model.MessageSender.MAHI }
                        lastMahi?.text ?: "Ready whenever you are, babe~"
                    }
                    else -> "Ready whenever you are, babe~"
                }

                Text(
                    text = captionText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.voiceState == VoiceState.SPEAKING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeonRose.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, NeonRose),
                        modifier = Modifier.clickable { onStopSpeaking() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Interrupt",
                                tint = NeonRose,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Interrupt",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonRose
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Prompts row
        QuickPromptBar(onPromptSelected = onPromptSelected)

        Spacer(modifier = Modifier.height(14.dp))

        // Giant Glowing Neon Mic Button
        val isListening = uiState.voiceState == VoiceState.LISTENING
        val isSpeaking = uiState.voiceState == VoiceState.SPEAKING

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .shadow(
                        elevation = if (isListening) 24.dp else 12.dp,
                        shape = CircleShape,
                        spotColor = if (isListening) NeonRose else ElectricViolet
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (isListening) NeonRose else ElectricViolet,
                                if (isListening) BrightRose else Color(0xFF4A154B)
                            )
                        )
                    )
                    .border(
                        width = if (isListening) 3.dp else 1.5.dp,
                        color = Color.White.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .clickable { onMicAction() }
                    .testTag("stage_mic_fab")
            ) {
                Icon(
                    imageVector = when {
                        isListening -> Icons.Default.Stop
                        isSpeaking -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Voice Control",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    isListening -> "Tap to Send"
                    isSpeaking -> "Mahi is Talking"
                    else -> "Tap to Talk to Mahi"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isListening) NeonRose else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ChatView(
    uiState: MahiUiState,
    textInput: String,
    onTextInputChanged: (String) -> Unit,
    onSendText: () -> Unit,
    onMicAction: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Message Transcript
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list")
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onSpeakClick = onSpeakMessage
                )
            }

            if (uiState.voiceState == VoiceState.PROCESSING) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mahi is typing something witty...",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonRose
                        )
                    }
                }
            }
        }

        // Quick suggestions bar
        QuickPromptBar(onPromptSelected = onPromptSelected)

        // Bottom Input Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Mic Button
                IconButton(
                    onClick = onMicAction,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.voiceState == VoiceState.LISTENING) NeonRose else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("chat_mic_button")
                ) {
                    Icon(
                        imageVector = if (uiState.voiceState == VoiceState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Record Voice",
                        tint = if (uiState.voiceState == VoiceState.LISTENING) Color.White else NeonRose,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChanged,
                    placeholder = {
                        Text(
                            "Message Mahi...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonRose,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                IconButton(
                    onClick = onSendText,
                    enabled = textInput.isNotBlank(),
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (textInput.isNotBlank()) NeonRose else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (textInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
