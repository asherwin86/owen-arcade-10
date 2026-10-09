package com.example.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameRegistry
import com.example.ui.components.ArcadeGameHeader
import com.example.ui.components.GameOverDialog
import com.example.ui.components.HapticHelper
import com.example.ui.theme.ArcadeTheme
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val ARENA_WIDTH = 1000f
private const val ARENA_HEIGHT = 1000f
private const val PLAYER_RADIUS = 28f
private const val TAGGER_RADIUS = 26f
private const val LOBBY_RADIUS = 130f

private enum class TagDisplayMode {
    NATIVE_ARENA,
    WEBGL_PLAYER
}

private data class TaggerBot(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var speed: Float = 175f,
    val color: Color = Color(0xFFEF4444)
)

private data class StaminaOrb(
    val x: Float,
    val y: Float,
    val bonusSeconds: Int = 3
)

@Composable
fun OwenTagGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val gameInfo = remember { GameRegistry.getGame("owen_tag") }

    var displayMode by remember { mutableStateOf(TagDisplayMode.NATIVE_ARENA) }

    // Intercept back key if in WebGL mode to return to Native
    BackHandler(enabled = displayMode == TagDisplayMode.WEBGL_PLAYER) {
        displayMode = TagDisplayMode.NATIVE_ARENA
    }

    // Game loop states
    var isRoundActive by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var survivalSeconds by remember { mutableIntStateOf(0) }
    var startTimeMs by remember { mutableLongStateOf(0L) }

    // Player position and movement
    var playerX by remember { mutableFloatStateOf(ARENA_WIDTH / 2f) }
    var playerY by remember { mutableFloatStateOf(ARENA_HEIGHT / 2f) }
    var moveDirX by remember { mutableFloatStateOf(0f) }
    var moveDirY by remember { mutableFloatStateOf(0f) }
    var lookAngleRad by remember { mutableFloatStateOf(0f) }

    // Stamina
    var stamina by remember { mutableFloatStateOf(4f) }
    val maxStamina = 4f
    var isSprintHeld by remember { mutableStateOf(false) }

    // Taggers list
    var taggers by remember {
        mutableStateOf(
            listOf(
                TaggerBot(x = 180f, y = 180f, speed = 160f),
                TaggerBot(x = 820f, y = 820f, speed = 175f)
            )
        )
    }

    // Stamina orbs
    var orbs by remember { mutableStateOf(listOf<StaminaOrb>()) }

    fun startNewRound() {
        playerX = ARENA_WIDTH / 2f
        playerY = ARENA_HEIGHT / 2f
        moveDirX = 0f
        moveDirY = 0f
        lookAngleRad = 0f
        stamina = maxStamina
        isSprintHeld = false
        survivalSeconds = 0
        startTimeMs = System.currentTimeMillis()
        isGameOver = false
        isRoundActive = true

        taggers = listOf(
            TaggerBot(x = 180f, y = 180f, speed = 165f),
            TaggerBot(x = 820f, y = 820f, speed = 175f)
        )

        orbs = listOf(
            StaminaOrb(x = 260f, y = 740f),
            StaminaOrb(x = 740f, y = 260f)
        )
    }

    // 60fps Native Game Loop
    LaunchedEffect(isRoundActive, isGameOver, displayMode) {
        if (!isRoundActive || isGameOver || displayMode != TagDisplayMode.NATIVE_ARENA) return@LaunchedEffect

        var lastFrameTime = System.currentTimeMillis()
        var orbSpawnTimer = 0f

        while (isRoundActive && !isGameOver && displayMode == TagDisplayMode.NATIVE_ARENA) {
            delay(16)
            val now = System.currentTimeMillis()
            val dt = ((now - lastFrameTime) / 1000f).coerceIn(0.001f, 0.05f)
            lastFrameTime = now

            survivalSeconds = ((now - startTimeMs) / 1000L).toInt()

            val isMoving = (moveDirX != 0f || moveDirY != 0f)
            val canSprint = isSprintHeld && stamina > 0.1f && isMoving
            val currentSpeed = if (canSprint) 290f else 180f

            if (canSprint) {
                stamina = (stamina - dt * 1.1f).coerceAtLeast(0f)
            } else {
                stamina = (stamina + dt * 1.0f).coerceAtMost(maxStamina)
            }

            if (isMoving) {
                val len = sqrt(moveDirX * moveDirX + moveDirY * moveDirY)
                val nx = moveDirX / len
                val ny = moveDirY / len
                lookAngleRad = atan2(ny, nx)

                playerX = (playerX + nx * currentSpeed * dt).coerceIn(PLAYER_RADIUS, ARENA_WIDTH - PLAYER_RADIUS)
                playerY = (playerY + ny * currentSpeed * dt).coerceIn(PLAYER_RADIUS, ARENA_HEIGHT - PLAYER_RADIUS)
            }

            // Orb collection
            val collectedOrbs = mutableListOf<StaminaOrb>()
            for (orb in orbs) {
                val dx = playerX - orb.x
                val dy = playerY - orb.y
                if (sqrt(dx * dx + dy * dy) < PLAYER_RADIUS + 18f) {
                    collectedOrbs.add(orb)
                    stamina = (stamina + 1.5f).coerceAtMost(maxStamina)
                    HapticHelper.playScore(context)
                }
            }
            if (collectedOrbs.isNotEmpty()) {
                orbs = orbs.filterNot { collectedOrbs.contains(it) }
            }

            // Orb respawn
            orbSpawnTimer += dt
            if (orbSpawnTimer >= 7f && orbs.size < 3) {
                orbSpawnTimer = 0f
                val newOrb = StaminaOrb(
                    x = Random.nextFloat() * (ARENA_WIDTH - 200f) + 100f,
                    y = Random.nextFloat() * (ARENA_HEIGHT - 200f) + 100f
                )
                orbs = orbs + newOrb
            }

            // Add 3rd tagger after 20s
            if (survivalSeconds >= 20 && taggers.size == 2) {
                taggers = taggers + TaggerBot(x = 180f, y = 820f, speed = 180f)
            }

            // Bot movement and collision
            var playerGotTagged = false
            val updatedTaggers = taggers.map { bot ->
                val toPlayerX = playerX - bot.x
                val toPlayerY = playerY - bot.y
                val dist = sqrt(toPlayerX * toPlayerX + toPlayerY * toPlayerY)

                if (dist <= PLAYER_RADIUS + TAGGER_RADIUS - 6f) {
                    playerGotTagged = true
                }

                if (dist > 1f) {
                    val dirX = toPlayerX / dist
                    val dirY = toPlayerY / dist
                    val botSpeed = (bot.speed + (survivalSeconds * 0.8f)).coerceAtMost(240f)
                    val nextX = (bot.x + dirX * botSpeed * dt).coerceIn(TAGGER_RADIUS, ARENA_WIDTH - TAGGER_RADIUS)
                    val nextY = (bot.y + dirY * botSpeed * dt).coerceIn(TAGGER_RADIUS, ARENA_HEIGHT - TAGGER_RADIUS)
                    bot.copy(x = nextX, y = nextY)
                } else {
                    bot
                }
            }
            taggers = updatedTaggers

            if (playerGotTagged) {
                HapticHelper.playGameOver(context)
                isGameOver = true
                isRoundActive = false
                onRecordScore(survivalSeconds)
                break
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0C20))
            .navigationBarsPadding()
    ) {
        ArcadeGameHeader(
            game = gameInfo,
            score = if (displayMode == TagDisplayMode.NATIVE_ARENA) survivalSeconds else 0,
            highScore = maxOf(survivalSeconds, highScore),
            onBack = onBack,
            onRestart = {
                if (displayMode == TagDisplayMode.NATIVE_ARENA) {
                    startNewRound()
                }
            }
        )

        // Segmented Mode Switcher: Mobile Arena vs Unity WebGL
        TabRow(
            selectedTabIndex = if (displayMode == TagDisplayMode.NATIVE_ARENA) 0 else 1,
            containerColor = colors.surfaceElevated,
            contentColor = colors.neonCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (displayMode == TagDisplayMode.NATIVE_ARENA) 0 else 1]),
                    color = Color(0xFFFF5722),
                    height = 3.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Tab(
                selected = displayMode == TagDisplayMode.NATIVE_ARENA,
                onClick = {
                    HapticHelper.playClick(context)
                    displayMode = TagDisplayMode.NATIVE_ARENA
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (displayMode == TagDisplayMode.NATIVE_ARENA) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MOBILE ARENA",
                            style = textStyles.hudLabel.copy(fontSize = 11.sp),
                            color = if (displayMode == TagDisplayMode.NATIVE_ARENA) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.testTag("tab_native_arena")
            )

            Tab(
                selected = displayMode == TagDisplayMode.WEBGL_PLAYER,
                onClick = {
                    HapticHelper.playClick(context)
                    displayMode = TagDisplayMode.WEBGL_PLAYER
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (displayMode == TagDisplayMode.WEBGL_PLAYER) colors.scoreGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UNITY WEBGL",
                            style = textStyles.hudLabel.copy(fontSize = 11.sp),
                            color = if (displayMode == TagDisplayMode.WEBGL_PLAYER) colors.scoreGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.testTag("tab_unity_webgl")
            )
        }

        // Render Active Mode
        if (displayMode == TagDisplayMode.WEBGL_PLAYER) {
            OwenWebGlView(
                modifier = Modifier.weight(1f),
                onSwitchToNative = { displayMode = TagDisplayMode.NATIVE_ARENA }
            )
        } else {
            // NATIVE ARENA MODE
            // Status bar
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱️ SURVIVED: ", style = textStyles.hudLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${survivalSeconds}s",
                            style = textStyles.hudScore.copy(fontSize = 18.sp),
                            color = colors.neonCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "• BOTS: ${taggers.size}",
                            style = textStyles.hudLabel,
                            color = Color(0xFFEF4444)
                        )
                    }

                    Text(
                        text = "RECORD: ${maxOf(survivalSeconds, highScore)}s",
                        style = textStyles.hudLabel.copy(fontSize = 10.sp),
                        color = colors.scoreGold
                    )
                }
            }

            // Arena Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131028)),
                    elevation = CardDefaults.cardElevation(8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5722).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .pointerInput(isRoundActive) {
                            if (isRoundActive) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        moveDirX = dragAmount.x
                                        moveDirY = dragAmount.y
                                    },
                                    onDragEnd = {
                                        moveDirX = 0f
                                        moveDirY = 0f
                                    }
                                )
                            } else {
                                detectTapGestures {
                                    startNewRound()
                                }
                            }
                        }
                        .testTag("tag_arena_canvas")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val scaleX = w / ARENA_WIDTH
                            val scaleY = h / ARENA_HEIGHT

                            // Grid background
                            val step = 100f * scaleX
                            var gx = 0f
                            while (gx < w) {
                                drawLine(Color(0x12FFFFFF), Offset(gx, 0f), Offset(gx, h), 1f)
                                gx += step
                            }
                            var gy = 0f
                            while (gy < h) {
                                drawLine(Color(0x12FFFFFF), Offset(0f, gy), Offset(w, gy), 1f)
                                gy += step
                            }

                            // Center Lobby Circle
                            val lobbyCenterX = (ARENA_WIDTH / 2f) * scaleX
                            val lobbyCenterY = (ARENA_HEIGHT / 2f) * scaleY
                            val lobbyRadiusPx = LOBBY_RADIUS * scaleX

                            drawCircle(
                                color = Color(0x22FBBF24),
                                radius = lobbyRadiusPx,
                                center = Offset(lobbyCenterX, lobbyCenterY)
                            )
                            drawCircle(
                                color = Color(0x66FBBF24),
                                radius = lobbyRadiusPx,
                                center = Offset(lobbyCenterX, lobbyCenterY),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Stamina Orbs
                            for (orb in orbs) {
                                val ox = orb.x * scaleX
                                val oy = orb.y * scaleY
                                drawCircle(
                                    color = Color(0xFF00F5D4),
                                    radius = 10f * scaleX,
                                    center = Offset(ox, oy)
                                )
                                drawCircle(
                                    color = Color(0x88FFFFFF),
                                    radius = 5f * scaleX,
                                    center = Offset(ox, oy)
                                )
                            }

                            // AI Taggers
                            for (bot in taggers) {
                                val bx = bot.x * scaleX
                                val by = bot.y * scaleY
                                val rPx = TAGGER_RADIUS * scaleX

                                drawCircle(
                                    color = Color(0x33EF4444),
                                    radius = rPx * 1.35f,
                                    center = Offset(bx, by)
                                )
                                drawCircle(
                                    color = Color(0xFFDC2626),
                                    radius = rPx,
                                    center = Offset(bx, by)
                                )
                                drawCircle(
                                    color = Color(0xFFB91C1C),
                                    radius = rPx * 0.7f,
                                    center = Offset(bx, by)
                                )

                                val toPX = (playerX * scaleX) - bx
                                val toPY = (playerY * scaleY) - by
                                val botDist = sqrt(toPX * toPX + toPY * toPY).coerceAtLeast(1f)
                                val lookX = (toPX / botDist) * (rPx * 0.35f)
                                val lookY = (toPY / botDist) * (rPx * 0.35f)

                                drawCircle(
                                    color = Color.White,
                                    radius = rPx * 0.22f,
                                    center = Offset(bx - rPx * 0.3f + lookX, by - rPx * 0.2f + lookY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = rPx * 0.22f,
                                    center = Offset(bx + rPx * 0.3f + lookX, by - rPx * 0.2f + lookY)
                                )
                                drawCircle(
                                    color = Color(0xFF7F1D1D),
                                    radius = rPx * 0.12f,
                                    center = Offset(bx - rPx * 0.3f + lookX * 1.2f, by - rPx * 0.2f + lookY * 1.2f)
                                )
                                drawCircle(
                                    color = Color(0xFF7F1D1D),
                                    radius = rPx * 0.12f,
                                    center = Offset(bx + rPx * 0.3f + lookX * 1.2f, by - rPx * 0.2f + lookY * 1.2f)
                                )
                            }

                            // Player Avatar
                            val px = playerX * scaleX
                            val py = playerY * scaleY
                            val prPx = PLAYER_RADIUS * scaleX

                            if (isSprintHeld && stamina > 0.1f) {
                                drawCircle(
                                    color = Color(0x5500F5D4),
                                    radius = prPx * 1.45f,
                                    center = Offset(px, py)
                                )
                            }

                            drawCircle(
                                color = Color(0xFF00F5D4),
                                radius = prPx,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color(0xFF00BFA5),
                                radius = prPx * 0.8f,
                                center = Offset(px, py)
                            )

                            val eyeDist = prPx * 0.35f
                            val perpAngle = lookAngleRad + Math.PI.toFloat() / 2f
                            val eyeOffX = cos(perpAngle) * eyeDist
                            val eyeOffY = sin(perpAngle) * eyeDist
                            val lookOffX = cos(lookAngleRad) * (prPx * 0.25f)
                            val lookOffY = sin(lookAngleRad) * (prPx * 0.25f)

                            drawCircle(
                                color = Color.White,
                                radius = prPx * 0.24f,
                                center = Offset(px - eyeOffX + lookOffX, py - eyeOffY + lookOffY)
                            )
                            drawCircle(
                                color = Color(0xFF003730),
                                radius = prPx * 0.12f,
                                center = Offset(px - eyeOffX + lookOffX * 1.3f, py - eyeOffY + lookOffY * 1.3f)
                            )

                            drawCircle(
                                color = Color.White,
                                radius = prPx * 0.24f,
                                center = Offset(px + eyeOffX + lookOffX, py + eyeOffY + lookOffY)
                            )
                            drawCircle(
                                color = Color(0xFF003730),
                                radius = prPx * 0.12f,
                                center = Offset(px + eyeOffX + lookOffX * 1.3f, py + eyeOffY + lookOffY * 1.3f)
                            )

                            val mouthCenterX = px + lookOffX * 1.4f
                            val mouthCenterY = py + lookOffY * 1.4f
                            drawCircle(
                                color = Color(0xFF003730),
                                radius = prPx * 0.14f,
                                center = Offset(mouthCenterX, mouthCenterY)
                            )
                        }

                        // Start overlay
                        if (!isRoundActive && !isGameOver) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xBB000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text("🏃", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "OWEN'S TAG ARENA",
                                        style = textStyles.gameHeaderTitle.copy(fontSize = 20.sp),
                                        color = Color(0xFFFF5722)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Survive against relentless AI taggers!\nSwipe or use D-Pad to move. Hold SPRINT to evade!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            HapticHelper.playClick(context)
                                            startNewRound()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF5722),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(48.dp).testTag("start_tag_game_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("START SURVIVAL RUN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Controls & Stamina HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Directional D-Pad
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            moveDirX = 0f
                            moveDirY = -1f
                            HapticHelper.playBump(context)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E1B2E), RoundedCornerShape(10.dp))
                            .testTag("tag_dpad_up")
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
                    }

                    Row {
                        IconButton(
                            onClick = {
                                moveDirX = -1f
                                moveDirY = 0f
                                HapticHelper.playBump(context)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF1E1B2E), RoundedCornerShape(10.dp))
                                .testTag("tag_dpad_left")
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.size(42.dp))
                        IconButton(
                            onClick = {
                                moveDirX = 1f
                                moveDirY = 0f
                                HapticHelper.playBump(context)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF1E1B2E), RoundedCornerShape(10.dp))
                                .testTag("tag_dpad_right")
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White)
                        }
                    }

                    IconButton(
                        onClick = {
                            moveDirX = 0f
                            moveDirY = 1f
                            HapticHelper.playBump(context)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E1B2E), RoundedCornerShape(10.dp))
                            .testTag("tag_dpad_down")
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
                    }
                }

                // Stamina Gauge + Hold-to-Sprint Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("⚡ STAMINA", style = textStyles.hudLabel, color = colors.neonCyan)
                        Text("${(stamina * 25).toInt()}%", style = textStyles.hudLabel, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color(0xFF1E1B2E), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(stamina / maxStamina)
                                .height(10.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(colors.neonCyan, colors.scoreGold)
                                    ),
                                    RoundedCornerShape(5.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isSprintHeld = !isSprintHeld
                            HapticHelper.playClick(context)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSprintHeld) colors.neonCyan else Color(0xFFFF5722),
                            contentColor = if (isSprintHeld) Color(0xFF003730) else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("tag_sprint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Sprint",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSprintHeld) "SPRINT ACTIVE (1.6X)" else "HOLD SPRINT BOOST",
                            fontWeight = FontWeight.ExtraBold,
                            style = textStyles.hudLabel.copy(fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver,
        title = "TAGGED!",
        subtitle = "You survived for $survivalSeconds seconds against the bots!",
        score = survivalSeconds,
        highScore = highScore,
        scoreUnit = "sec",
        onRestart = { startNewRound() },
        onHome = onBack
    )
}
