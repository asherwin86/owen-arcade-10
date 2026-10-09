package com.example.ui.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.ArcadeMusicManager
import com.example.audio.ChiptuneSynthesizer
import com.example.audio.GeneratedMusicTrack
import com.example.audio.LyriaMusicService
import com.example.audio.MusicTrackRegistry
import kotlinx.coroutines.launch

/**
 * Comprehensive AI Music Studio & Jukebox Dialog.
 * Allows generating custom AI background music with Lyria 3 (Clip & Pro) or browsing all 12 game soundtracks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicGenerationDialog(
    isOpen: Boolean,
    musicManager: ArcadeMusicManager,
    initialTargetGameId: String? = null,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedTargetId by remember {
        mutableStateOf(initialTargetGameId ?: MusicTrackRegistry.LOBBY_TRACK_ID)
    }
    var selectedModel by remember { mutableStateOf(LyriaMusicService.MODEL_CLIP) }

    val currentTargetInfo = remember(selectedTargetId) {
        MusicTrackRegistry.getTrackById(selectedTargetId)
    }

    var promptText by remember(selectedTargetId) {
        mutableStateOf(currentTargetInfo.defaultAiPrompt)
    }

    var isGenerating by remember { mutableStateOf(false) }
    var isEnhancing by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var generatedTrack by remember { mutableStateOf<GeneratedMusicTrack?>(null) }

    val previewingFile by musicManager.previewPlayingFile.collectAsState()
    val isAppMuted by musicManager.isMuted.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF8B5CF6), Color(0xFF00FFCC), Color(0xFF1E1B4B))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .testTag("music_generation_dialog"),
            color = Color(0xFF0D0C1D),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ARCADE MUSIC STUDIO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Powered by Google Lyria 3 & Gemini AI",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF00FFCC)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: [ AI Studio (Generate) ] [ Jukebox (12 Soundtracks) ]
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF171530),
                    contentColor = Color(0xFF00FFCC),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF00FFCC)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("AI Studio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("Jukebox (${MusicTrackRegistry.tracks.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB 0: AI GENERATION STUDIO
                if (selectedTabIndex == 0) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Target Selector (Arcade Lobby + 11 Games)
                        item {
                            Text(
                                text = "1. SELECT TARGET AREA / GAME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(MusicTrackRegistry.tracks) { track ->
                                    val isSelected = track.id == selectedTargetId
                                    val isAiActive = musicManager.isGameUsingAiTrack(track.id)

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTargetId = track.id
                                            promptText = track.defaultAiPrompt
                                            generatedTrack = null
                                            generationError = null
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(track.emoji)
                                                Text(track.gameTitle, fontSize = 12.sp)
                                                if (isAiActive) {
                                                    Text("✨", fontSize = 10.sp)
                                                }
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF8B5CF6),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF1E1B4B),
                                            labelColor = Color(0xFFCBD5E1)
                                        )
                                    )
                                }
                            }
                        }

                        // 2. Model Selector (lyria-3-clip-preview vs lyria-3-pro-preview)
                        item {
                            Text(
                                text = "2. CHOOSE LYRIA MODEL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // lyria-3-clip-preview
                                val isClipSelected = selectedModel == LyriaMusicService.MODEL_CLIP
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.5.dp,
                                            if (isClipSelected) Color(0xFF00FFCC) else Color(0xFF334155),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedModel = LyriaMusicService.MODEL_CLIP },
                                    color = if (isClipSelected) Color(0xFF003730) else Color(0xFF1E293B)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "lyria-3-clip-preview",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isClipSelected) Color(0xFF00FFCC) else Color.White
                                            )
                                        }
                                        Text(
                                            text = "Short clip (up to 30s) • Fast looping",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                // lyria-3-pro-preview
                                val isProSelected = selectedModel == LyriaMusicService.MODEL_PRO
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.5.dp,
                                            if (isProSelected) Color(0xFFA855F7) else Color(0xFF334155),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedModel = LyriaMusicService.MODEL_PRO },
                                    color = if (isProSelected) Color(0xFF2E1065) else Color(0xFF1E293B)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "lyria-3-pro-preview",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isProSelected) Color(0xFFA855F7) else Color.White
                                            )
                                        }
                                        Text(
                                            text = "Full-length track • Studio depth",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Prompt Presets for current target
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "3. STYLE PRESETS FOR ${currentTargetInfo.gameTitle.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace
                                )

                                Text(
                                    text = "Tap to load",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(
                                    "Classic 8-Bit" to "Upbeat 8-bit retro arcade chiptune with catchy square wave melodies and punchy drum machine.",
                                    "Cyber Synthwave" to "Fast-paced driving retro 80s synthwave with pulsing bassline, arpeggiated synths, and arcade tempo.",
                                    "Chill Lo-Fi" to "Relaxed lo-fi study beat with warm electric piano chords, soft mellow bass, and vinyl crackle.",
                                    "Heroic Power-Pop" to "High-energy power chiptune rock with soaring anthemic leads and driving four-on-the-floor kick."
                                )

                                presets.forEach { (label, presetPrompt) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E293B))
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .clickable { promptText = presetPrompt }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            color = Color(0xFF00FFCC),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Prompt Input & AI Enhancement Button
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "4. MUSIC PROMPT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace
                                )

                                // Enhance with Gemini button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2E1065))
                                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isEnhancing && !isGenerating) {
                                            isEnhancing = true
                                            coroutineScope.launch {
                                                val result = LyriaMusicService.enhanceMusicPrompt(
                                                    currentTargetInfo.gameTitle,
                                                    promptText
                                                )
                                                result.onSuccess { enhanced ->
                                                    promptText = enhanced
                                                }
                                                isEnhancing = false
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isEnhancing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                color = Color(0xFFA855F7),
                                                strokeWidth = 1.5.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isEnhancing) "Enhancing..." else "AI Enhance",
                                            fontSize = 10.sp,
                                            color = Color(0xFFD8B4FE),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = promptText,
                                onValueChange = { promptText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("music_prompt_input"),
                                minLines = 3,
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FFCC),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedContainerColor = Color(0xFF131126),
                                    unfocusedContainerColor = Color(0xFF131126),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color(0xFFE2E8F0)
                                ),
                                placeholder = {
                                    Text("Describe instruments, tempo, BPM, and mood...", color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                            )
                        }

                        // Generate Button
                        item {
                            Button(
                                onClick = {
                                    isGenerating = true
                                    generationError = null
                                    coroutineScope.launch {
                                        val result = LyriaMusicService.generateTrack(
                                            context = context,
                                            gameId = selectedTargetId,
                                            prompt = promptText,
                                            modelName = selectedModel
                                        )
                                        result.onSuccess { track ->
                                            generatedTrack = track
                                            isGenerating = false
                                        }.onFailure { err ->
                                            generationError = err.message ?: "Music generation failed."
                                            isGenerating = false
                                        }
                                    }
                                },
                                enabled = !isGenerating && promptText.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("generate_music_submit_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF8B5CF6),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isGenerating) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Composing with $selectedModel...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "GENERATE MUSIC WITH LYRIA 3",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Error Display
                        if (generationError != null) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF450A0A),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Generation Notice",
                                            color = Color(0xFFFCA5A5),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = generationError ?: "",
                                            color = Color(0xFFFECACA),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "💡 Tip: Make sure GEMINI_API_KEY is configured in the AI Studio Secrets panel. Meanwhile, high-fidelity built-in chiptune tracks are fully active for all 10 games & arcade lobby!",
                                            color = Color(0xFFFDE047),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Generated Result Card
                        if (generatedTrack != null) {
                            val track = generatedTrack!!
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp)),
                                    color = Color(0xFF003730).copy(alpha = 0.6f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF00FFCC)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = Color(0xFF003730),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "GENERATED AUDIO READY",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF00FFCC),
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = "${track.modelUsed} • For ${currentTargetInfo.gameTitle}",
                                                        color = Color(0xFFCBD5E1),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            // Play/Pause Preview
                                            val isThisPreviewing = previewingFile == track.file.absolutePath
                                            IconButton(
                                                onClick = {
                                                    musicManager.previewAudio(track.file)
                                                },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00FFCC))
                                            ) {
                                                Icon(
                                                    imageVector = if (isThisPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Preview",
                                                    tint = Color(0xFF003730),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Apply as Background Music Button
                                        Button(
                                            onClick = {
                                                musicManager.assignCustomTrack(selectedTargetId, track)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10B981),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "✓ SET AS BGM FOR ${currentTargetInfo.gameTitle.uppercase()}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: JUKEBOX (ALL 12 SOUNDTRACKS OVERVIEW)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "ACTIVE SOUNDTRACKS ACROSS ARCADE & ALL 10 GAMES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF00FFCC),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Each game features a distinct musical theme. Tap audition to listen, or switch between 8-bit and Lyria AI.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        items(MusicTrackRegistry.tracks) { trackInfo ->
                            val isAi = musicManager.isGameUsingAiTrack(trackInfo.id)
                            val activeTitle = musicManager.getTrackTitleForGame(trackInfo.id)

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                                color = Color(0xFF171530)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(trackInfo.emoji, fontSize = 24.sp)
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = trackInfo.gameTitle,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (isAi) Color(0xFF8B5CF6).copy(alpha = 0.35f)
                                                            else Color(0xFF10B981).copy(alpha = 0.3f)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = if (isAi) "LYRIA AI" else "8-BIT",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isAi) Color(0xFFD8B4FE) else Color(0xFF6EE7B7)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "$activeTitle • ${trackInfo.bpm} BPM",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Audition / Test play button
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val file = ChiptuneSynthesizer.getOrCreateTrackFile(context, trackInfo.id)
                                                    musicManager.previewAudio(file)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Audition",
                                                tint = Color(0xFF00FFCC),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Reset to Chiptune if AI active
                                        if (isAi) {
                                            IconButton(
                                                onClick = {
                                                    musicManager.resetToDefaultChiptune(trackInfo.id)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RestartAlt,
                                                    contentDescription = "Reset to Chiptune",
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Switch to AI tab for this track
                                        IconButton(
                                            onClick = {
                                                selectedTargetId = trackInfo.id
                                                promptText = trackInfo.defaultAiPrompt
                                                selectedTabIndex = 0
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Generate for this track",
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(18.dp)
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
